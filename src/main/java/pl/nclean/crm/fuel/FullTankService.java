package pl.nclean.crm.fuel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.BusinessDates;

/** Full-to-full measurements are separate from cash expenses on purchase dates. */
@Service
@Transactional(readOnly=true)
public class FullTankService {
    private final JdbcTemplate db;
    private final FuelJournalService journal;
    public FullTankService(JdbcTemplate db,FuelJournalService journal){this.db=db;this.journal=journal;}
    private static void target(String target){if(!Set.of("CAR","GENERATOR").contains(Objects.toString(target,"")))throw new IllegalArgumentException("Wybierz samochód lub agregat.");}
    public static String fuelType(String target){target(target);return target.equals("CAR")?"DIESEL":"PETROL";}
    private static String column(String target){target(target);return target.equals("CAR")?"odometer":"generator_meter";}
    private static BigDecimal meter(String value,String target){return FuelJournalService.decimal(value,target.equals("CAR")?1:2,target.equals("CAR")?"999999999.9":"9999999999.99",false);}
    private static LocalDateTime time(Map<String,Object> row,String field){Object value=row.get(field);return value instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)value;}
    private static void date(LocalDateTime time){if(time==null||time.isAfter(LocalDateTime.now(ZoneId.of("Europe/Warsaw")).plusMinutes(5)))throw new IllegalArgumentException("Podaj datę i godzinę bez przyszłych wpisów.");BusinessDates.period(time.toLocalDate(),"day");}
    public Map<String,Map<String,Object>> baselines(){
        var result=new LinkedHashMap<String,Map<String,Object>>();
        for(String target:List.of("CAR","GENERATOR"))result.put(target,Map.of("version",-1));
        for(var row:db.queryForList("SELECT * FROM fuel_baseline")){row.put("started_at",time(row,"started_at"));result.put((String)row.get("target"),row);}
        return result;
    }
    @Transactional public void saveBaseline(String target,long version,UUID token,LocalDateTime when,String reading,String actor){
        target(target);date(when);var value=meter(reading,target);
        db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        var rows=db.queryForList("SELECT *,to_jsonb(b)::text previous FROM fuel_baseline b WHERE target=?",target);
        var old=rows.isEmpty()?null:rows.get(0);
        var replay=db.queryForList("SELECT * FROM fuel_baseline WHERE request_token=?",token);
        if(!replay.isEmpty()){
            var saved=replay.get(0);
            if(target.equals(saved.get("target"))&&when.equals(time(saved,"started_at"))&&value.compareTo((BigDecimal)saved.get("reading"))==0)return;
            throw new IllegalArgumentException("Ten formularz stanu początkowego został już zapisany z innymi danymi. Odśwież widok.");
        }
        if(old==null&&version!=-1||old!=null&&((Number)old.get("version")).longValue()!=version)throw new IllegalArgumentException("Stan początkowy zmienił się w innym oknie. Odśwież formularz.");
        var earliest=db.queryForObject("SELECT min(purchased_at) FROM fuel_purchase WHERE target=? AND metered_full AND NOT cancelled",java.sql.Timestamp.class,target);
        if(earliest!=null&&when.isAfter(earliest.toLocalDateTime()))throw new IllegalArgumentException("Stan początkowy musi poprzedzać pierwsze zapisane tankowanie do pełna.");
        String column=column(target);
        var previous=db.queryForList("SELECT "+column+" reading FROM fuel_purchase WHERE NOT cancelled AND target=? AND "+column+" IS NOT NULL AND purchased_at<? ORDER BY purchased_at DESC,id DESC LIMIT 1",target,when);
        var next=db.queryForList("SELECT "+column+" reading FROM fuel_purchase WHERE NOT cancelled AND target=? AND "+column+" IS NOT NULL AND purchased_at>=? ORDER BY purchased_at,id LIMIT 1",target,when);
        if(!previous.isEmpty()&&value.compareTo((BigDecimal)previous.get(0).get("reading"))<0||!next.isEmpty()&&value.compareTo((BigDecimal)next.get(0).get("reading"))>0)throw new IllegalArgumentException("Stan początkowy jest sprzeczny z licznikami zapisanych tankowań.");
        if(old==null)db.update("INSERT INTO fuel_baseline(target,started_at,reading,request_token,updated_by) VALUES (?,?,?,?,?)",target,when,value,token,actor);
        else db.update("UPDATE fuel_baseline SET started_at=?,reading=?,request_token=?,updated_by=?,updated_at=CURRENT_TIMESTAMP,version=version+1 WHERE target=?",when,value,token,actor,target);
        db.update("INSERT INTO fuel_audit(kind,record_key,before_data,after_data,actor) SELECT 'BASELINE',?,?::jsonb,to_jsonb(b),? FROM fuel_baseline b WHERE target=?",target,old==null?null:old.get("previous"),actor,target);
    }
    @Transactional public long save(Map<String,String> fields,String actor){
        String target=fields.get("target");target(target);
        var reading=meter(fields.get("meter"),target);
        var litres=FuelJournalService.decimal(fields.get("litres"),3,"9999999.999",false);
        if(litres.signum()<=0)throw new IllegalArgumentException("Podaj dodatnią liczbę litrów.");
        String id=fields.get("id"),receipt=fields.get("receipt_id");
        return journal.savePurchase(new FuelJournalService.Purchase(id==null||id.isBlank()?null:Long.valueOf(id),Long.parseLong(fields.get("version")),UUID.fromString(fields.get("request_token")),LocalDateTime.parse(fields.get("purchased_at")),target,fuelType(target),fields.get("amount"),litres.toPlainString(),null,true,target.equals("CAR")?reading.toPlainString():null,true,fields.getOrDefault("note",""),receipt==null||receipt.isBlank()?null:UUID.fromString(receipt),target.equals("GENERATOR")?reading.toPlainString():null,true),actor);
    }
    public record Reading(LocalDateTime time,BigDecimal value){}
    public Reading latestReading(String target,LocalDate through){
        var rows=db.queryForList("SELECT purchased_at time,"+column(target)+" reading FROM fuel_purchase WHERE NOT cancelled AND target=? AND "+column(target)+" IS NOT NULL AND purchased_at<? ORDER BY purchased_at DESC,id DESC LIMIT 1",target,through.plusDays(1).atStartOfDay());
        var bases=db.queryForList("SELECT started_at time,reading FROM fuel_baseline WHERE target=? AND started_at<?",target,through.plusDays(1).atStartOfDay());
        Map<String,Object> row=rows.isEmpty()?null:rows.get(0);
        if(!bases.isEmpty()&&(row==null||time(bases.get(0),"time").isAfter(time(row,"time"))))row=bases.get(0);
        return row==null?null:new Reading(time(row,"time"),(BigDecimal)row.get("reading"));
    }
    public record Interval(long ending,String target,LocalDateTime start,LocalDateTime end,BigDecimal distance,BigDecimal litres,BigDecimal rate,boolean complete){}
    public List<Interval> intervals(LocalDate start,LocalDate end){
        var result=new ArrayList<Interval>();
        for(String target:List.of("CAR","GENERATOR")){
            var rows=new ArrayList<>(db.queryForList("SELECT * FROM fuel_purchase WHERE NOT cancelled AND target=? AND purchased_at<? ORDER BY purchased_at,id",target,end.plusDays(1).atStartOfDay()));
            for(var base:db.queryForList("SELECT * FROM fuel_baseline WHERE target=? AND started_at<?",target,end.plusDays(1).atStartOfDay())){
                var row=new HashMap<String,Object>();row.put("purchased_at",base.get("started_at"));row.put("id",-1L);row.put("baseline",true);row.put(column(target),base.get("reading"));rows.add(row);
            }
            rows.sort(Comparator.comparing((Map<String,Object> row)->time(row,"purchased_at")).thenComparingLong(row->((Number)row.get("id")).longValue()));
            Map<String,Object> opening=null;var interval=new ArrayList<Map<String,Object>>();
            for(var row:rows){
                if(Boolean.TRUE.equals(row.get("baseline"))){opening=row;interval.clear();continue;}
                if(opening!=null)interval.add(row);
                if(!Boolean.TRUE.equals(row.get("full_tank"))||row.get(column(target))==null)continue;
                if(opening!=null&&!time(row,"purchased_at").toLocalDate().isBefore(start)){
                    var distance=((BigDecimal)row.get(column(target))).subtract((BigDecimal)opening.get(column(target)));
                    boolean complete=distance.signum()>0&&Boolean.TRUE.equals(row.get("records_complete"))&&(Boolean.TRUE.equals(opening.get("baseline"))||fuelType(target).equals(opening.get("fuel_type")));
                    var litres=BigDecimal.ZERO;
                    for(var part:interval){if(part.get("litres")==null||!fuelType(target).equals(part.get("fuel_type")))complete=false;else litres=litres.add((BigDecimal)part.get("litres"));}
                    var rate=complete?litres.multiply(target.equals("CAR")?new BigDecimal("100"):BigDecimal.ONE).divide(distance,3,RoundingMode.HALF_UP):null;
                    result.add(new Interval(((Number)row.get("id")).longValue(),target,time(opening,"purchased_at"),time(row,"purchased_at"),distance,complete?litres:null,rate,complete));
                }
                opening=row;interval.clear();
            }
        }
        result.sort(Comparator.comparing(Interval::end).reversed());return result;
    }
}
