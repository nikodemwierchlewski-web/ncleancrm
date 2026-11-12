package pl.nclean.crm.notes;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.dashboard.DashboardService;

@Service
@Transactional(readOnly=true)
public class NoteService {
    public record Step(UUID id,String text,boolean done){}
    public record Entry(Long id,long version,UUID token,String kind,String title,String content,LocalDate due,boolean pinned,boolean completed,boolean archived,List<Step> steps){
        public String label(){return switch(kind){case "TASK"->"Zadanie";case "GOAL"->"Cel";default->"Notatka";};}
        public long doneSteps(){return steps.stream().filter(Step::done).count();}
        public boolean overdue(){return !kind.equals("NOTE")&&!completed&&!archived&&due!=null&&due.isBefore(DashboardService.today());}
    }
    public record Page(List<Entry> entries,long total,int page){public boolean more(){return (page+1L)*50<total;}}
    private final JdbcTemplate db;
    public NoteService(JdbcTemplate db){this.db=db;}
    public Entry draft(){return new Entry(null,0,UUID.randomUUID(),"NOTE","","",null,false,false,false,List.of());}
    private void lock(){db.queryForObject("SELECT id FROM memo_write_guard WHERE id=1 FOR UPDATE",Integer.class);}
    private Entry read(Map<String,Object> r){long id=((Number)r.get("id")).longValue();var steps=db.query("SELECT * FROM memo_step WHERE entry_id=? ORDER BY position",(rs,n)->new Step(rs.getObject("id",UUID.class),rs.getString("text"),rs.getBoolean("done")),id);return new Entry(id,((Number)r.get("version")).longValue(),(UUID)r.get("request_token"),(String)r.get("kind"),(String)r.get("title"),(String)r.get("content"),r.get("due")==null?null:((java.sql.Date)r.get("due")).toLocalDate(),(Boolean)r.get("pinned"),(Boolean)r.get("completed"),(Boolean)r.get("archived"),steps);}
    public Entry get(long id){return db.queryForList("SELECT * FROM memo_entry WHERE id=?",id).stream().findFirst().map(this::read).orElseThrow(()->new IllegalArgumentException("Nie znaleziono wpisu."));}
    public Page list(String state,String query,boolean pinned,int page){
        if(!Set.of("active","done","archived").contains(state)||page<0||page>200000||query==null||query.length()>120)throw new IllegalArgumentException("Sprawdź filtr lub wyszukiwanie.");
        String filter=" WHERE (CASE WHEN ?='archived' THEN archived WHEN ?='done' THEN completed AND NOT archived ELSE NOT completed AND NOT archived END) AND (NOT ? OR pinned) AND (position(lower(?) in lower(title))>0 OR position(lower(?) in lower(content))>0)";
        Object[] args={state,state,pinned,query.strip(),query.strip()};long total=db.queryForObject("SELECT count(*) FROM memo_entry"+filter,Long.class,args);
        var parameters=new ArrayList<>(Arrays.asList(args));parameters.add(page*50);
        var entries=db.queryForList("SELECT * FROM memo_entry"+filter+" ORDER BY pinned DESC,due NULLS LAST,updated_at DESC,id DESC LIMIT 50 OFFSET ?",parameters.toArray()).stream().map(this::read).toList();return new Page(entries,total,page);
    }
    public List<Entry> pinned(){return db.queryForList("SELECT * FROM memo_entry WHERE pinned AND NOT completed AND NOT archived ORDER BY due NULLS LAST,pinned_at DESC,id DESC LIMIT 3").stream().map(this::read).toList();}
    public long pinnedCount(){return db.queryForObject("SELECT count(*) FROM memo_entry WHERE pinned AND NOT completed AND NOT archived",Long.class);}
    @Transactional public long save(Long id,long version,UUID token,String kind,String title,String content,LocalDate due,boolean pinned,List<Step> steps){
        if(token==null||!Set.of("NOTE","TASK","GOAL").contains(Objects.toString(kind,"")))throw new IllegalArgumentException("Wybierz rodzaj wpisu.");
        title=title==null?"":title.strip();content=content==null?"":content;
        if(title.isBlank()||title.length()>160||content.length()>5000)throw new IllegalArgumentException("Wpisz tytuł do 160 znaków i treść do 5000 znaków.");
        if(due!=null)DashboardService.period(due,"day");
        if(steps==null||steps.size()>30||(!kind.equals("GOAL")&&!steps.isEmpty()))throw new IllegalArgumentException("Do celu dodaj maksymalnie 30 kroków. Usuń kroki przed zmianą rodzaju wpisu.");
        var ids=new HashSet<UUID>();for(var step:steps)if(step.id==null||!ids.add(step.id)||step.text==null||step.text.isBlank()||step.text.length()>300)throw new IllegalArgumentException("Kroki muszą mieć różne identyfikatory i tekst od 1 do 300 znaków.");
        lock();
        if(id==null){var existing=db.queryForList("SELECT id FROM memo_entry WHERE request_token=?",Long.class,token);if(!existing.isEmpty())return existing.get(0);id=db.queryForObject("INSERT INTO memo_entry(request_token,kind,title,content,due,pinned,pinned_at) VALUES (?,?,?,?,?,?,CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END) RETURNING id",Long.class,token,kind,title,content,due,pinned,pinned);}
        else changed(db.update("UPDATE memo_entry SET kind=?,title=?,content=?,due=?,pinned_at=CASE WHEN ? AND NOT pinned THEN CURRENT_TIMESTAMP ELSE pinned_at END,pinned=?,completed=CASE WHEN ?='NOTE' THEN FALSE ELSE completed END,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",kind,title,content,due,pinned,pinned,kind,id,version));
        db.update("DELETE FROM memo_step WHERE entry_id=?",id);for(int i=0;i<steps.size();i++){var step=steps.get(i);db.update("INSERT INTO memo_step(entry_id,id,position,text,done) VALUES (?,?,?,?,?)",id,step.id,i,step.text.strip(),step.done);}return id;
    }
    @Transactional public void action(long id,long version,String action,UUID step,boolean checked){
        if(!Set.of("pin","unpin","done","reopen","archive","restore","step").contains(Objects.toString(action,"")))throw new IllegalArgumentException("Nieznana czynność.");
        lock();var entry=get(id);if(entry.version!=version)throw conflict();
        if(action.equals("step")){
            if(entry.archived||!entry.kind.equals("GOAL")||step==null)throw new IllegalArgumentException("Wybierz krok aktywnego celu.");
            changed(db.update("UPDATE memo_step SET done=? WHERE entry_id=? AND id=?",checked,id,step));
        }else{
            if((action.equals("done")||action.equals("reopen"))&&(entry.kind.equals("NOTE")||entry.archived))throw new IllegalArgumentException("Notatkę możesz zarchiwizować; ukończenie dotyczy zadań i celów.");
            String set=switch(action){case "pin"->"pinned=TRUE,pinned_at=CURRENT_TIMESTAMP";case "unpin"->"pinned=FALSE";case "done"->"completed=TRUE";case "reopen"->"completed=FALSE";case "archive"->"archived=TRUE";case "restore"->"archived=FALSE";default->throw new IllegalArgumentException("Nieznana czynność.");};
            db.update("UPDATE memo_entry SET "+set+" WHERE id=?",id);
        }
        changed(db.update("UPDATE memo_entry SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",id,version));
    }
    private static void changed(int count){if(count!=1)throw conflict();}
    private static IllegalArgumentException conflict(){return new IllegalArgumentException("Wpis zmienił się w innym oknie. Twoje pola są zachowane. Sprawdź aktualny wpis przed ponownym zapisem.");}
}
