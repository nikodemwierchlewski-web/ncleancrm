package pl.nclean.crm.context;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.fuel.ReceiptService;

@Service
@Transactional(readOnly=true)
public class ContextService {
    private final JdbcTemplate db;private final ReceiptService images;
    public ContextService(JdbcTemplate db,ReceiptService images){this.db=db;this.images=images;}
    public static final Map<String,String> SOURCES=labels();
    private static Map<String,String> labels(){var labels=new LinkedHashMap<String,String>();labels.put("UNKNOWN","Nieustalone");labels.put("GOOGLE","Google / Mapy");labels.put("FACEBOOK","Facebook");labels.put("REFERRAL","Polecenie");labels.put("OLX","OLX");labels.put("OTHER","Inne");return Collections.unmodifiableMap(labels);}
    public static String source(String value){var s=value==null||value.isBlank()?"UNKNOWN":value;if(!SOURCES.containsKey(s))throw new IllegalArgumentException("Wybierz źródło klienta.");return s;}
    public static Optional<LocalDateTime> filenameTime(String filename){
        if(filename==null)return Optional.empty();
        var m=Pattern.compile("^(?:IMG_|PXL_)?(\\d{8})[_-](\\d{6})(?:[._-].*)?\\.[A-Za-z0-9]+$",Pattern.CASE_INSENSITIVE).matcher(filename);
        if(m.matches())try{return Optional.of(LocalDateTime.parse(m.group(1)+m.group(2),DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT)));}catch(DateTimeException ignored){}
        m=Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})[_ ](\\d{2})[-.](\\d{2})[-.](\\d{2})\\.[A-Za-z0-9]+$").matcher(filename);
        if(m.matches())try{return Optional.of(LocalDateTime.parse(m.group(1)+"T"+m.group(2)+":"+m.group(3)+":"+m.group(4)));}catch(DateTimeException ignored){}
        return Optional.empty();
    }
    public List<Map<String,Object>> photos(long booking){return db.queryForList("SELECT id,stage,original_name,captured_at FROM booking_photo WHERE booking_id=? ORDER BY stage DESC,captured_at NULLS LAST,created_at,id",booking);}
    public record PhotoTime(LocalDateTime start,LocalDateTime end,Long minutes,String message){}
    public PhotoTime time(long booking){var rows=photos(booking);LocalDateTime start=null,end=null;boolean unknown=false;
        for(var row:rows){if(row.get("captured_at")==null){unknown=true;continue;}var t=((java.sql.Timestamp)row.get("captured_at")).toLocalDateTime();if("BEFORE".equals(row.get("stage"))&&(start==null||t.isBefore(start)))start=t;if("AFTER".equals(row.get("stage"))&&(end==null||t.isAfter(end)))end=t;}
        if(unknown)return new PhotoTime(start,end,null,"Nie rozpoznano czasu części zdjęć. Ustaw czas ręcznie w wykonaniu.");
        if(start==null||end==null)return new PhotoTime(start,end,null,"Dodaj zdjęcia Przed i Po z datą i godziną w nazwie.");
        var date=db.queryForObject("SELECT day FROM booking WHERE id=?",java.sql.Date.class,booking).toLocalDate();long seconds=seconds(start,end);
        if(!start.toLocalDate().equals(date)||seconds<0||seconds>86400)return new PhotoTime(start,end,null,"Daty zdjęć są sprzeczne z wizytą lub kolejnością. Ustaw czas ręcznie.");
        return new PhotoTime(start,end,(seconds+59)/60,"Czas ze zdjęć — możesz poprawić go ręcznie.");
    }
    private static long seconds(LocalDateTime start,LocalDateTime end){var zone=ZoneId.of("Europe/Warsaw");if(zone.getRules().getValidOffsets(start).size()!=1||zone.getRules().getValidOffsets(end).size()!=1)return -1;return Duration.between(start.atZone(zone).toInstant(),end.atZone(zone).toInstant()).getSeconds();}
    @Transactional public long manualTime(long booking,long version,LocalDateTime start,LocalDateTime end,String actor){var b=lock(booking);if(((Number)b.get("version")).longValue()!=version)throw new IllegalArgumentException("Zlecenie zmieniło się. Odśwież formularz.");long seconds=seconds(start,end);if(seconds<0||seconds>604800||!start.toLocalDate().equals(((java.sql.Date)b.get("day")).toLocalDate())||b.get("timer_started")!=null)throw new IllegalArgumentException("Sprawdź kolejność/datę godzin i zatrzymaj aktywny timer. Niejednoznaczne godziny zmiany czasu wymagają minut w wykonaniu.");int minutes=(int)((seconds+59)/60);String before=db.queryForObject("SELECT to_jsonb(b)::text FROM booking b WHERE id=?",String.class,booking);db.update("UPDATE booking SET work_started_at=?,work_finished_at=?,work_minutes=?,work_time_source='MANUAL',version=version+1 WHERE id=?",start,end,minutes,booking);db.update("UPDATE booking_item SET work_minutes=NULL WHERE booking_id=?",booking);if(db.queryForObject("SELECT count(*) FROM booking_item WHERE booking_id=?",Integer.class,booking)==1)db.update("UPDATE booking_item SET work_minutes=? WHERE booking_id=?",minutes,booking);db.update("INSERT INTO booking_audit(booking_id,before_data,after_data,actor) SELECT ?,?::jsonb,to_jsonb(b),? FROM booking b WHERE id=?",booking,before,actor,booking);return version+1;}
    private Map<String,Object> lock(long booking){db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);return db.queryForList("SELECT * FROM booking WHERE id=? FOR UPDATE",booking).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznane zlecenie."));}
    private void recalculate(long booking,String actor){
        var b=lock(booking);if(!Set.of("UNKNOWN","PHOTO").contains((String)b.get("work_time_source"))||b.get("timer_started")!=null){db.update("UPDATE booking SET version=version+1 WHERE id=?",booking);return;}
        var t=time(booking);String before=db.queryForObject("SELECT to_jsonb(b)::text FROM booking b WHERE id=?",String.class,booking);
        db.update("UPDATE booking SET work_minutes=?,work_started_at=?,work_finished_at=?,work_time_source=?,version=version+1 WHERE id=?",t.minutes(),t.minutes()==null?null:t.start(),t.minutes()==null?null:t.end(),t.minutes()==null?"UNKNOWN":"PHOTO",booking);
        db.update("UPDATE booking_item SET work_minutes=NULL WHERE booking_id=?",booking);
        if(t.minutes()!=null&&db.queryForObject("SELECT count(*) FROM booking_item WHERE booking_id=?",Integer.class,booking)==1)db.update("UPDATE booking_item SET work_minutes=? WHERE booking_id=?",t.minutes(),booking);
        db.update("INSERT INTO booking_audit(booking_id,before_data,after_data,actor) SELECT ?,?::jsonb,to_jsonb(b),? FROM booking b WHERE id=?",booking,before,actor,booking);
    }
    @Transactional public UUID upload(long booking,String stage,UUID token,MultipartFile file,String actor,long version){
        if(!Set.of("BEFORE","AFTER").contains(stage))throw new IllegalArgumentException("Wybierz Przed lub Po.");byte[] bytes=images.normalize(file);var locked=lock(booking);
        var replay=db.queryForList("SELECT id,booking_id,stage FROM booking_photo WHERE request_token=?",token);if(!replay.isEmpty()){if(((Number)replay.get(0).get("booking_id")).longValue()!=booking||!stage.equals(replay.get(0).get("stage")))throw new IllegalArgumentException("Zdjęcie zostało już zapisane w innym etapie lub wizycie. Odśwież widok.");return (UUID)replay.get(0).get("id");}
        if(((Number)locked.get("version")).longValue()!=version)throw new IllegalArgumentException("Zlecenie zmieniło się. Zachowaj formularz i odśwież przed dodaniem zdjęć.");
        if(db.queryForObject("SELECT count(*) FROM booking_photo WHERE booking_id=?",Integer.class,booking)>=40)throw new IllegalArgumentException("Maksymalnie 40 zdjęć na zlecenie.");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("zdjęcie").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.length()>180)throw new IllegalArgumentException("Nazwa zdjęcia może mieć do 180 znaków.");
        UUID id=UUID.randomUUID();db.update("INSERT INTO booking_photo(id,request_token,booking_id,stage,original_name,captured_at,image) VALUES (?,?,?,?,?,?,?)",id,token,booking,stage,name,filenameTime(name).orElse(null),bytes);recalculate(booking,actor);return id;
    }
    public long version(long booking){return db.queryForObject("SELECT version FROM booking WHERE id=?",Long.class,booking);}
    public byte[] image(UUID id){return db.query("SELECT image FROM booking_photo WHERE id=?",(r,n)->r.getBytes(1),id).stream().findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));}
    @Transactional public void delete(long booking,UUID id,String actor){lock(booking);if(db.update("DELETE FROM booking_photo WHERE id=? AND booking_id=?",id,booking)!=1)throw new IllegalArgumentException("Zdjęcie nie istnieje.");recalculate(booking,actor);}
    public record Result(String type,long id,String title,String detail){}
    public List<Result> search(String value){String q=value==null?"":value.strip();if(q.length()<2)return List.of();if(q.length()>120)q=q.substring(0,120);String phone=q.replaceAll("[\\s()+-]","");var result=new ArrayList<Result>();
        db.query("SELECT id,name,phone,address FROM crm_customer WHERE position(lower(?) in lower(name||' '||address))>0 OR (?<>'' AND position(? in regexp_replace(phone,'[^0-9]','','g'))>0) ORDER BY name,id LIMIT 15",r->{result.add(new Result("customer",r.getLong("id"),r.getString("name"),r.getString("phone")+" · "+r.getString("address")));},q,phone,phone);
        db.query("SELECT id,day,customer_name,address FROM booking WHERE position(lower(?) in lower(customer_name||' '||address))>0 OR (?<>'' AND position(? in regexp_replace(phone,'[^0-9]','','g'))>0) ORDER BY day DESC,starts LIMIT 20",r->{result.add(new Result("order",r.getLong("id"),r.getString("customer_name"),r.getDate("day")+" · "+r.getString("address")));},q,phone,phone);return result;
    }
    public List<Map<String,Object>> contacts(long customer){return db.queryForList("SELECT * FROM customer_contact WHERE customer_id=? ORDER BY id DESC LIMIT 30",customer);}
    public List<Map<String,Object>> callbacks(){return db.queryForList("SELECT c.*,v.last_day FROM crm_customer c JOIN (SELECT customer_id,max(day) last_day FROM booking WHERE status='DONE' GROUP BY customer_id) v ON v.customer_id=c.id WHERE c.active=TRUE AND v.last_day<=? AND (c.contact_due IS NULL OR c.contact_due<=?) AND NOT EXISTS(SELECT 1 FROM booking b WHERE b.customer_id=c.id AND b.status='PLANNED' AND b.day>=?) ORDER BY COALESCE(c.contact_due,v.last_day),c.name",DashboardService.today().minusMonths(6),DashboardService.today(),DashboardService.today());}
    @Transactional public void contact(long customer,long version,UUID token,LocalDate next,String note){if(next==null||!next.isAfter(DashboardService.today())||next.getYear()>2100||note==null||note.length()>1000)throw new IllegalArgumentException("Podaj przyszłą datę ponownego kontaktu i notatkę do 1000 znaków.");
        db.queryForList("SELECT id FROM crm_customer WHERE id=? FOR UPDATE",customer);var replay=db.queryForList("SELECT customer_id FROM customer_contact WHERE request_token=?",token);if(!replay.isEmpty()){if(((Number)replay.get(0).get("customer_id")).longValue()!=customer)throw new IllegalArgumentException("Token należy do innego klienta.");return;}
        if(db.update("UPDATE crm_customer SET last_contact=?,contact_due=?,version=version+1 WHERE id=? AND version=?",DashboardService.today(),next,customer,version)!=1)throw new IllegalArgumentException("Klient zmienił się w innym oknie. Odśwież przed zapisem.");db.update("INSERT INTO customer_contact(request_token,customer_id,contacted_on,next_on,note) VALUES (?,?,?,?,?)",token,customer,DashboardService.today(),next,note.strip());
    }
}
