package pl.nclean.crm.notifications;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.BusinessDates;

/** Durable definitions and an idempotent in-app outbox. No device/provider dependencies. */
@Service
@Transactional(readOnly=true)
public class EntityReminderService {
    private final JdbcTemplate db;
    public EntityReminderService(JdbcTemplate db){this.db=db;}
    public record Source(String type,long id,String key,String title,String href,Instant anchor,boolean active){}
    public record Plan(Source source,long version,List<ReminderPlan.Spec> reminders){}
    public record Notification(UUID id,String sourceType,long sourceId,String title,String href,Instant scheduledAt,Instant availableAt,long version,boolean displayed){}
    private static Instant instant(Object value){return value==null?null:((Timestamp)value).toInstant();}
    private static Timestamp stamp(Instant value){return value==null?null:Timestamp.from(value.truncatedTo(java.time.temporal.ChronoUnit.MICROS));}
    private void lock(){db.queryForObject("SELECT pg_advisory_xact_lock(442019)",(rs,n)->1);}
    public Source source(String type,long id){
        if(id<=0||!Set.of("BOOKING","MEMO").contains(type))throw new IllegalArgumentException("Wybierz zlecenie, notatkę lub cel.");
        var rows=db.queryForList(type.equals("BOOKING")?"SELECT customer_name title,day AS anchor_day,starts AS anchor_time,removed_at IS NULL AND status='PLANNED' active FROM booking WHERE id=?":"SELECT title,due AS anchor_day,due_time AS anchor_time,NOT completed AND NOT archived active FROM memo_entry WHERE id=?",id);
        if(rows.isEmpty())throw new IllegalArgumentException("Nie znaleziono powiązanego wpisu.");
        var r=rows.get(0);Instant anchor=r.get("anchor_day")==null||r.get("anchor_time")==null?null:((java.sql.Date)r.get("anchor_day")).toLocalDate().atTime(((java.sql.Time)r.get("anchor_time")).toLocalTime()).atZone(BusinessDates.WARSAW).toInstant();
        return new Source(type,id,type+":"+id,(String)r.get("title"),type.equals("BOOKING")?"/work/"+id:"/notes?entry="+id,anchor,(Boolean)r.get("active"));
    }
    public Plan plan(String type,long id){
        var s=source(type,id);var versions=db.queryForList("SELECT version FROM reminder_owner WHERE source_key=?",Long.class,s.key());
        var specs=db.query("SELECT * FROM entity_reminder WHERE source_key=? AND enabled ORDER BY id",(rs,n)->new ReminderPlan.Spec(rs.getObject("id",UUID.class),rs.getObject("offset_minutes",Long.class),instant(rs.getTimestamp("absolute_at"))),s.key());
        return new Plan(s,versions.isEmpty()?0:versions.get(0),specs);
    }
    @Transactional public Plan save(String type,long id,ReminderPlan.Edit edit){
        if(edit==null)return plan(type,id);
        lock();var old=plan(type,id);var s=old.source();
        if(edit.reminders().stream().anyMatch(r->r.offsetMinutes()!=null)&&s.anchor()==null)throw new IllegalArgumentException("Najpierw podaj termin i godzinę wpisu albo wybierz konkretną datę przypomnienia.");
        if(new HashSet<>(old.reminders()).equals(new HashSet<>(edit.reminders())))return old; // exact retry
        if(old.version()!=edit.version())throw new IllegalArgumentException("Przypomnienia zmieniły się w innym oknie. Otwórz wpis ponownie; pola pozostają tutaj.");
        db.update("INSERT INTO reminder_owner(source_key,booking_id,memo_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",s.key(),type.equals("BOOKING")?id:null,type.equals("MEMO")?id:null);
        for(var r:edit.reminders()){
            var owners=db.queryForList("SELECT source_key FROM entity_reminder WHERE id=?",String.class,r.id());
            if(!owners.isEmpty()&&!owners.get(0).equals(s.key()))throw new IllegalArgumentException("Przypomnienie należy do innego wpisu.");
        }
        db.update("UPDATE entity_reminder SET enabled=FALSE WHERE source_key=?",s.key());
        for(var r:edit.reminders())db.update("INSERT INTO entity_reminder(id,source_key,offset_minutes,absolute_at) VALUES (?,?,?,?) ON CONFLICT(id) DO UPDATE SET offset_minutes=EXCLUDED.offset_minutes,absolute_at=EXCLUDED.absolute_at,enabled=TRUE",r.id(),s.key(),r.offsetMinutes(),stamp(r.absoluteAt()));
        db.update("UPDATE reminder_owner SET version=version+1 WHERE source_key=?",s.key());
        reconcileLocked();return plan(type,id);
    }
    /** Called within the entity transaction to prevent silently losing a relative deadline. */
    public void validateAnchor(String type,long id){var p=plan(type,id);if(p.source().anchor()==null&&p.reminders().stream().anyMatch(r->r.offsetMinutes()!=null))throw new IllegalArgumentException("Wpis ma przypomnienia przed terminem. Podaj też godzinę albo usuń te przypomnienia.");}
    private void reconcileLocked(){
        db.update("UPDATE notification_occurrence SET valid=FALSE WHERE valid");
        for(var owner:db.queryForList("SELECT booking_id,memo_id FROM reminder_owner")){
            boolean booking=owner.get("booking_id")!=null;var p=plan(booking?"BOOKING":"MEMO",((Number)owner.get(booking?"booking_id":"memo_id")).longValue());
            if(!p.source().active())continue;
            for(var r:p.reminders()){
                var at=r.scheduled(p.source().anchor());if(at==null)continue;
                db.update("INSERT INTO notification_occurrence(id,reminder_id,scheduled_at) VALUES (?,?,?) ON CONFLICT(reminder_id,scheduled_at) DO UPDATE SET valid=TRUE",UUID.randomUUID(),r.id(),stamp(at));
            }
        }
    }
    @Transactional public List<Notification> due(){return due(Instant.now());}
    @Transactional public List<Notification> due(Instant now){lock();reconcileLocked();return dueLocked(now);}
    private List<Notification> dueLocked(Instant now){
        var result=new ArrayList<Notification>();
        for(var r:db.queryForList("SELECT o.*,r.source_key,p.booking_id,p.memo_id FROM notification_occurrence o JOIN entity_reminder r ON r.id=o.reminder_id JOIN reminder_owner p ON p.source_key=r.source_key WHERE o.valid AND o.state<>'READ' AND GREATEST(o.scheduled_at,COALESCE(o.snoozed_until,o.scheduled_at))<=? ORDER BY GREATEST(o.scheduled_at,COALESCE(o.snoozed_until,o.scheduled_at)),o.id LIMIT 100",stamp(now))){
            boolean booking=r.get("booking_id")!=null;var s=source(booking?"BOOKING":"MEMO",((Number)r.get(booking?"booking_id":"memo_id")).longValue());
            result.add(new Notification((UUID)r.get("id"),s.type(),s.id(),s.title(),s.href(),instant(r.get("scheduled_at")),r.get("snoozed_until")==null?instant(r.get("scheduled_at")):instant(r.get("snoozed_until")),((Number)r.get("version")).longValue(),r.get("displayed_at")!=null));
        }
        return List.copyOf(result);
    }
    @Transactional public boolean acknowledge(UUID id,long version,String action,int minutes){return acknowledge(id,version,action,minutes,Instant.now());}
    @Transactional public boolean acknowledge(UUID id,long version,String action,int minutes,Instant now){
        if(!Set.of("DISPLAYED","READ","SNOOZE").contains(action)||minutes<1||minutes>10080)throw new IllegalArgumentException("Wybierz odczytanie albo odroczenie od 1 minuty do 7 dni.");
        lock();reconcileLocked();
        var rows=db.queryForList("SELECT * FROM notification_occurrence WHERE id=?",id);
        if(rows.isEmpty()||!Boolean.TRUE.equals(rows.get(0).get("valid")))throw new IllegalArgumentException("Termin przypomnienia zmienił się albo wpis nie jest już aktywny.");
        var r=rows.get(0);if("READ".equals(r.get("state")))return false;
        var available=instant(r.get("snoozed_until")==null?r.get("scheduled_at"):r.get("snoozed_until"));
        if(available.isAfter(now)||((Number)r.get("version")).longValue()!=version)throw new IllegalArgumentException("Powiadomienie zostało zmienione. Odśwież dane.");
        if(action.equals("DISPLAYED"))return db.update("UPDATE notification_occurrence SET displayed_at=? WHERE id=? AND displayed_at IS NULL",stamp(now),id)==1;
        db.update("UPDATE notification_occurrence SET state=?,snoozed_until=?,displayed_at=CASE WHEN ? THEN NULL ELSE displayed_at END,version=version+1 WHERE id=?",action.equals("READ")?"READ":"SNOOZED",action.equals("SNOOZE")?stamp(now.plusSeconds(minutes*60L)):null,action.equals("SNOOZE"),id);return true;
    }
}
