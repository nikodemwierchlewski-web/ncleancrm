package pl.nclean.crm.costs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.BusinessDates;
import pl.nclean.crm.shared.MonthlyAllocation;

/** Optional economic report projection. Does not write costs, fuel, bookings or payments. */
@Service
@Transactional(readOnly = true)
public class DepreciationService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final JdbcTemplate db;
    private final EquipmentService equipment;
    private final ObjectMapper json;
    public DepreciationService(JdbcTemplate db, EquipmentService equipment, ObjectMapper json) {
        this.db = db; this.equipment = equipment; this.json = json;
    }
    public record Command(long assetId, long version, UUID token, BigDecimal initialValue,
            BigDecimal residualValue, @DateTimeFormat(pattern="yyyy-MM") YearMonth startMonth,
            int months, boolean enabled) {}
    public record Revision(long id, long assetId, long version, YearMonth effectiveMonth, YearMonth startMonth,
            BigDecimal initialValue, BigDecimal residualValue, int months, boolean enabled, String actor) {}
    public record Charge(long assetId, String name, YearMonth month, BigDecimal amount) {}
    public record Item(long assetId, String name, BigDecimal amount) {}
    public record Report(BigDecimal total, List<Item> items, int unconfigured, Map<LocalDate, BigDecimal> daily) {
        public BigDecimal after(BigDecimal profit) { return profit.subtract(total); }
        public BigDecimal afterOn(LocalDate day, BigDecimal profit) { return profit.subtract(daily.getOrDefault(day, ZERO)); }
    }
    public record Detail(Revision plan, LocalDate asOf, BigDecimal monthly, BigDecimal annual,
            BigDecimal accrued, BigDecimal remaining, String status, List<Revision> history) {}

    private static YearMonth month(Object value) { return YearMonth.from(((java.sql.Date)value).toLocalDate()); }
    private Revision revision(Map<String,Object> row) {
        return new Revision(((Number)row.get("id")).longValue(), ((Number)row.get("asset_id")).longValue(),
                ((Number)row.get("version")).longValue(), month(row.get("effective_month")), month(row.get("start_month")),
                (BigDecimal)row.get("initial_value"), (BigDecimal)row.get("residual_value"),
                ((Number)row.get("months")).intValue(), (Boolean)row.get("enabled"), (String)row.get("actor"));
    }
    public List<Revision> history(long asset) {
        return db.queryForList("SELECT * FROM equipment_depreciation_revision WHERE asset_id=? ORDER BY version DESC",asset).stream().map(this::revision).toList();
    }
    public Revision latest(long asset) { return history(asset).stream().findFirst().orElse(null); }
    private String payload(Command c) {
        try { return json.writeValueAsString(c); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Nie udało się odczytać formularza."); }
    }
    private static void money(BigDecimal value) {
        if(value==null || value.signum()<0 || value.scale()>2 || value.compareTo(new BigDecimal("9999999999.99"))>0)
            throw new IllegalArgumentException("Podaj nieujemną kwotę do dwóch miejsc po przecinku.");
    }
    private static void month(YearMonth value) {
        if(value==null || value.getYear()<2000 || value.getYear()>2100)
            throw new IllegalArgumentException("Wybierz miesiąc w latach 2000–2100.");
    }
    @Transactional public long save(Command c,String actor) {
        money(c.initialValue()); money(c.residualValue()); month(c.startMonth());
        if(c.residualValue().compareTo(c.initialValue())>0 || c.months()<1 || c.months()>1200)
            throw new IllegalArgumentException("Wartość końcowa nie może przewyższać początkowej; okres od 1 do 1200 miesięcy.");
        month(c.startMonth().plusMonths(c.months()-1));
        if(c.token()==null)throw new IllegalArgumentException("Otwórz nowy formularz amortyzacji.");
        db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        var replay=db.queryForList("SELECT id,payload=?::jsonb same FROM equipment_depreciation_revision WHERE request_token=?",payload(c),c.token());
        if(!replay.isEmpty()) {
            if(!Boolean.TRUE.equals(replay.get(0).get("same")))throw new IllegalArgumentException("Ten formularz został już zapisany z innymi danymi.");
            return ((Number)replay.get(0).get("id")).longValue();
        }
        equipment.asset(c.assetId());
        var old=latest(c.assetId());
        if(c.version()!=(old==null?-1:old.version()))throw new IllegalArgumentException("Plan zmienił się w innym oknie. Zachowano pola — otwórz aktualny formularz.");
        if(old!=null && !old.startMonth().equals(c.startMonth()))throw new IllegalArgumentException("Pierwszy miesiąc zapisanego planu pozostaje niezmienny. Zmień wartości lub okres od bieżącego miesiąca.");
        var current=YearMonth.from(BusinessDates.today());
        var effective=old==null?c.startMonth():current.isAfter(c.startMonth())?current:c.startMonth();
        var frozen=total(charges(c.assetId(),LocalDate.of(2000,1,1),effective.atDay(1).minusDays(1)),LocalDate.of(2000,1,1),effective.atDay(1).minusDays(1));
        var remaining=c.initialValue().subtract(c.residualValue()).subtract(frozen);
        if(remaining.signum()<0)throw new IllegalArgumentException("Wartość początkowa minus końcowa nie może być niższa niż amortyzacja zachowana za wcześniejsze miesiące: "+frozen+" zł.");
        int monthsLeft=(int)ChronoUnit.MONTHS.between(effective,c.startMonth().plusMonths(c.months()));
        if(c.enabled() && remaining.signum()>0 && monthsLeft<1)throw new IllegalArgumentException("Wybrany okres już upłynął. Wydłuż okres albo wyłącz dalszą amortyzację.");
        long id=db.queryForObject("INSERT INTO equipment_depreciation_revision(asset_id,version,request_token,payload,effective_month,start_month,initial_value,residual_value,months,enabled,actor) VALUES (?,?,?,?::jsonb,?,?,?,?,?,?,?) RETURNING id",Long.class,
                c.assetId(),old==null?0:old.version()+1,c.token(),payload(c),effective.atDay(1),c.startMonth().atDay(1),c.initialValue(),c.residualValue(),c.months(),c.enabled(),actor);
        if(c.enabled() && monthsLeft>0) {
            var schedule=EconomicDepreciation.schedule(remaining,monthsLeft);
            for(int i=0;i<schedule.size();i++)db.update("INSERT INTO equipment_depreciation_charge(revision_id,month,amount) VALUES (?,?,?)",id,effective.plusMonths(i).atDay(1),schedule.get(i));
        }
        return id;
    }
    /** Latest effective revision wins, including disabled revisions without scheduled rows. */
    private List<Charge> charges(Long asset,LocalDate from,LocalDate to) {
        if(to.isBefore(from))return List.of();
        return db.queryForList("""
            SELECT r.asset_id,a.name,c.month,c.amount FROM equipment_depreciation_charge c
            JOIN equipment_depreciation_revision r ON r.id=c.revision_id
            JOIN equipment_item a ON a.id=r.asset_id
            WHERE (?::bigint IS NULL OR r.asset_id=?) AND c.month BETWEEN ? AND ?
            AND NOT EXISTS (SELECT 1 FROM equipment_depreciation_revision newer
                WHERE newer.asset_id=r.asset_id AND newer.version>r.version AND newer.effective_month<=c.month)
            ORDER BY r.asset_id,c.month
            """,asset,asset,from.withDayOfMonth(1),to.withDayOfMonth(1)).stream().map(r->new Charge(
                    ((Number)r.get("asset_id")).longValue(),(String)r.get("name"),month(r.get("month")),(BigDecimal)r.get("amount"))).toList();
    }
    private static BigDecimal within(Charge charge,LocalDate from,LocalDate to) {
        var first=from.isAfter(charge.month().atDay(1))?from:charge.month().atDay(1);
        var last=to.isBefore(charge.month().atEndOfMonth())?to:charge.month().atEndOfMonth();
        var result=ZERO;
        for(var day=first;!day.isAfter(last);day=day.plusDays(1))result=result.add(MonthlyAllocation.forDay(charge.amount(),day));
        return result;
    }
    private static BigDecimal total(List<Charge> charges,LocalDate from,LocalDate to) {
        return charges.stream().map(c->within(c,from,to)).reduce(ZERO,BigDecimal::add);
    }
    public Report report(LocalDate from,LocalDate to) {
        if(from==null || to==null || from.isBefore(BusinessDates.period(LocalDate.of(2000,1,1),"week").start()) || to.isAfter(BusinessDates.period(LocalDate.of(2100,12,31),"week").end()) || to.isBefore(from) || ChronoUnit.DAYS.between(from,to)>365)throw new IllegalArgumentException("Zakres amortyzacji musi obejmować od 1 do 366 dni.");
        var charges=charges(null,from,to);
        var totals=new LinkedHashMap<Long,BigDecimal>();var names=new HashMap<Long,String>();
        var daily=new LinkedHashMap<LocalDate,BigDecimal>();
        for(var c:charges) {
            totals.merge(c.assetId(),within(c,from,to),BigDecimal::add);names.put(c.assetId(),c.name());
            var first=from.isAfter(c.month().atDay(1))?from:c.month().atDay(1);
            var last=to.isBefore(c.month().atEndOfMonth())?to:c.month().atEndOfMonth();
            for(var d=first;!d.isAfter(last);d=d.plusDays(1))daily.merge(d,MonthlyAllocation.forDay(c.amount(),d),BigDecimal::add);
        }
        var items=totals.entrySet().stream().map(e->new Item(e.getKey(),names.get(e.getKey()),e.getValue())).toList();
        int missing=db.queryForObject("SELECT count(*) FROM equipment_item a WHERE a.active AND (a.purchased_on IS NULL OR a.purchased_on<=?) AND NOT EXISTS (SELECT 1 FROM equipment_depreciation_revision r WHERE r.asset_id=a.id)",Integer.class,to);
        return new Report(totals.values().stream().reduce(ZERO,BigDecimal::add),items,missing,Collections.unmodifiableMap(daily));
    }
    public Detail detail(long asset,LocalDate asOf) {
        equipment.asset(asset);BusinessDates.period(asOf,"day");
        var history=history(asset);var plan=history.stream().findFirst().orElse(null);
        if(plan==null)return new Detail(null,asOf,null,null,null,null,"Brak planu",history);
        var m=YearMonth.from(asOf);
        var monthly=total(charges(asset,m.atDay(1),m.atEndOfMonth()),m.atDay(1),m.atEndOfMonth());
        var start=LocalDate.of(asOf.getYear(),1,1);var end=LocalDate.of(asOf.getYear(),12,31);
        var annual=total(charges(asset,start,end),start,end);
        var accrued=total(charges(asset,LocalDate.of(2000,1,1),asOf),LocalDate.of(2000,1,1),asOf);
        var status=!plan.enabled()?"Wyłączona od "+plan.effectiveMonth():m.isBefore(plan.startMonth())?"Jeszcze nierozpoczęta":m.isAfter(plan.startMonth().plusMonths(plan.months()-1))?"Okres zakończony":"Liniowa";
        return new Detail(plan,asOf,monthly,annual,accrued,plan.initialValue().subtract(accrued).max(plan.residualValue()),status,history);
    }
}
