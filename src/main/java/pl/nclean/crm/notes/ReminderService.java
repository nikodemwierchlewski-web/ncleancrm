package pl.nclean.crm.notes;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.BusinessDates;

@Service
@Transactional(readOnly=true)
public class ReminderService {
    private final JdbcTemplate db;
    public ReminderService(JdbcTemplate db){this.db=db;}
    private ReminderSchedule.Config config(Map<String,Object> row){return new ReminderSchedule.Config((String)row.get("frequency"),((java.sql.Date)row.get("starts")).toLocalDate(),((java.sql.Time)row.get("at_time")).toLocalTime(),((Number)row.get("weekdays")).intValue(),((Number)row.get("interval_days")).intValue());}
    public ReminderSchedule.Config configuration(Long id){
        var rows=id==null?List.<Map<String,Object>>of():db.queryForList("SELECT * FROM memo_reminder WHERE entry_id=?",id);
        return rows.isEmpty()?new ReminderSchedule.Config("NONE",BusinessDates.today(),LocalTime.of(9,0),0,1):config(rows.get(0));
    }
    @Transactional public void configure(long id,ReminderSchedule.Config choice){
        if(choice==null)return;
        db.queryForObject("SELECT id FROM memo_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        var rows=db.queryForList("SELECT * FROM memo_reminder WHERE entry_id=?",id);
        if(!rows.isEmpty()&&config(rows.get(0)).equals(choice))return;
        db.update("INSERT INTO memo_reminder(entry_id,frequency,starts,at_time,weekdays,interval_days) VALUES (?,?,?,?,?,?) ON CONFLICT(entry_id) DO UPDATE SET revision=memo_reminder.revision+1,frequency=excluded.frequency,starts=excluded.starts,at_time=excluded.at_time,weekdays=excluded.weekdays,interval_days=excluded.interval_days",id,choice.frequency(),choice.starts(),choice.time(),choice.weekdays(),choice.intervalDays());
    }
    public record Pending(long id,long revision,LocalDate occurrence,Instant at,String title,String kind,String frequency,boolean seen){}
    public List<Pending> pending(){return pending(Instant.now());}
    public List<Pending> pending(Instant now){
        var rows=db.queryForList("SELECT r.*,e.title,e.kind FROM memo_reminder r JOIN memo_entry e ON e.id=r.entry_id WHERE r.frequency<>'NONE' AND NOT e.archived AND NOT e.completed AND r.starts<=?",now.atZone(BusinessDates.WARSAW).toLocalDate());
        var events=new HashMap<Long,Map<String,Object>>();
        for(var row:db.queryForList("SELECT DISTINCT ON (v.entry_id) v.* FROM memo_reminder_event v JOIN memo_reminder r ON r.entry_id=v.entry_id AND r.revision=v.revision ORDER BY v.entry_id,v.scheduled_date DESC"))events.put(((Number)row.get("entry_id")).longValue(),row);
        var result=new ArrayList<Pending>();
        for(var row:rows){var occurrence=config(row).latest(now);if(occurrence.isEmpty())continue;long id=((Number)row.get("entry_id")).longValue();var event=events.get(id);var due=occurrence.get();boolean same=event!=null&&due.day().equals(((java.sql.Date)event.get("scheduled_date")).toLocalDate());
            if(same&&("DONE".equals(event.get("state"))||event.get("snoozed_until")!=null&&((Timestamp)event.get("snoozed_until")).toInstant().isAfter(now)))continue;
            result.add(new Pending(id,((Number)row.get("revision")).longValue(),due.day(),due.at(),(String)row.get("title"),(String)row.get("kind"),(String)row.get("frequency"),same&&event.get("seen_at")!=null));
        }
        result.sort(Comparator.comparing(Pending::at).thenComparingLong(Pending::id));return List.copyOf(result);
    }
    @Transactional public boolean action(long id,long revision,LocalDate occurrence,String action,int minutes){return action(id,revision,occurrence,action,minutes,Instant.now());}
    @Transactional public boolean action(long id,long revision,LocalDate occurrence,String action,int minutes,Instant now){
        if(!Set.of("seen","done","snooze").contains(Objects.toString(action,""))||action.equals("snooze")&&!Set.of(15,60,180).contains(minutes))throw new IllegalArgumentException("Wybierz wykonanie lub odłożenie o 15, 60 albo 180 minut.");
        db.queryForObject("SELECT id FROM memo_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        var due=pending(now).stream().filter(p->p.id==id&&p.revision==revision&&p.occurrence.equals(occurrence)).findFirst();
        if(due.isEmpty()){
            // A repeated acknowledgement is harmless; it cannot modify another occurrence.
            if(action.equals("seen"))return false;
            boolean done="done".equals(action)&&db.queryForObject("SELECT count(*) FROM memo_reminder_event WHERE entry_id=? AND revision=? AND scheduled_date=? AND state='DONE'",Integer.class,id,revision,occurrence)==1;
            if(done)return false;
            throw new IllegalArgumentException("Przypomnienie zmieniło się lub zostało już odłożone. Odśwież listę.");
        }
        var p=due.get();
        if(action.equals("seen"))return db.update("INSERT INTO memo_reminder_event(entry_id,revision,scheduled_date,scheduled_at,state,seen_at) VALUES (?,?,?,?,'PENDING',?) ON CONFLICT(entry_id,revision,scheduled_date) DO UPDATE SET seen_at=excluded.seen_at WHERE memo_reminder_event.seen_at IS NULL",id,revision,occurrence,Timestamp.from(p.at),Timestamp.from(now))==1;
        db.update("INSERT INTO memo_reminder_event(entry_id,revision,scheduled_date,scheduled_at,state,snoozed_until,seen_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(entry_id,revision,scheduled_date) DO UPDATE SET state=excluded.state,snoozed_until=excluded.snoozed_until,seen_at=COALESCE(memo_reminder_event.seen_at,excluded.seen_at),updated_at=CURRENT_TIMESTAMP",id,revision,occurrence,Timestamp.from(p.at),action.equals("done")?"DONE":"SNOOZED",action.equals("snooze")?Timestamp.from(now.plusSeconds(minutes*60L)):null,Timestamp.from(now));
        return true;
    }
}
