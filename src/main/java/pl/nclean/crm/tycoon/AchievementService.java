package pl.nclean.crm.tycoon;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.dashboard.DashboardService;

/** Shared owner CRM milestones. Writes join the successful domain transaction. */
@Service
@Transactional(readOnly=true)
public class AchievementService {
    public record Badge(String code,String title,String description,int target,String icon){}
    public static final List<Badge> BADGES=List.of(
        new Badge("FIRST_CLEAN","Pierwszy czysty efekt","Pierwsze wykonane zlecenie",1,"drop"),
        new Badge("TEN_CLEAN","Dobry rytm","10 wykonanych zleceń",10,"sofa"),
        new Badge("FIFTY_CLEAN","Sprawna firma","50 wykonanych zleceń",50,"van"),
        new Badge("FIRST_DAY","Wszystko dopięte","Pierwszy sprawdzony dzień z usługami",1,"check"),
        new Badge("FIRST_GOAL","Cel osiągnięty","Pierwszy świadomie ukończony cel",1,"star"));
    private final JdbcTemplate db;
    public AchievementService(JdbcTemplate db){this.db=db;}
    private void lock(){db.queryForObject("SELECT id FROM tycoon_write_guard WHERE id=1 FOR UPDATE",Integer.class);}
    public long performedCount(){return db.queryForObject("SELECT count(*) FROM booking WHERE removed_at IS NULL AND status='DONE' AND day<=?",Long.class,DashboardService.today());}
    private void award(String code,String source,long value){db.update("INSERT INTO tycoon_award(code,source_key,source_value) VALUES (?,?,?) ON CONFLICT(code) DO NOTHING",code,source,value);}
    @Transactional public void performed(long id){
        lock();if(db.queryForObject("SELECT count(*) FROM booking WHERE removed_at IS NULL AND id=? AND status='DONE' AND day<=?",Integer.class,id,DashboardService.today())==0)return;
        long count=performedCount();for(var badge:BADGES)if(badge.code.endsWith("_CLEAN")&&count>=badge.target)award(badge.code,Long.toString(id),count);
    }
    @Transactional public void goalCompleted(long id){lock();if(db.queryForObject("SELECT count(*) FROM memo_entry WHERE id=? AND kind='GOAL' AND completed",Integer.class,id)==1)award("FIRST_GOAL",Long.toString(id),1);}
    @Transactional public void dayClosed(LocalDate day,int count){if(count==0)return;lock();award("FIRST_DAY",day.toString(),count);}
    public List<Map<String,Object>> badges(){
        var earned=new HashMap<String,Map<String,Object>>();db.queryForList("SELECT * FROM tycoon_award").forEach(a->earned.put((String)a.get("code"),a));
        long done=performedCount();var list=new ArrayList<Map<String,Object>>();
        for(var b:BADGES){var row=new LinkedHashMap<String,Object>();row.put("code",b.code);row.put("title",b.title);row.put("description",b.description);row.put("target",b.target);row.put("icon",b.icon);row.put("earned",earned.containsKey(b.code));row.put("current",b.code.endsWith("_CLEAN")?done:null);var a=earned.get(b.code);row.put("earnedAt",a==null?null:((java.sql.Timestamp)a.get("earned_at")).toInstant().toString());row.put("origin",a==null?null:a.get("origin"));row.put("pending",a!=null&&a.get("notified_at")==null);list.add(row);}return list;
    }
    /** At-most-once notification claim, also across tabs and JVM restarts. */
    @Transactional public boolean acknowledge(String code){if(BADGES.stream().noneMatch(b->b.code.equals(code)))throw new IllegalArgumentException("Nieznana odznaka.");return db.update("UPDATE tycoon_award SET notified_at=CURRENT_TIMESTAMP WHERE code=? AND notified_at IS NULL",code)==1;}
}
