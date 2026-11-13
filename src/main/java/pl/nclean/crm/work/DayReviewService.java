package pl.nclean.crm.work;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.records.*;

@Service
@Transactional(readOnly=true)
public class DayReviewService {
    private final JdbcTemplate db; private final WorkService work; private final CrmRecords records; private final ActualUsageService actual;
    private final pl.nclean.crm.tycoon.AchievementService achievements;
    public DayReviewService(JdbcTemplate db,WorkService work,CrmRecords records,ActualUsageService actual,pl.nclean.crm.tycoon.AchievementService achievements){this.db=db;this.work=work;this.records=records;this.actual=actual;this.achievements=achievements;}
    public record Review(LocalDate day,List<Map<String,Object>> orders,List<Map<String,Object>> moves,int complete,String fingerprint,boolean closed,boolean changed){}
    public Review review(LocalDate day){
        DashboardService.period(day,"day");
        var orders=work.orders(day);int complete=0;
        for(var order:orders){
            long id=((Number)order.get("id")).longValue();var missing=new ArrayList<String>();
            if("PLANNED".equals(order.get("status")))missing.add("potwierdź, czy usługa się odbyła");
            if("DONE".equals(order.get("status"))){
                if(order.get("final_total")==null)missing.add("kwota końcowa");
                if(Boolean.TRUE.equals(order.get("missing_chem")))missing.add("rzeczywista chemia");
                for(String stage:List.of("BEFORE","AFTER"))if(db.queryForObject("SELECT count(*) FROM booking_photo WHERE booking_id=? AND stage=?",Integer.class,id,stage)==0 && ((String)order.get("photo_"+(stage.equals("BEFORE")?"before":"after")+"_exception")).isBlank())missing.add(stage.equals("BEFORE")?"zdjęcie przed":"zdjęcie po");
                if(((String)order.get("completion_note")).isBlank()&&!Boolean.TRUE.equals(order.get("completion_no_notes")))missing.add("notatka po wykonaniu lub Bez uwag");
            }
            order.put("missing",missing);order.put("complete",missing.isEmpty());if(missing.isEmpty())complete++;
        }
        var moves=db.queryForList("SELECT m.*,b.customer_name,b.starts FROM booking_move m JOIN booking b ON b.id=m.booking_id WHERE from_day=? ORDER BY m.id",day);
        var fingerprint=fingerprint(day);var saved=db.queryForList("SELECT * FROM day_review WHERE day=?",day);boolean closed=!saved.isEmpty()&&fingerprint.equals(saved.get(0).get("fingerprint"))&&complete==orders.size();
        return new Review(day,orders,moves,complete,fingerprint,closed,!saved.isEmpty()&&!closed);
    }
    private String fingerprint(LocalDate day){return db.queryForObject("""
        SELECT md5(jsonb_build_array(
          (SELECT COALESCE(jsonb_agg(to_jsonb(b) ORDER BY id),'[]') FROM booking b WHERE day=?),
          (SELECT COALESCE(jsonb_agg(to_jsonb(i) ORDER BY booking_id,position),'[]') FROM booking_item i WHERE booking_id IN (SELECT id FROM booking WHERE day=?)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(a) ORDER BY booking_id,item_position,chemical_id),'[]') FROM booking_actual_chemical a WHERE booking_id IN (SELECT id FROM booking WHERE day=?)),
          (SELECT COALESCE(jsonb_agg(jsonb_build_object('id',p.id,'booking_id',p.booking_id,'stage',p.stage,'name',p.original_name,'time',p.captured_at) ORDER BY p.id),'[]') FROM booking_photo p WHERE booking_id IN (SELECT id FROM booking WHERE day=?)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(m) ORDER BY id),'[]') FROM booking_move m WHERE from_day=?)
        )::text)
        """,String.class,day,day,day,day,day);}
    @Transactional public void close(LocalDate day,String expected,String actor){
        if(day.isAfter(DashboardService.today()))throw new IllegalArgumentException("Dzień możesz zamknąć najwcześniej w dniu pracy.");
        // Prevent a new booking or photo/measurement write between validation and closure.
        db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        db.execute("LOCK TABLE booking,booking_item,booking_actual_chemical,booking_photo,booking_move,day_review IN SHARE ROW EXCLUSIVE MODE");
        var review=review(day);if(!review.fingerprint.equals(expected))throw new IllegalArgumentException("Dane zmieniły się w innym oknie. Odśwież dzień i sprawdź zmiany.");
        if(review.complete!=review.orders.size())throw new IllegalArgumentException("Uzupełnij: "+review.orders.stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))).map(o->o.get("customer_name")+": "+o.get("missing")).findFirst().orElse("brakujące dane"));
        db.update("INSERT INTO day_review(day,fingerprint,closed_by) VALUES (?,?,?) ON CONFLICT(day) DO UPDATE SET fingerprint=EXCLUDED.fingerprint,closed_by=EXCLUDED.closed_by,closed_at=CURRENT_TIMESTAMP",day,expected,actor);
        achievements.dayClosed(day,review.orders.size());
    }
    @Transactional public void save(long id,LocalDate day,Map<String,String> p,String actor){
        db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        var b=db.queryForList("SELECT * FROM booking WHERE id=? FOR UPDATE",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznane zlecenie."));
        long version=Long.parseLong(p.get("version"));
        if(((Number)b.get("version")).longValue()!=version||!((java.sql.Date)b.get("day")).toLocalDate().equals(day))throw new IllegalArgumentException("Zlecenie zmieniło się w innym oknie. Zachowaj dane i odśwież widok przed korektą.");
        var outcome=p.getOrDefault("outcome","");if(!Set.of("DONE","CANCELLED","MOVED","UNKNOWN").contains(outcome))throw new IllegalArgumentException("Wybierz, czy zlecenie się odbyło.");
        String note=limited(p.get("note"),2000),before=limited(p.get("before_exception"),300),after=limited(p.get("after_exception"),300);
        boolean noNotes=p.containsKey("no_notes");if(noNotes&&!note.isBlank())throw new IllegalArgumentException("Wpisz notatkę albo wybierz Bez uwag.");
        String original=db.queryForObject("SELECT to_jsonb(b)::text FROM booking b WHERE id=?",String.class,id);
        if(outcome.equals("DONE")){
            if(day.isAfter(DashboardService.today()))throw new IllegalArgumentException("Nie potwierdzaj wykonania przyszłej wizyty.");
            var measured=new ArrayList<WorkService.Measurement>();var zero=new ArrayList<int[]>();
            for(var item:records.items(id)){
                int pos=((Number)item.get("position")).intValue();String mode=p.getOrDefault("chem_"+pos,"KEEP");
                if(!Set.of("KEEP","MEASURED","ZERO").contains(mode))throw new IllegalArgumentException("Wybierz sposób potwierdzenia chemii.");
                if(mode.equals("ZERO"))zero.add(new int[]{pos});
                if(mode.equals("MEASURED")){
                    var values=new LinkedHashMap<Long,BigDecimal>();String prefix="usage_"+pos+"_";
                    p.forEach((key,value)->{if(key.startsWith(prefix)&&!value.isBlank())values.put(Long.parseLong(key.substring(prefix.length())),DashboardService.number(value,3,new BigDecimal("99999999999.999"),false));});
                    if(values.isEmpty())throw new IllegalArgumentException("Podaj ilości albo wybierz Nie użyto chemii.");
                    measured.add(new WorkService.Measurement(pos,Long.parseLong(p.get("actualVersion_"+pos)),values));
                }
            }
            if("CANCELLED".equals(b.get("status")))throw new IllegalArgumentException("Przywróć anulowane zlecenie do planu w jego szczegółach.");
            // Preserve existing allocation and duration; no guessed distribution across services.
            var prices=new LinkedHashMap<Integer,BigDecimal>();var times=new LinkedHashMap<Integer,Integer>();
            for(var item:records.items(id)){int pos=((Number)item.get("position")).intValue();if(item.get("final_amount")!=null)prices.put(pos,(BigDecimal)item.get("final_amount"));if(item.get("work_minutes")!=null)times.put(pos,((Number)item.get("work_minutes")).intValue());}
            if(p.containsKey("allocations")){
                prices.clear();times.clear();
                for(var item:records.items(id)){int pos=((Number)item.get("position")).intValue();String amount=p.get("amount_"+pos),minutes=p.get("time_"+pos);if(amount!=null&&!amount.isBlank())prices.put(pos,WorkService.money(amount));if(minutes!=null&&!minutes.isBlank())times.put(pos,WorkService.minutes(minutes));}
            }else if(b.get("final_total")==null||WorkService.money(p.get("final_total")).compareTo((BigDecimal)b.get("final_total"))!=0)prices.clear();
            work.complete(id,version,p.get("final_total"),b.get("timer_started")!=null?null:p.getOrDefault("minutes",Objects.toString(b.get("work_minutes"),"")),note,prices,times,measured,actor);
            for(var z:zero)actual.zero(id,z[0],Long.parseLong(p.get("actualVersion_"+z[0])));
            db.update("UPDATE booking SET completion_no_notes=?,photo_before_exception=?,photo_after_exception=? WHERE id=?",noNotes,before,after,id);
        }else{
            var target=day;var starts=((java.sql.Time)b.get("starts")).toLocalTime();
            if(outcome.equals("MOVED")){
                target=LocalDate.parse(p.get("new_day"));starts=LocalTime.parse(p.get("new_starts"));
                if(target.equals(day))throw new IllegalArgumentException("Nowy termin musi być w innym dniu. Godzinę tego dnia zmienisz w szczegółach.");
                db.update("INSERT INTO booking_move(booking_id,from_day,to_day,reason) VALUES (?,?,?,?)",id,day,target,limited(p.get("reason"),300));
            }
            if(outcome.equals("CANCELLED")&&work.paid(id).signum()>0)throw new IllegalArgumentException("Najpierw rozlicz wpłatę w szczegółach zlecenia.");
            records.reschedule(id,version,target,starts,outcome.equals("CANCELLED")?"CANCELLED":"PLANNED");
            db.update("UPDATE booking SET review_reason=? WHERE id=?",limited(p.get("reason"),300),id);
            if(!note.isBlank()||noNotes)db.update("UPDATE booking SET completion_note=?,completion_no_notes=? WHERE id=?",note,noNotes,id);
        }
        db.update("INSERT INTO booking_audit(booking_id,before_data,after_data,actor) SELECT ?,?::jsonb,to_jsonb(b),? FROM booking b WHERE id=?",id,original,actor,id);
    }
    private static String limited(String value,int max){String v=value==null?"":value.strip();if(v.length()>max)throw new IllegalArgumentException("Tekst przekracza limit "+max+" znaków.");return v;}
}
