package pl.nclean.crm.dashboard;

import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class DashboardService {
    private final JdbcTemplate db;
    public DashboardService(JdbcTemplate db){this.db=db;}
    public static LocalDate today(){return LocalDate.now(ZoneId.of("Europe/Warsaw"));}
    public record Period(LocalDate start,LocalDate end,String range){
        public LocalDate previous(){return switch(range){case "week"->start.minusWeeks(1);case "month"->start.minusMonths(1);default->start.minusDays(1);};}
        public LocalDate next(){return switch(range){case "week"->start.plusWeeks(1);case "month"->start.plusMonths(1);default->start.plusDays(1);};}
    }
    public static Period period(LocalDate date,String range){
        if(date==null||date.getYear()<2000||date.getYear()>2100)throw new IllegalArgumentException("Wybierz datę w latach 2000–2100.");
        return switch(range){case "day"->new Period(date,date,"day");case "week"->new Period(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),"week");case "month"->new Period(date.withDayOfMonth(1),YearMonth.from(date).atEndOfMonth(),"month");default->throw new IllegalArgumentException("Wybierz dzień, tydzień lub miesiąc.");};
    }
    public record Slice(String name,String color,BigDecimal amount,BigDecimal percentage){}
    public record Summary(Period period,String mode,List<Map<String,Object>> orders,int planned,int done,int cancelled,
                          BigDecimal revenue,BigDecimal chemistry,BigDecimal fuel,BigDecimal marketing,BigDecimal fixed,
                          BigDecimal costs,BigDecimal remaining,int missingChemistry,int missingFuel,int missingMonths,List<Slice> slices){
        public boolean complete(){return missingChemistry+missingFuel+missingMonths==0;}
    }
    private static BigDecimal money(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}
    public static BigDecimal allocated(BigDecimal amount,LocalDate day){
        // Cumulative rounding makes every month's daily shares sum to exactly the monthly amount.
        var length=BigDecimal.valueOf(YearMonth.from(day).lengthOfMonth());
        return amount.multiply(BigDecimal.valueOf(day.getDayOfMonth())).divide(length,2,RoundingMode.HALF_UP)
            .subtract(amount.multiply(BigDecimal.valueOf(day.getDayOfMonth()-1)).divide(length,2,RoundingMode.HALF_UP));
    }
    public Summary summary(LocalDate date,String range,String mode){
        var p=period(date,range);if(!Set.of("plan","actual").contains(mode))throw new IllegalArgumentException("Nieznany tryb podsumowania.");
        var orders=db.queryForList("SELECT b.*,COALESCE((SELECT string_agg(i.name, ', ' ORDER BY i.position) FROM booking_item i WHERE i.booking_id=b.id),'') service_label FROM booking b WHERE day BETWEEN ? AND ? ORDER BY day,starts,id",p.start(),p.end());
        int planned=0,done=0,cancelled=0,missingChem=0;BigDecimal revenue=BigDecimal.ZERO,chem=BigDecimal.ZERO;
        var included=new HashSet<Long>();
        for(var order:orders){String status=(String)order.get("status");if(status.equals("PLANNED"))planned++;else if(status.equals("DONE"))done++;else cancelled++;
            if(status.equals("CANCELLED")||(mode.equals("actual")&&!status.equals("DONE")))continue;
            revenue=revenue.add((BigDecimal)order.get("total"));included.add(((Number)order.get("id")).longValue());
        }
        // Aggregate before joining to avoid multiplying booking/item prices by chemical rows.
        var lines=db.queryForList("SELECT i.booking_id,i.forecast_defined,i.chemical_cost,i.actual_defined,COALESCE((SELECT SUM(a.amount) FROM booking_actual_chemical a WHERE a.booking_id=i.booking_id AND a.item_position=i.position),0) actual_cost, EXISTS(SELECT 1 FROM booking_actual_chemical a WHERE a.booking_id=i.booking_id AND a.item_position=i.position AND a.amount IS NULL) unknown_price, EXISTS(SELECT 1 FROM booking_chemical c WHERE c.booking_id=i.booking_id AND c.item_position=i.position AND c.quantity>0 AND NOT EXISTS(SELECT 1 FROM booking_actual_chemical a WHERE a.booking_id=c.booking_id AND a.item_position=c.item_position AND a.chemical_id=c.chemical_id AND a.unit=c.unit)) missing_material FROM booking_item i JOIN booking b ON b.id=i.booking_id WHERE b.day BETWEEN ? AND ?",p.start(),p.end());
        var missingBookings=new HashSet<Long>();
        for(var line:lines){long id=((Number)line.get("booking_id")).longValue();if(!included.contains(id))continue;
            if(mode.equals("plan")){if(!(Boolean)line.get("forecast_defined")||line.get("chemical_cost")==null)missingBookings.add(id);else chem=chem.add((BigDecimal)line.get("chemical_cost"));}
            else {chem=chem.add((BigDecimal)line.get("actual_cost"));if(!(Boolean)line.get("actual_defined")||(Boolean)line.get("unknown_price")||(Boolean)line.get("missing_material"))missingBookings.add(id);}
        }
        missingChem=missingBookings.size();
        BigDecimal fuel=BigDecimal.ZERO,marketing=BigDecimal.ZERO,fixed=BigDecimal.ZERO;int missingFuel=0;
        var fuels=new HashMap<LocalDate,Map<String,Object>>();for(var row:db.queryForList("SELECT * FROM dashboard_fuel WHERE day BETWEEN ? AND ?",p.start(),p.end()))fuels.put(((java.sql.Date)row.get("day")).toLocalDate(),row);
        var months=new HashMap<LocalDate,Boolean>();for(var row:db.queryForList("SELECT * FROM dashboard_month WHERE month BETWEEN ? AND ?",p.start().withDayOfMonth(1),p.end().withDayOfMonth(1)))months.put(((java.sql.Date)row.get("month")).toLocalDate(),(Boolean)row.get("configured"));
        var monthly=db.queryForList("SELECT month,category,SUM(amount) amount FROM dashboard_month_cost WHERE month BETWEEN ? AND ? GROUP BY month,category",p.start().withDayOfMonth(1),p.end().withDayOfMonth(1));
        var missingMonths=new HashSet<YearMonth>();
        for(LocalDate day=p.start();!day.isAfter(p.end());day=day.plusDays(1)){
            var f=fuels.get(day);String kmKey=mode.equals("plan")?"planned_km":"actual_km";
            if(f==null||(mode.equals("plan")||f.get("actual_cost")==null)&&f.get(kmKey)==null)missingFuel++;
            else if(mode.equals("actual")&&f.get("actual_cost")!=null)fuel=fuel.add((BigDecimal)f.get("actual_cost"));
            else fuel=fuel.add(money(((BigDecimal)f.get(kmKey)).multiply((BigDecimal)f.get("consumption")).multiply((BigDecimal)f.get("litre_price")).divide(new BigDecimal("100"))));
            if(!Boolean.TRUE.equals(months.get(day.withDayOfMonth(1))))missingMonths.add(YearMonth.from(day));
            else for(var row:monthly)if(((java.sql.Date)row.get("month")).toLocalDate().equals(day.withDayOfMonth(1))){var share=allocated((BigDecimal)row.get("amount"),day);if(row.get("category").equals("MARKETING"))marketing=marketing.add(share);else fixed=fixed.add(share);}
        }
        chem=money(chem);fuel=money(fuel);marketing=money(marketing);fixed=money(fixed);var total=chem.add(fuel).add(marketing).add(fixed);var slices=new ArrayList<Slice>();
        String[] names={"Chemia","Paliwo","Marketing","Koszty stałe"},colors={"#218DCA","#F1AA42","#9D77CA","#42AC93"};BigDecimal[] amounts={chem,fuel,marketing,fixed};
        for(int i=0;i<4;i++)slices.add(new Slice(names[i],colors[i],amounts[i],total.signum()==0?BigDecimal.ZERO:amounts[i].multiply(new BigDecimal("100")).divide(total,2,RoundingMode.HALF_UP)));
        return new Summary(p,mode,orders,planned,done,cancelled,money(revenue),chem,fuel,marketing,fixed,total,money(revenue.subtract(total)),missingChem,missingFuel,missingMonths.size(),slices);
    }
    public List<Map<String,Object>> monthCosts(YearMonth month){return db.queryForList("SELECT * FROM dashboard_month_cost WHERE month=? ORDER BY id",month.atDay(1));}
    public long monthVersion(YearMonth month){var rows=db.queryForList("SELECT version FROM dashboard_month WHERE month=?",month.atDay(1));return rows.isEmpty()?-1:((Number)rows.get(0).get("version")).longValue();}
    public Map<String,Object> fuel(LocalDate day){var rows=db.queryForList("SELECT * FROM dashboard_fuel WHERE day=?",day);return rows.isEmpty()?Map.of("version",-1):rows.get(0);}
    public Map<String,Object> fuelDefaults(){var rows=db.queryForList("SELECT consumption,litre_price FROM dashboard_fuel ORDER BY day DESC LIMIT 1");return rows.isEmpty()?Map.of("consumption","","litre_price",""):rows.get(0);}
    public record Cost(String name,String category,BigDecimal amount){}
    private static void changed(int count){if(count!=1)throw new IllegalArgumentException("Dane zostały zmienione w innym oknie. Odśwież stronę przed ponownym zapisem.");}
    public static BigDecimal number(String value,int scale,BigDecimal max,boolean nullable){
        if(value==null||value.isBlank()){if(nullable)return null;throw new IllegalArgumentException("Uzupełnij wymagane kwoty i parametry.");}
        try{var n=new BigDecimal(value.replace(',','.')).setScale(scale,RoundingMode.UNNECESSARY);if(n.signum()<0||n.compareTo(max)>0)throw new NumberFormatException();return n;}catch(ArithmeticException|NumberFormatException ex){throw new IllegalArgumentException("Podaj nieujemną liczbę, maksymalnie "+max+", do "+scale+" miejsc po przecinku.");}
    }
    @Transactional public void saveMonth(YearMonth month,long version,List<Cost> costs){
        if(month.getYear()<2000||month.getYear()>2100||month.isBefore(YearMonth.from(today())))throw new IllegalArgumentException("Zamkniętych miesięcy nie można zmieniać. Wybierz bieżący lub przyszły miesiąc.");
        if(costs.size()>50)throw new IllegalArgumentException("Dodaj maksymalnie 50 kosztów.");
        for(var c:costs)if(c.name()==null||c.name().isBlank()||c.name().length()>120||c.category()==null||!Set.of("MARKETING","LEASE","INSURANCE","ACCOUNTING","PHONE","SOFTWARE","OTHER").contains(c.category())||c.amount()==null||c.amount().signum()<0||c.amount().scale()>2||c.amount().compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException("Sprawdź nazwę, kategorię i kwotę kosztu.");
        if(version==-1)changed(db.update("INSERT INTO dashboard_month(month,configured) VALUES (?,TRUE) ON CONFLICT DO NOTHING",month.atDay(1)));
        else changed(db.update("UPDATE dashboard_month SET configured=TRUE,version=version+1 WHERE month=? AND version=?",month.atDay(1),version));
        db.update("DELETE FROM dashboard_month_cost WHERE month=?",month.atDay(1));
        for(var c:costs)db.update("INSERT INTO dashboard_month_cost(month,name,category,amount) VALUES (?,?,?,?)",month.atDay(1),c.name().strip(),c.category(),c.amount());
    }
    @Transactional public void saveFuel(LocalDate day,long version,BigDecimal consumption,BigDecimal price,BigDecimal plan,BigDecimal actual,BigDecimal direct){
        period(day,"day");number(consumption==null?null:consumption.toPlainString(),3,new BigDecimal("99999.999"),false);number(price==null?null:price.toPlainString(),3,new BigDecimal("99999.999"),false);
        for(var km:Arrays.asList(plan,actual))number(km==null?null:km.toPlainString(),3,new BigDecimal("9999999.999"),true);
        number(direct==null?null:direct.toPlainString(),2,new BigDecimal("9999999999.99"),true);
        if(version==-1)changed(db.update("INSERT INTO dashboard_fuel(day,consumption,litre_price,planned_km,actual_km,actual_cost) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",day,consumption,price,plan,actual,direct));
        else changed(db.update("UPDATE dashboard_fuel SET consumption=?,litre_price=?,planned_km=?,actual_km=?,actual_cost=?,version=version+1 WHERE day=? AND version=?",consumption,price,plan,actual,direct,day,version));
    }
}
