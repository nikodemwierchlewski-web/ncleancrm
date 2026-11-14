package pl.nclean.crm.fuel;

import pl.nclean.crm.shared.DecimalInput;
import pl.nclean.crm.shared.BusinessDates;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class FuelJournalService {
    private final JdbcTemplate db;
    public FuelJournalService(JdbcTemplate db){this.db=db;}
    public static BigDecimal decimal(String value,int scale,String max,boolean optional){return DecimalInput.parse(value,scale,new BigDecimal(max),optional);}
    public static BigDecimal rounded(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal n(Map<String,Object> row,String key){return (BigDecimal)row.get(key);}
    private static LocalDate day(Map<String,Object> row){return ((java.sql.Date)row.get("day")).toLocalDate();}
    private void lock(){db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);}
    private static void changed(int rows){if(rows!=1)throw new IllegalArgumentException("Dane zmieniły się w innym oknie. Zachowano formularz; odśwież go przed kolejnym zapisem.");}
    private static void fuel(String type){if(type==null||!Set.of("PETROL","DIESEL","LPG").contains(type))throw new IllegalArgumentException("Wybierz rodzaj paliwa.");}
    public List<Map<String,Object>> parameters(){return db.queryForList("SELECT * FROM fuel_parameters ORDER BY effective_day DESC,id DESC");}
    public Map<String,Object> parameters(LocalDate date){return db.queryForList("SELECT * FROM fuel_parameters WHERE effective_day<=? ORDER BY effective_day DESC,id DESC LIMIT 1",date).stream().findFirst().orElse(Map.of());}
    @Transactional public void saveParameters(UUID token,LocalDate date,String carRate,String generatorRate,String carFuel,String generatorFuel,String carPrice,String generatorPrice){
        BusinessDates.period(date,"day");if(date.isBefore(BusinessDates.today()))throw new IllegalArgumentException("Nowe parametry mogą obowiązywać od dziś lub w przyszłości. Historia pozostaje zachowana.");
        fuel(carFuel);fuel(generatorFuel);var cr=decimal(carRate,3,"99999.999",false);var gr=decimal(generatorRate,3,"99999.999",false);var cp=decimal(carPrice,3,"99999.999",false);var gp=decimal(generatorPrice,3,"99999.999",false);
        lock();db.update("INSERT INTO fuel_parameters(request_token,effective_day,car_rate,generator_rate,car_fuel,generator_fuel,car_price,generator_price) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(request_token) DO NOTHING",token,date,cr,gr,carFuel,generatorFuel,cp,gp);
    }
    public Map<String,Object> legacyFuel(LocalDate date){return db.queryForList("SELECT * FROM dashboard_fuel WHERE day=?",date).stream().findFirst().orElse(Map.of());}
    public Map<String,Object> daily(LocalDate date){return db.queryForList("SELECT * FROM fuel_day WHERE day=?",date).stream().findFirst().orElse(Map.of("version",-1));}
    public Map<String,Object> previous(LocalDate date){return db.queryForList("SELECT * FROM fuel_day WHERE day<? ORDER BY day DESC LIMIT 1",date).stream().findFirst().orElse(Map.of());}
    public Map<String,Object> next(LocalDate date){return db.queryForList("SELECT * FROM fuel_day WHERE day>? ORDER BY day LIMIT 1",date).stream().findFirst().orElse(Map.of());}
    public List<Map<String,Object>> history(LocalDate before){return db.queryForList("SELECT * FROM fuel_day WHERE day<=? ORDER BY day DESC LIMIT 31",before);}
    public List<Map<String,Object>> audit(LocalDate date){return db.queryForList("SELECT created_at,actor,before_data->>'odometer' old_odometer,after_data->>'odometer' odometer,before_data->>'generator_minutes' old_minutes,after_data->>'generator_minutes' minutes FROM fuel_audit WHERE kind='DAY' AND record_key=? ORDER BY id DESC LIMIT 20",date.toString());}
    @Transactional public void saveDay(LocalDate date,long version,String odometer,int hours,int minutes,boolean confirmed,String actor){
        BusinessDates.period(date,"day");if(date.isAfter(BusinessDates.today()))throw new IllegalArgumentException("Rzeczywisty odczyt zapisz najwcześniej w danym dniu.");
        var km=decimal(odometer,1,"999999999.9",false);if(hours<0||hours>24||minutes<0||minutes>59||hours*60+minutes>1440)throw new IllegalArgumentException("Podaj czas od 0 do 24 godzin; minuty od 0 do 59.");
        lock();var old=daily(date);if(((Number)old.get("version")).longValue()!=version)throw new IllegalArgumentException("Wpis zmienił się w innym oknie. Odśwież formularz.");
        var prev=previous(date);var following=next(date);
        if(!prev.isEmpty()&&km.compareTo(n(prev,"odometer"))<0||!following.isEmpty()&&km.compareTo(n(following,"odometer"))>0)throw new IllegalArgumentException("Licznik musi mieścić się pomiędzy poprzednim i następnym odczytem.");
        var purchaseBefore=db.queryForObject("SELECT max(odometer) FROM fuel_purchase WHERE NOT cancelled AND target='CAR' AND purchased_at<?",BigDecimal.class,date.plusDays(1).atStartOfDay());
        var purchaseAfter=db.queryForObject("SELECT min(odometer) FROM fuel_purchase WHERE NOT cancelled AND target='CAR' AND purchased_at>=?",BigDecimal.class,date.plusDays(1).atStartOfDay());
        if(purchaseBefore!=null&&km.compareTo(purchaseBefore)<0||purchaseAfter!=null&&km.compareTo(purchaseAfter)>0)throw new IllegalArgumentException("Odczyt końca dnia jest sprzeczny z licznikiem zapisanym przy tankowaniu.");
        boolean unusual=hours*60+minutes>960;
        if(!prev.isEmpty()){long days=java.time.temporal.ChronoUnit.DAYS.between(day(prev),date);unusual|=km.subtract(n(prev,"odometer")).compareTo(BigDecimal.valueOf(days*1000))>0;}
        if(!following.isEmpty()){long days=java.time.temporal.ChronoUnit.DAYS.between(date,day(following));unusual|=n(following,"odometer").subtract(km).compareTo(BigDecimal.valueOf(days*1000))>0;}
        boolean correction=version>=0&&(km.compareTo(n(old,"odometer"))!=0||hours*60+minutes!=((Number)old.get("generator_minutes")).intValue());
        if((unusual||correction)&&!confirmed)throw new IllegalArgumentException("Potwierdź korektę lub nietypowy wpis. Zmiana licznika przeliczy odcinek od poprzedniego i do następnego odczytu; luki pozostaną bez podziału na dni.");
        var settings=version>=0?old:parameters(date);if(settings.isEmpty())throw new IllegalArgumentException("Najpierw zapisz parametry spalania i ceny paliwa obowiązujące w tym dniu.");
        if(version<0)db.update("INSERT INTO fuel_day(day,odometer,generator_minutes,parameters_id,car_rate,generator_rate,car_price,generator_price,car_fuel,generator_fuel) VALUES (?,?,?,?,?,?,?,?,?,?)",date,km,hours*60+minutes,settings.get("id"),settings.get("car_rate"),settings.get("generator_rate"),settings.get("car_price"),settings.get("generator_price"),settings.get("car_fuel"),settings.get("generator_fuel"));
        else changed(db.update("UPDATE fuel_day SET odometer=?,generator_minutes=?,version=version+1 WHERE day=? AND version=?",km,hours*60+minutes,date,version));
        db.update("INSERT INTO fuel_audit(kind,record_key,before_data,after_data,actor) SELECT 'DAY',?,?::jsonb,to_jsonb(f),? FROM fuel_day f WHERE day=?",date.toString(),version<0?null:jsonDayBefore(old),actor,date);
    }
    private String jsonDayBefore(Map<String,Object> old){
        // JdbcTemplate's JSON serialization stays in PostgreSQL; only known scalar columns are reconstructed.
        return db.queryForObject("SELECT jsonb_build_object('day',?::text,'version',?::bigint,'odometer',?::numeric,'generator_minutes',?::integer,'parameters_id',?::bigint)::text",String.class,old.get("day").toString(),old.get("version"),old.get("odometer"),old.get("generator_minutes"),old.get("parameters_id"));
    }
    public record DayUsage(LocalDate date,BigDecimal kilometres,BigDecimal carLitres,BigDecimal carCost,BigDecimal generatorLitres,BigDecimal generatorCost,boolean carKnown,boolean generatorKnown,String note){}
    public Map<LocalDate,DayUsage> usage(LocalDate start,LocalDate end){
        var result=new LinkedHashMap<LocalDate,DayUsage>();
        var rows=db.queryForList("SELECT * FROM fuel_day WHERE day BETWEEN ? AND ? OR day=(SELECT max(day) FROM fuel_day WHERE day<?) ORDER BY day",start,end,start);
        Map<String,Object> prev=null;
        for(var row:rows){var date=day(row);if(date.isBefore(start)){prev=row;continue;}
            var gen=n(row,"generator_rate").multiply(BigDecimal.valueOf(((Number)row.get("generator_minutes")).longValue())).divide(new BigDecimal("60"),12,RoundingMode.HALF_UP);
            var km=prev==null?null:n(row,"odometer").subtract(n(prev,"odometer"));boolean adjacent=prev!=null&&day(prev).plusDays(1).equals(date);
            String note=prev==null?"Pierwszy odczyt — punkt początkowy, bez dystansu.":adjacent?"Dystans od poprzedniego dnia: "+km.toPlainString()+" km.":"Od "+day(prev)+" do "+date+": "+km.toPlainString()+" km. Brak podziału na dni; koszt samochodu niepełny.";
            var litres=adjacent?km.multiply(n(row,"car_rate")).divide(new BigDecimal("100")):null;
            result.put(date,new DayUsage(date,km,litres,litres==null?null:rounded(litres.multiply(n(row,"car_price"))),gen,rounded(gen.multiply(n(row,"generator_price"))),adjacent,true,note));prev=row;
        }
        return result;
    }
    public record Totals(BigDecimal kilometres,int generatorMinutes,BigDecimal carLitres,BigDecimal generatorLitres,BigDecimal carCost,BigDecimal generatorCost,BigDecimal purchases,int missingDays,int gapDays){}
    public Totals totals(LocalDate start,LocalDate end){var usages=usage(start,end);BigDecimal kilometres=BigDecimal.ZERO;int generatorMinutes=0;BigDecimal cl=BigDecimal.ZERO,gl=BigDecimal.ZERO,cc=BigDecimal.ZERO,gc=BigDecimal.ZERO;int missing=0,gaps=0;
        for(var date=start;!date.isAfter(end);date=date.plusDays(1)){var u=usages.get(date);if(u==null){missing++;continue;}gl=gl.add(u.generatorLitres());gc=gc.add(u.generatorCost());if(u.carKnown()){cl=cl.add(u.carLitres());cc=cc.add(u.carCost());}else gaps++;}
        generatorMinutes=db.queryForObject("SELECT COALESCE(SUM(generator_minutes),0) FROM fuel_day WHERE day BETWEEN ? AND ?",Integer.class,start,end);
        for(var u:usages.values())if(u.carKnown())kilometres=kilometres.add(u.kilometres());
        var spend=db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM fuel_purchase WHERE NOT cancelled AND purchased_at>=? AND purchased_at<?",BigDecimal.class,start.atStartOfDay(),end.plusDays(1).atStartOfDay());
        return new Totals(kilometres,generatorMinutes,cl.setScale(3,RoundingMode.HALF_UP),gl.setScale(3,RoundingMode.HALF_UP),rounded(cc),rounded(gc),rounded(spend),missing,gaps);
    }
    public List<Map<String,Object>> purchases(LocalDate start,LocalDate end){return db.queryForList("SELECT * FROM fuel_purchase WHERE purchased_at>=? AND purchased_at<? ORDER BY purchased_at DESC,id DESC",start.atStartOfDay(),end.plusDays(1).atStartOfDay());}
    public Map<String,Object> purchase(long id){return db.queryForList("SELECT * FROM fuel_purchase WHERE id=?",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznane tankowanie."));}
    /** Convert JDBC values at the read-model boundary, before they reach the HTTP adapter. */
    public Map<String,Object> purchaseForEdit(long id) {
        var result = new LinkedHashMap<String,Object>(purchase(id));
        if (result.get("purchased_at") instanceof java.sql.Timestamp timestamp) {
            result.put("purchased_at", timestamp.toLocalDateTime());
        }
        return result;
    }
    public record Purchase(Long id,long version,UUID token,LocalDateTime time,String target,String fuel,String amount,String litres,String price,boolean full,String odometer,boolean complete,String note,UUID receipt){}
    @Transactional public long savePurchase(Purchase p,String actor){
        if(p.time()==null||p.time().isAfter(LocalDateTime.now(ZoneId.of("Europe/Warsaw")).plusMinutes(5)))throw new IllegalArgumentException("Podaj datę i godzinę zakupu, bez przyszłych tankowań.");BusinessDates.period(p.time().toLocalDate(),"day");fuel(p.fuel());
        if(p.target()==null||!Set.of("CAR","GENERATOR","SHARED").contains(p.target()))throw new IllegalArgumentException("Wybierz przeznaczenie paliwa.");
        var amount=decimal(p.amount(),2,"9999999999.99",false);var litres=decimal(p.litres(),3,"9999999.999",true);var price=decimal(p.price(),3,"9999999.999",true);var odo=decimal(p.odometer(),1,"999999999.9",true);
        if(litres!=null&&litres.signum()==0||price!=null&&price.signum()==0)throw new IllegalArgumentException("Litry i cena litra muszą być dodatnie albo pozostawione puste.");
        if(litres!=null){var calculated=amount.divide(litres,3,RoundingMode.HALF_UP);if(price!=null&&price.subtract(calculated).abs().compareTo(new BigDecimal("0.02"))>0)throw new IllegalArgumentException("Kwota, litry i cena litra nie są zgodne. Popraw dane lub zostaw cenę pustą.");if(calculated.compareTo(new BigDecimal("9999999.999"))>0)throw new IllegalArgumentException("Wyliczona cena litra przekracza limit. Sprawdź kwotę i liczbę litrów.");price=calculated.signum()>0?calculated:null;}
        if(p.full()&&(!"CAR".equals(p.target())||odo==null))throw new IllegalArgumentException("Tankowanie do pełna wymaga samochodu i odczytu licznika.");
        if(p.note()==null||p.note().length()>1000)throw new IllegalArgumentException("Notatka może mieć do 1000 znaków.");
        lock();if(p.id()==null){var replay=db.queryForList("SELECT id FROM fuel_purchase WHERE request_token=?",p.token());if(!replay.isEmpty())return ((Number)replay.get(0).get("id")).longValue();}
        var old=p.id()==null?null:purchase(p.id());if(old!=null&&(Boolean)old.get("cancelled"))throw new IllegalArgumentException("Tankowanie jest anulowane i pozostaje w historii.");if(old!=null&&((Number)old.get("version")).longValue()!=p.version())throw new IllegalArgumentException("Tankowanie zmieniło się w innym oknie. Odśwież formularz.");
        if(p.receipt()!=null&&db.queryForObject("SELECT count(*) FROM fuel_receipt WHERE id=?",Integer.class,p.receipt())!=1)throw new IllegalArgumentException("Zdjęcie wygasło lub nie istnieje. Prześlij je ponownie.");
        if(p.receipt()!=null&&db.queryForObject("SELECT (SELECT count(*) FROM fuel_purchase WHERE receipt_id=? AND id<>?)+(SELECT count(*) FROM cost_entry WHERE receipt_id=?)",Integer.class,p.receipt(),p.id()==null?-1:p.id(),p.receipt())>0)throw new IllegalArgumentException("Zdjęcie jest już przypisane do innego rachunku.");
        if("CAR".equals(p.target())&&odo!=null){
            var prior=previous(p.time().toLocalDate());
            var later=db.queryForList("SELECT odometer FROM fuel_day WHERE day>=? ORDER BY day LIMIT 1",p.time().toLocalDate());
            if(!prior.isEmpty()&&odo.compareTo(n(prior,"odometer"))<0||!later.isEmpty()&&odo.compareTo(n(later.get(0),"odometer"))>0)throw new IllegalArgumentException("Licznik tankowania jest sprzeczny z odczytami końca dnia.");
            var before=db.queryForList("SELECT odometer FROM fuel_purchase WHERE NOT cancelled AND target='CAR' AND odometer IS NOT NULL AND id<>? AND (purchased_at<? OR (purchased_at=? AND id<?)) ORDER BY purchased_at DESC,id DESC LIMIT 1",p.id()==null?-1:p.id(),p.time(),p.time(),p.id()==null?Long.MAX_VALUE:p.id());
            var after=db.queryForList("SELECT odometer FROM fuel_purchase WHERE NOT cancelled AND target='CAR' AND odometer IS NOT NULL AND id<>? AND (purchased_at>? OR (purchased_at=? AND id>?)) ORDER BY purchased_at,id LIMIT 1",p.id()==null?-1:p.id(),p.time(),p.time(),p.id()==null?Long.MAX_VALUE:p.id());
            if(!before.isEmpty()&&odo.compareTo(n(before.get(0),"odometer"))<0||!after.isEmpty()&&odo.compareTo(n(after.get(0),"odometer"))>0)throw new IllegalArgumentException("Licznik tankowania jest sprzeczny z sąsiednimi tankowaniami.");
        }
        String beforeJson=old==null?null:db.queryForObject("SELECT to_jsonb(p)::text FROM fuel_purchase p WHERE id=?",String.class,p.id());long id;
        if(old==null)id=db.queryForObject("INSERT INTO fuel_purchase(request_token,purchased_at,target,fuel_type,amount,litres,litre_price,full_tank,odometer,records_complete,note,receipt_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,p.token(),p.time(),p.target(),p.fuel(),amount,litres,price,p.full(),odo,p.complete(),p.note().strip(),p.receipt());
        else{changed(db.update("UPDATE fuel_purchase SET purchased_at=?,target=?,fuel_type=?,amount=?,litres=?,litre_price=?,full_tank=?,odometer=?,records_complete=?,note=?,receipt_id=?,version=version+1 WHERE id=? AND version=?",p.time(),p.target(),p.fuel(),amount,litres,price,p.full(),odo,p.complete(),p.note().strip(),p.receipt(),p.id(),p.version()));id=p.id();}
        db.update("INSERT INTO fuel_audit(kind,record_key,before_data,after_data,actor) SELECT 'PURCHASE',?,?::jsonb,to_jsonb(p),? FROM fuel_purchase p WHERE id=?",Long.toString(id),beforeJson,actor,id);
        return id;
    }
    @Transactional public void cancelPurchase(long id,long version,String reason,String actor){
        reason=reason==null?"":reason.strip();if(reason.isBlank()||reason.length()>300)throw new IllegalArgumentException("Podaj powód anulowania do 300 znaków.");lock();var old=purchase(id);if(((Number)old.get("version")).longValue()!=version||(Boolean)old.get("cancelled"))throw new IllegalArgumentException("Tankowanie zmieniło się lub jest już anulowane.");String before=db.queryForObject("SELECT to_jsonb(p)::text FROM fuel_purchase p WHERE id=?",String.class,id);changed(db.update("UPDATE fuel_purchase SET cancelled=TRUE,cancel_reason=?,version=version+1 WHERE id=? AND version=?",reason,id,version));db.update("INSERT INTO fuel_audit(kind,record_key,before_data,after_data,actor) SELECT 'PURCHASE',?,?::jsonb,to_jsonb(p),? FROM fuel_purchase p WHERE id=?",Long.toString(id),before,actor,id);
    }
    @Transactional public void restorePurchase(long id,long version,String actor){
        lock();var old=purchase(id);if(((Number)old.get("version")).longValue()!=version||!Boolean.TRUE.equals(old.get("cancelled")))throw new IllegalArgumentException("Tankowanie zmieniło się lub jest już przywrócone.");
        // Reuse all receipt, amount and odometer checks; rollback keeps it cancelled on failure.
        db.update("UPDATE fuel_purchase SET cancelled=FALSE,cancel_reason='' WHERE id=? AND version=?",id,version);
        savePurchase(new Purchase(id,version,(UUID)old.get("request_token"),((java.sql.Timestamp)old.get("purchased_at")).toLocalDateTime(),(String)old.get("target"),(String)old.get("fuel_type"),old.get("amount").toString(),old.get("litres")==null?null:old.get("litres").toString(),old.get("litre_price")==null?null:old.get("litre_price").toString(),(Boolean)old.get("full_tank"),old.get("odometer")==null?null:old.get("odometer").toString(),(Boolean)old.get("records_complete"),(String)old.get("note"),(UUID)old.get("receipt_id")),actor);
    }
    public record Comparison(long ending,LocalDateTime start,LocalDateTime end,BigDecimal kilometres,BigDecimal litres,BigDecimal actualRate,BigDecimal expectedLitres,BigDecimal difference,BigDecimal percent,boolean complete,String note){}
    public List<Comparison> comparisons(LocalDate end){
        var rows=db.queryForList("SELECT * FROM fuel_purchase WHERE NOT cancelled AND target='CAR' AND purchased_at<? ORDER BY purchased_at,id",end.plusDays(1).atStartOfDay());var result=new ArrayList<Comparison>();Map<String,Object> first=null;var interval=new ArrayList<Map<String,Object>>();
        for(var row:rows){if(first!=null)interval.add(row);if(!(Boolean)row.get("full_tank"))continue;if(first!=null){
            var start=((java.sql.Timestamp)first.get("purchased_at")).toLocalDateTime();var finish=((java.sql.Timestamp)row.get("purchased_at")).toLocalDateTime();var km=n(row,"odometer").subtract(n(first,"odometer"));boolean valid=km.signum()>0&&(Boolean)row.get("records_complete");BigDecimal litres=BigDecimal.ZERO;
            for(var part:interval)if(part.get("litres")==null||!part.get("fuel_type").equals(first.get("fuel_type")))valid=false;else litres=litres.add(n(part,"litres"));
            var setting=parameters(start.toLocalDate());boolean expectedKnown=!setting.isEmpty()&&setting.get("car_fuel").equals(first.get("fuel_type"));
            if(expectedKnown)for(var change:parameters())if(!((java.sql.Date)change.get("effective_day")).toLocalDate().isBefore(start.toLocalDate())&&!((java.sql.Date)change.get("effective_day")).toLocalDate().isAfter(finish.toLocalDate())&&(!change.get("car_rate").equals(setting.get("car_rate"))||!change.get("car_fuel").equals(setting.get("car_fuel"))))expectedKnown=false;
            BigDecimal rate=valid?litres.multiply(new BigDecimal("100")).divide(km,3,RoundingMode.HALF_UP):null,expected=valid&&expectedKnown?km.multiply(n(setting,"car_rate")).divide(new BigDecimal("100")):null,diff=expected==null?null:litres.subtract(expected),percent=expected==null||expected.signum()==0?null:diff.multiply(new BigDecimal("100")).divide(expected,2,RoundingMode.HALF_UP);
            String note=!valid?"Niepełne: sprawdź litry, rodzaj paliwa, dystans i potwierdzenie kompletności tankowań.":!expectedKnown?"Spalanie rzeczywiste policzone; brak jednoznacznej normy dla całego odcinka.":percent!=null&&percent.compareTo(new BigDecimal("10"))>0?"Zużycie wyższe od oczekiwanego — sprawdź dane i warunki jazdy.":"Porównanie pełny–pełny, przy potwierdzonej kompletności wpisów.";
            result.add(new Comparison(((Number)row.get("id")).longValue(),start,finish,km,valid?litres:null,rate,expected==null?null:expected.setScale(3,RoundingMode.HALF_UP),diff==null?null:diff.setScale(3,RoundingMode.HALF_UP),percent,valid&&expectedKnown,note));
        }first=row;interval.clear();}
        Collections.reverse(result);return result.stream().limit(20).toList();
    }
}
