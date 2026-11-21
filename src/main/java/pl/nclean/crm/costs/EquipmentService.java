package pl.nclean.crm.costs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.fuel.FuelJournalService;
import pl.nclean.crm.shared.BusinessDates;

/** Asset maintenance uses the existing cost book and fuel readings. No new financial ledger. */
@Service
@Transactional(readOnly = true)
public class EquipmentService {
    public static final List<String> KINDS = List.of("EQUIPMENT", "CAR", "GENERATOR");
    private final JdbcTemplate db;
    private final CostService costs;
    private final FuelJournalService fuel;
    private final ObjectMapper json;
    private final EquipmentUsageService usage;

    public EquipmentService(JdbcTemplate db, CostService costs, FuelJournalService fuel, ObjectMapper json,EquipmentUsageService usage) {
        this.usage=usage;
        this.db = db; this.costs = costs; this.fuel = fuel; this.json = json;
    }

    public record AssetCommand(Long id, long version, UUID token, String name, String category,
            String model, String serial, String supplier, String note, String kind, boolean active,
            LocalDate purchasedOn, LocalDate startedOn, LocalDate warrantyUntil,
            LocalDate historyBoundary, boolean historyComplete) {}
    public record CounterCommand(long assetId, UUID token, String name, String unit, String fuelTarget) {}
    public record ReadingCommand(long counterId, long version, UUID token, LocalDate day,
            BigDecimal value, String kind, String note) {}
    public record PlanCommand(long assetId, UUID token, String title, Long counterId, LocalDate dueOn,
            BigDecimal dueValue, Integer intervalDays, BigDecimal intervalValue, int warningDays,
            BigDecimal warningValue) {}
    public record ExecutionCommand(long planId, long version, UUID token, LocalDate day,
            BigDecimal reading, BigDecimal cost, String note) {}
    public record PlanAction(long planId, long version, UUID token, LocalDate until) {}
    public record ArchiveCommand(long assetId, long version, UUID token, boolean active) {}
    public record Counter(long id, long assetId, long version, String name, String unit, String fuelTarget,
            BigDecimal actual, LocalDate actualOn, BigDecimal estimate, LocalDate estimateOn,BigDecimal trackedHours,int trackedJobs) {
        public BigDecimal current() { return estimated() ? estimate : actual; }
        public boolean estimated() { return estimate != null && (actual == null || estimateOn.isAfter(actualOn)); }
    }
    public record Asset(long id, long version, String name, String category, String model, String serial,
            String supplier, String note, String kind, boolean active, LocalDate purchasedOn,
            LocalDate startedOn, LocalDate warrantyUntil, LocalDate historyBoundary, boolean historyComplete,
            BigDecimal purchases, BigDecimal maintenance) {}
    public record Plan(long id, long assetId, long version, String assetName, String title, Long counterId,
            String unit, boolean estimated, LocalDate dueOn, BigDecimal dueValue, Integer intervalDays,
            BigDecimal intervalValue, LocalDate snoozedUntil, boolean active, boolean assetActive,
            String label, String severity, int rank) {}

    private void lock() { db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE", Integer.class); }
    private static LocalDate day(Object value) { return value == null ? null : ((java.sql.Date) value).toLocalDate(); }
    private static long id(Map<String,Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    private static void date(LocalDate day, boolean required) {
        if (day == null) { if (required) throw new IllegalArgumentException("Wybierz datę."); return; }
        BusinessDates.period(day, "day");
    }
    private static void past(LocalDate day) {
        date(day, true);
        if (day.isAfter(BusinessDates.today())) throw new IllegalArgumentException("Nie zapisuj wykonania ani odczytu z przyszłości.");
    }
    private static void text(String value, int max, boolean required) {
        if (value == null || value.length() > max || required && value.isBlank())
            throw new IllegalArgumentException("Uzupełnij opis i sprawdź długość pól.");
    }
    private static void value(BigDecimal value, boolean required) {
        if (value == null) { if (required) throw new IllegalArgumentException("Podaj wartość licznika."); return; }
        if (value.signum() < 0 || value.scale() > 2 || value.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("Podaj nieujemną liczbę do dwóch miejsc po przecinku.");
    }
    private String json(Object command) {
        try { return json.writeValueAsString(command); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Nie udało się odczytać polecenia."); }
    }
    private Long replay(UUID token, String operation, Object command) {
        if(token==null)throw new IllegalArgumentException("Brak identyfikatora formularza. Otwórz go ponownie.");
        var rows = db.queryForList("SELECT result_id,operation,detail=?::jsonb same FROM equipment_change WHERE request_token=?", json(command), token);
        if (rows.isEmpty()) return null;
        if (!operation.equals(rows.get(0).get("operation")) || !Boolean.TRUE.equals(rows.get(0).get("same")))
            throw new IllegalArgumentException("Ten formularz już zapisano z innymi danymi. Otwórz nowy formularz.");
        return id(rows.get(0), "result_id");
    }
    private void audit(UUID token, String operation, long asset, long result, Object command, String actor) {
        db.update("INSERT INTO equipment_change(request_token,operation,asset_id,result_id,detail,actor) VALUES (?,?,?,?,?::jsonb,?)",
                token, operation, asset, result, json(command), actor);
    }
    private static void changed(int count) {
        if (count != 1) throw new IllegalArgumentException("Dane zmieniły się w innym oknie. Zachowano pola; otwórz aktualny formularz.");
    }
    private void available(long assetId) {
        if (!asset(assetId).active()) throw new IllegalArgumentException("Sprzęt jest archiwalny. Przywróć aktywność przed zapisem.");
    }
    public List<Asset> assets(String status, String query) {
        if (!List.of("active", "archived", "all").contains(status) || query.length() > 120)
            throw new IllegalArgumentException("Sprawdź filtr sprzętu.");
        String q = query.strip().toLowerCase(Locale.ROOT);
        return costs.assets().stream().map(this::asset).filter(a -> status.equals("all") || a.active() == status.equals("active"))
                .filter(a -> (a.name()+" "+a.model()+" "+a.category()).toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    public Asset asset(long id) {
        var a = costs.asset(id);
        var totals = db.queryForMap("SELECT COALESCE(SUM(amount) FILTER(WHERE effect='INVESTMENT'),0) purchases,COALESCE(SUM(amount) FILTER(WHERE effect<>'INVESTMENT'),0) maintenance FROM cost_entry WHERE asset_id=? AND NOT cancelled", id);
        a.putAll(totals); return asset(a);
    }
    private Asset asset(Map<String,Object> a) {
        return new Asset(id(a,"id"),id(a,"version"),(String)a.get("name"),(String)a.get("category"),
                (String)a.get("model"),(String)a.get("serial_number"),(String)a.get("supplier"),(String)a.get("note"),
                (String)a.get("kind"),(Boolean)a.get("active"),day(a.get("purchased_on")),day(a.get("started_on")),
                day(a.get("warranty_until")),day(a.get("history_boundary")),(Boolean)a.get("history_complete"),
                (BigDecimal)a.getOrDefault("purchases",a.get("purchase_total")),
                (BigDecimal)a.getOrDefault("maintenance",a.get("maintenance_total")));
    }
    @Transactional public long saveAsset(AssetCommand c, String actor) {
        text(c.name(),160,true); text(c.category(),100,false); text(c.model(),160,false);
        text(c.serial(),160,false); text(c.supplier(),160,false); text(c.note(),2000,false);
        if (!KINDS.contains(c.kind())) throw new IllegalArgumentException("Wybierz rodzaj sprzętu.");
        date(c.purchasedOn(),false); date(c.startedOn(),false); date(c.warrantyUntil(),false); date(c.historyBoundary(),false);
        if (c.purchasedOn()!=null && c.startedOn()!=null && c.startedOn().isBefore(c.purchasedOn()))
            throw new IllegalArgumentException("Początek użytkowania nie może poprzedzać zakupu.");
        lock(); var replay = replay(c.token(),"ASSET",c); if (replay != null) return replay;
        long asset;
        if (c.id() == null) {
            asset = db.queryForObject("INSERT INTO equipment_item(request_token,name,category,model,serial_number,supplier,note,active,warranty_until) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
                    c.token(),c.name().strip(),c.category().strip(),c.model().strip(),c.serial().strip(),c.supplier().strip(),c.note().strip(),c.active(),c.warrantyUntil());
        } else {
            asset = c.id();
            if (!asset(asset).kind().equals(c.kind()) && db.queryForObject("SELECT count(*) FROM equipment_counter WHERE asset_id=? AND fuel_target IS NOT NULL",Integer.class,asset)>0)
                throw new IllegalArgumentException("Rodzaj jest powiązany z licznikiem paliwa i nie może zostać zmieniony.");
            var fields = new HashMap<String,String>();
            fields.put("name",c.name()); fields.put("category",c.category()); fields.put("model",c.model());
            fields.put("serial_number",c.serial()); fields.put("supplier",c.supplier()); fields.put("note",c.note());
            fields.put("warranty_until",Objects.toString(c.warrantyUntil(),"")); if(c.active())fields.put("active","true");
            costs.editAsset(asset,c.version(),fields,actor);
        }
        db.update("UPDATE equipment_item SET kind=?,purchased_on=?,started_on=?,history_boundary=?,history_complete=? WHERE id=?",c.kind(),c.purchasedOn(),c.startedOn(),c.historyBoundary(),c.historyComplete(),asset);
        audit(c.token(),"ASSET",asset,asset,c,actor); return asset;
    }
    @Transactional public long archive(ArchiveCommand c,String actor) {
        lock();var replay=replay(c.token(),"ARCHIVE",c);if(replay!=null)return replay;
        changed(db.update("UPDATE equipment_item SET active=?,version=version+1 WHERE id=? AND version=?",c.active(),c.assetId(),c.version()));
        audit(c.token(),"ARCHIVE",c.assetId(),c.assetId(),c,actor);return c.assetId();
    }
    public List<Counter> counters(long asset,LocalDate at) {
        return db.queryForList("SELECT id FROM equipment_counter WHERE asset_id=? ORDER BY id",Long.class,asset).stream().map(id->counter(id,at)).toList();
    }
    public Counter counter(long id,LocalDate at) {
        var r=db.queryForList("SELECT * FROM equipment_counter WHERE id=?",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznany licznik."));
        var actual=reading(id,"ACTUAL",at);var estimate=reading(id,"ESTIMATE",at);
        if(r.get("fuel_target")!=null) actual=fuel.equipmentMeter((String)r.get("fuel_target"),at);
        var projection="h".equals(r.get("unit"))?usage.estimate(id,at,(BigDecimal)actual.get("value"),day(actual.get("read_on")),(BigDecimal)estimate.get("value"),day(estimate.get("read_on"))):null;
        return new Counter(id,id(r,"asset_id"),id(r,"version"),(String)r.get("name"),(String)r.get("unit"),(String)r.get("fuel_target"),
                (BigDecimal)actual.get("value"),day(actual.get("read_on")),projection==null?(BigDecimal)estimate.get("value"):projection.value(),projection==null?day(estimate.get("read_on")):projection.day(),projection==null?BigDecimal.ZERO:projection.trackedHours(),projection==null?0:projection.jobs());
    }
    private Map<String,Object> reading(long id,String kind,LocalDate at) {
        return db.queryForList("SELECT value,read_on FROM equipment_reading WHERE counter_id=? AND kind=? AND read_on<=? ORDER BY read_on DESC,id DESC LIMIT 1",id,kind,at).stream().findFirst().orElse(Map.of());
    }
    @Transactional public long addCounter(CounterCommand c,String actor) {
        text(c.name(),160,true);
        String target=c.fuelTarget()==null || c.fuelTarget().isBlank()?null:c.fuelTarget();
        if(!List.of("h","km").contains(c.unit()))throw new IllegalArgumentException("Wybierz godziny lub kilometry.");
        lock();var replay=replay(c.token(),"COUNTER",c);if(replay!=null)return replay;available(c.assetId());
        if(target!=null && (!List.of("CAR","GENERATOR").contains(target) || !asset(c.assetId()).kind().equals(target) || !c.unit().equals(target.equals("CAR")?"km":"h")))
            throw new IllegalArgumentException("Licznik paliwa musi odpowiadać rodzajowi zasobu i jednostce.");
        if(target!=null && db.queryForObject("SELECT count(*) FROM equipment_counter WHERE fuel_target=?",Integer.class,target)>0)
            throw new IllegalArgumentException("Ten licznik paliwa jest już przypisany do kartoteki.");
        long id=db.queryForObject("INSERT INTO equipment_counter(asset_id,request_token,name,unit,fuel_target) VALUES (?,?,?,?,?) RETURNING id",Long.class,c.assetId(),c.token(),c.name().strip(),c.unit(),target);
        audit(c.token(),"COUNTER",c.assetId(),id,c,actor);return id;
    }
    @Transactional public long addReading(ReadingCommand c,String actor) {
        past(c.day());value(c.value(),true);text(c.note(),1000,false);
        if(!List.of("ACTUAL","ESTIMATE").contains(c.kind()))throw new IllegalArgumentException("Wybierz odczyt lub szacunek.");
        lock();var replay=replay(c.token(),"READING",c);if(replay!=null)return replay;
        var counter=counter(c.counterId(),BusinessDates.today());available(counter.assetId());
        if(counter.fuelTarget()!=null)throw new IllegalArgumentException("Ten licznik pochodzi z Paliwa. Zapisz odczyt w module paliwa.");
        changed(db.update("UPDATE equipment_counter SET version=version+1 WHERE id=? AND version=?",c.counterId(),c.version()));
        insertReading(c.counterId(),c.token(),c.day(),c.value(),c.kind(),c.note(),actor);
        audit(c.token(),"READING",counter.assetId(),c.counterId(),c,actor);return c.counterId();
    }
    private void insertReading(long counter,UUID token,LocalDate day,BigDecimal value,String kind,String note,String actor) {
        if(kind.equals("ACTUAL")) {
            var before=db.queryForObject("SELECT max(value) FROM equipment_reading WHERE counter_id=? AND kind='ACTUAL' AND read_on<=?",BigDecimal.class,counter,day);
            var after=db.queryForObject("SELECT min(value) FROM equipment_reading WHERE counter_id=? AND kind='ACTUAL' AND read_on>?",BigDecimal.class,counter,day);
            if(before!=null && value.compareTo(before)<0 || after!=null && value.compareTo(after)>0)
                throw new IllegalArgumentException("Odczyt musi mieścić się między wcześniejszymi i późniejszymi odczytami. Szacunki nie są pomiarami.");
        }
        db.update("INSERT INTO equipment_reading(counter_id,request_token,read_on,value,kind,note,actor) VALUES (?,?,?,?,?,?,?)",counter,token,day,value,kind,note.strip(),actor);
    }
    public List<Plan> plans(Long asset,LocalDate at) {
        return db.queryForList("SELECT p.*,a.name asset_name,a.active asset_active FROM equipment_maintenance_plan p JOIN equipment_item a ON a.id=p.asset_id WHERE (?::bigint IS NULL OR p.asset_id=?) ORDER BY p.id",asset,asset)
                .stream().map(r->plan(r,at)).sorted(Comparator.comparingInt(Plan::rank).thenComparingLong(Plan::id)).toList();
    }
    public Plan plan(long id,LocalDate at) {
        return db.queryForList("SELECT p.*,a.name asset_name,a.active asset_active FROM equipment_maintenance_plan p JOIN equipment_item a ON a.id=p.asset_id WHERE p.id=?",id)
                .stream().findFirst().map(r->plan(r,at)).orElseThrow(()->new IllegalArgumentException("Nieznany plan serwisu."));
    }
    private Plan plan(Map<String,Object> r,LocalDate at) {
        Long cid=(Long)r.get("counter_id");var c=cid==null?null:counter(cid,at);
        var due=day(r.get("due_on"));var threshold=(BigDecimal)r.get("due_value");
        var remaining=c==null || c.current()==null || threshold==null?null:threshold.subtract(c.current());
        Long days=due==null?null:ChronoUnit.DAYS.between(at,due);
        String label;String severity="neutral";int rank;
        if(!Boolean.TRUE.equals(r.get("active"))) {label="Plan zakończony";rank=9;}
        else if(days!=null && days<0) {label="Termin przekroczony o "+(-days)+" dni";severity="danger";rank=0;}
        else if(remaining!=null && remaining.signum()<0) {label="Próg przekroczony o "+remaining.abs().stripTrailingZeros().toPlainString()+" "+c.unit();severity="danger";rank=0;}
        else if(days!=null && days==0 || remaining!=null && remaining.signum()==0) {label=days!=null && days==0?"Termin dzisiaj":"Osiągnięty próg";severity="warn";rank=1;}
        else if(remaining!=null && remaining.compareTo((BigDecimal)r.get("warning_value"))<=0) {label="Pozostało "+remaining.stripTrailingZeros().toPlainString()+" "+c.unit();severity="warn";rank=2;}
        else if(days!=null && days<=((Number)r.get("warning_days")).intValue()) {label="Termin za "+days+" dni";severity="warn";rank=2;}
        else if(days==null && remaining==null) {label="Brak danych do oceny";rank=4;}
        else {label=remaining!=null?"Pozostało "+remaining.stripTrailingZeros().toPlainString()+" "+c.unit():"Termin: "+due;rank=3;}
        if(c!=null && c.estimated() && remaining!=null) label+=" · szacunek";
        return new Plan(id(r,"id"),id(r,"asset_id"),id(r,"version"),(String)r.get("asset_name"),(String)r.get("title"),cid,
                c==null?"":c.unit(),c!=null && c.estimated(),due,threshold,(Integer)r.get("interval_days"),(BigDecimal)r.get("interval_value"),
                day(r.get("snoozed_until")),(Boolean)r.get("active"),(Boolean)r.get("asset_active"),label,severity,rank);
    }
    public List<Plan> attention(LocalDate at) {
        return plans(null,at).stream().filter(p->p.active() && p.assetActive() && p.rank()<=2 && (p.snoozedUntil()==null || !p.snoozedUntil().isAfter(at))).toList();
    }
    @Transactional public long addPlan(PlanCommand c,String actor) {
        text(c.title(),160,true);date(c.dueOn(),false);value(c.dueValue(),false);value(c.intervalValue(),false);value(c.warningValue(),true);
        if(c.dueOn()==null && c.dueValue()==null)throw new IllegalArgumentException("Podaj datę lub próg licznika.");
        if(c.dueValue()!=null && c.counterId()==null)throw new IllegalArgumentException("Próg wymaga wybrania licznika.");
        if(c.intervalDays()!=null && (c.intervalDays()<1 || c.intervalDays()>36500 || c.dueOn()==null) || c.warningDays()<0 || c.warningDays()>36500)
            throw new IllegalArgumentException("Sprawdź interwał dni i pierwszy termin.");
        if(c.intervalValue()!=null && (c.intervalValue().signum()==0 || c.dueValue()==null))throw new IllegalArgumentException("Dodatni interwał licznika wymaga pierwszego progu.");
        lock();var replay=replay(c.token(),"PLAN",c);if(replay!=null)return replay;available(c.assetId());
        if(c.counterId()!=null && counter(c.counterId(),BusinessDates.today()).assetId()!=c.assetId())throw new IllegalArgumentException("Licznik należy do innego zasobu.");
        long id=db.queryForObject("INSERT INTO equipment_maintenance_plan(asset_id,counter_id,request_token,title,due_on,due_value,interval_days,interval_value,warning_days,warning_value) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,
                c.assetId(),c.counterId(),c.token(),c.title().strip(),c.dueOn(),c.dueValue(),c.intervalDays(),c.intervalValue(),c.warningDays(),c.warningValue());
        audit(c.token(),"PLAN",c.assetId(),id,c,actor);return id;
    }
    @Transactional public long execute(ExecutionCommand c,String actor) {
        past(c.day());value(c.reading(),false);value(c.cost(),false);text(c.note(),1000,false);
        lock();var replay=replay(c.token(),"EXECUTE",c);if(replay!=null)return replay;
        var plan=plan(c.planId(),BusinessDates.today());available(plan.assetId());
        if(!plan.active() || plan.version()!=c.version())throw new IllegalArgumentException("Plan zmienił się lub wykonanie już zapisano. Sprawdź historię.");
        var previous=db.queryForObject("SELECT max(completed_on) FROM equipment_maintenance_execution WHERE plan_id=?",LocalDate.class,plan.id());
        if(previous!=null && c.day().isBefore(previous))throw new IllegalArgumentException("Wykonanie nie może poprzedzać ostatniego wykonania tego planu.");
        BigDecimal reading=c.reading();
        Counter counter=plan.counterId()==null?null:counter(plan.counterId(),c.day());
        if(counter==null && reading!=null)throw new IllegalArgumentException("Plan nie ma licznika.");
        if(counter!=null && counter.fuelTarget()!=null) {
            if(reading!=null)throw new IllegalArgumentException("Powiązany odczyt zapisz w module paliwa, nie w formularzu serwisu.");
            reading=counter.actual();
        }
        if(plan.intervalValue()!=null && reading==null)throw new IllegalArgumentException("Kolejny próg wymaga rzeczywistego odczytu z dnia wykonania lub wcześniejszego.");
        if(plan.intervalValue()!=null && counter!=null && counter.fuelTarget()!=null && !Objects.equals(counter.actualOn(),c.day()))
            throw new IllegalArgumentException("Dla kolejnego progu zapisz w Paliwie odczyt z dnia wykonania serwisu.");
        LocalDate nextDay=plan.intervalDays()==null?null:c.day().plusDays(plan.intervalDays());date(nextDay,false);
        BigDecimal nextValue=plan.intervalValue()==null?null:reading.add(plan.intervalValue());value(nextValue,false);
        if(counter!=null && counter.fuelTarget()==null && reading!=null) {
            insertReading(counter.id(),c.token(),c.day(),reading,"ACTUAL","Serwis: "+plan.title(),actor);
            db.update("UPDATE equipment_counter SET version=version+1 WHERE id=?",counter.id());
        }
        Long cost=null;
        if(c.cost()!=null) {
            var fields=new HashMap<String,String>();fields.put("token",c.token().toString());fields.put("spent_on",c.day().toString());
            fields.put("category",asset(plan.assetId()).kind().equals("CAR")?"CAR_SERVICE":"EQUIPMENT_SERVICE");
            fields.put("title",plan.title());fields.put("amount",c.cost().toPlainString());fields.put("asset_id",Long.toString(plan.assetId()));fields.put("note",c.note());
            cost=costs.save(fields,actor);
        }
        long execution=db.queryForObject("INSERT INTO equipment_maintenance_execution(plan_id,request_token,completed_on,reading,cost_entry_id,note,actor) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,
                plan.id(),c.token(),c.day(),reading,cost,c.note().strip(),actor);
        boolean recurring=nextDay!=null || nextValue!=null;
        changed(db.update("UPDATE equipment_maintenance_plan SET due_on=?,due_value=?,active=?,snoozed_until=NULL,version=version+1 WHERE id=? AND version=?",
                recurring?nextDay:plan.dueOn(),recurring?nextValue:plan.dueValue(),recurring,plan.id(),c.version()));
        audit(c.token(),"EXECUTE",plan.assetId(),execution,c,actor);return execution;
    }
    @Transactional public long snooze(PlanAction c,String actor) {
        date(c.until(),true);
        if(!c.until().isAfter(BusinessDates.today()))throw new IllegalArgumentException("Odroczenie musi dotyczyć przyszłego dnia.");
        lock();var replay=replay(c.token(),"SNOOZE",c);if(replay!=null)return replay;
        var plan=plan(c.planId(),BusinessDates.today());available(plan.assetId());
        changed(db.update("UPDATE equipment_maintenance_plan SET snoozed_until=?,version=version+1 WHERE id=? AND version=? AND active",c.until(),plan.id(),c.version()));
        audit(c.token(),"SNOOZE",plan.assetId(),plan.id(),c,actor);return plan.id();
    }
    @Transactional public long cancelPlan(PlanAction c,String actor) {
        lock();var replay=replay(c.token(),"CANCEL",c);if(replay!=null)return replay;
        var plan=plan(c.planId(),BusinessDates.today());
        changed(db.update("UPDATE equipment_maintenance_plan SET active=FALSE,snoozed_until=NULL,version=version+1 WHERE id=? AND version=? AND active",plan.id(),c.version()));
        audit(c.token(),"CANCEL",plan.assetId(),plan.id(),c,actor);return plan.id();
    }
    public List<Map<String,Object>> readings(long asset,String filter) {
        var a=asset(asset);validateHistory(filter);
        return db.queryForList("SELECT r.*,c.name,c.unit FROM equipment_reading r JOIN equipment_counter c ON c.id=r.counter_id WHERE c.asset_id=? ORDER BY r.read_on DESC,r.id DESC",asset)
                .stream().filter(r->history(day(r.get("read_on")),a.historyBoundary(),filter)).toList();
    }
    public List<Map<String,Object>> executions(long asset,String filter) {
        var a=asset(asset);validateHistory(filter);
        return db.queryForList("SELECT e.*,p.title,c.amount,c.cancelled FROM equipment_maintenance_execution e JOIN equipment_maintenance_plan p ON p.id=e.plan_id LEFT JOIN cost_entry c ON c.id=e.cost_entry_id WHERE p.asset_id=? ORDER BY e.completed_on DESC,e.id DESC",asset)
                .stream().filter(r->history(day(r.get("completed_on")),a.historyBoundary(),filter)).toList();
    }
    public List<Map<String,Object>> expenses(long asset,String filter) {
        var a=asset(asset);validateHistory(filter);
        return costs.assetHistory(asset).stream().filter(r->history(day(r.get("spent_on")),a.historyBoundary(),filter)).toList();
    }
    private static void validateHistory(String filter) {
        if(!List.of("all","historical","current").contains(filter))throw new IllegalArgumentException("Sprawdź zakres historii.");
    }
    private static boolean history(LocalDate date,LocalDate boundary,String filter) {
        return boundary==null || filter.equals("all") || date.isBefore(boundary)==filter.equals("historical");
    }
}
