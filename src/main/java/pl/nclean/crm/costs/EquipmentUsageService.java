package pl.nclean.crm.costs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.BusinessDates;

/** Frozen service standards, per-job corrections and a read-only projection of performed usage. */
@Service
@Transactional(readOnly=true)
public class EquipmentUsageService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public EquipmentUsageService(JdbcTemplate db,ObjectMapper json) {this.db=db;this.json=json;}
    public record ServiceChoice(long id,long version,String name,String unit) {}
    public record CounterChoice(long id,String name,String kind) {}
    public record Standard(long id,long version,long serviceId,long counterId,String counterName,String unit,
            BigDecimal minutes,boolean active,boolean currentUnit) {}
    public record StandardCommand(Long id,long version,long serviceId,long serviceVersion,String unit,
            long counterId,BigDecimal minutes,boolean active,UUID token) {}
    public record PrepareCommand(long bookingId,long bookingVersion,UUID token) {}
    public record OverrideCommand(long bookingId,long bookingVersion,long version,UUID token,
            boolean noEquipment,Map<Long,BigDecimal> minutes) {}
    public record Usage(long counterId,long assetId,String name,String kind,BigDecimal standardMinutes,
            BigDecimal overrideMinutes,BigDecimal minutes) {}
    public record Job(long id,long bookingVersion,LocalDate day,String status,boolean historical,
            boolean prepared,long version,boolean noEquipment,String source,int missingItems,List<Usage> usages) {}
    public record Estimate(BigDecimal value,LocalDate day,BigDecimal trackedHours,int jobs) {}

    private void lock() {
        db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);
    }
    private static long number(Map<String,Object> row,String key) {return ((Number)row.get(key)).longValue();}
    private static LocalDate day(Object value) {return value==null?null:((java.sql.Date)value).toLocalDate();}
    private String payload(Object command) {
        try {return json.writeValueAsString(command);}
        catch(JsonProcessingException ex){throw new IllegalArgumentException("Nie udało się odczytać formularza.");}
    }
    private Long replay(UUID token,String operation,Object command) {
        if(token==null)throw new IllegalArgumentException("Otwórz nowy formularz.");
        var rows=db.queryForList("SELECT subject_id,operation,payload=?::jsonb same FROM equipment_usage_action WHERE request_token=?",payload(command),token);
        if(rows.isEmpty())return null;
        if(!operation.equals(rows.get(0).get("operation")) || !Boolean.TRUE.equals(rows.get(0).get("same")))
            throw new IllegalArgumentException("Ten formularz zapisano już z innymi danymi.");
        return number(rows.get(0),"subject_id");
    }
    private String snapshot(String table,String field,long id) {
        return db.queryForObject("SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text),'[]')::text FROM "+table+" t WHERE "+field+"=?",String.class,id);
    }
    private void audit(UUID token,String kind,long subject,Object command,String before,String after,String actor) {
        db.update("INSERT INTO equipment_usage_action(request_token,operation,subject_id,payload,before_data,after_data,actor) VALUES (?,?,?,?::jsonb,?::jsonb,?::jsonb,?)",token,kind,subject,payload(command),before,after,actor);
    }
    private static void minutes(BigDecimal value,BigDecimal maximum) {
        if(value==null || value.signum()<0 || value.scale()>2 || value.compareTo(maximum)>0)
            throw new IllegalArgumentException("Podaj nieujemny czas w minutach, do dwóch miejsc po przecinku (maks. "+maximum+").");
    }
    public List<ServiceChoice> services() {
        return db.queryForList("SELECT s.id,s.version,s.name,s.unit FROM service_definition s JOIN service_category c ON c.id=s.category_id WHERE s.removed_at IS NULL AND c.removed_at IS NULL ORDER BY c.name,s.name,s.id")
                .stream().map(r->new ServiceChoice(number(r,"id"),number(r,"version"),(String)r.get("name"),(String)r.get("unit"))).toList();
    }
    public ServiceChoice service(long id) {
        return services().stream().filter(s->s.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Nieznana usługa."));
    }
    public List<CounterChoice> counters(boolean forStandard) {
        return db.queryForList("SELECT c.id,a.name||' — '||c.name name,a.kind FROM equipment_counter c JOIN equipment_item a ON a.id=c.asset_id WHERE c.unit='h' AND a.active AND a.kind<>'CAR'"+
                (forStandard?" AND a.kind='EQUIPMENT' AND c.fuel_target IS NULL":"")+" ORDER BY a.name,c.name,c.id")
                .stream().map(r->new CounterChoice(number(r,"id"),(String)r.get("name"),(String)r.get("kind"))).toList();
    }
    public List<Standard> standards(long service) {
        return db.queryForList("SELECT s.*,a.name||' — '||c.name counter_name,d.unit=s.unit current_unit FROM equipment_usage_standard s JOIN equipment_counter c ON c.id=s.counter_id JOIN equipment_item a ON a.id=c.asset_id JOIN service_definition d ON d.id=s.service_id WHERE s.service_id=? ORDER BY s.id",service)
                .stream().map(r->new Standard(number(r,"id"),number(r,"version"),number(r,"service_id"),number(r,"counter_id"),(String)r.get("counter_name"),(String)r.get("unit"),(BigDecimal)r.get("minutes_per_unit"),(Boolean)r.get("active"),(Boolean)r.get("current_unit"))).toList();
    }
    private Map<String,Object> counter(long id) {
        return db.queryForList("SELECT c.*,a.active asset_active,a.kind FROM equipment_counter c JOIN equipment_item a ON a.id=c.asset_id WHERE c.id=?",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznany licznik."));
    }
    @Transactional public long saveStandard(StandardCommand c,String actor) {
        minutes(c.minutes(),new BigDecimal("10080"));
        lock();var replay=replay(c.token(),"STANDARD",c);if(replay!=null)return replay;
        var service=db.queryForList("SELECT s.*,c.removed_at category_removed FROM service_definition s JOIN service_category c ON c.id=s.category_id WHERE s.id=?",c.serviceId()).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznana usługa."));
        if(number(service,"version")!=c.serviceVersion() || !Objects.equals(service.get("unit"),c.unit()))throw new IllegalArgumentException("Usługa lub jednostka zmieniła się. Odśwież formularz standardu.");
        var counter=counter(c.counterId());
        if(!"h".equals(counter.get("unit")) || !"EQUIPMENT".equals(counter.get("kind")) || counter.get("fuel_target")!=null)
            throw new IllegalArgumentException("Standard dotyczy osobnego licznika godzin sprzętu. Agregat wpisuj niezależnie na zleceniu.");
        if(c.active() && (!Boolean.TRUE.equals(counter.get("asset_active")) || !Boolean.TRUE.equals(service.get("active")) || service.get("removed_at")!=null || service.get("category_removed")!=null))
            throw new IllegalArgumentException("Wybierz aktywną usługę i sprzęt.");
        long id;String before=null;
        if(c.id()==null) {
            if(db.queryForObject("SELECT count(*) FROM equipment_usage_standard WHERE service_id=? AND counter_id=?",Integer.class,c.serviceId(),c.counterId())>0)
                throw new IllegalArgumentException("Ten standard już istnieje. Edytuj go poniżej.");
            id=db.queryForObject("INSERT INTO equipment_usage_standard(service_id,counter_id,unit,minutes_per_unit,active) VALUES (?,?,?,?,?) RETURNING id",Long.class,c.serviceId(),c.counterId(),c.unit(),c.minutes(),c.active());
        } else {
            id=c.id();before=snapshot("equipment_usage_standard","id",id);
            if(db.update("UPDATE equipment_usage_standard SET unit=?,minutes_per_unit=?,active=?,version=version+1 WHERE id=? AND version=? AND service_id=? AND counter_id=?",c.unit(),c.minutes(),c.active(),id,c.version(),c.serviceId(),c.counterId())!=1)
                throw new IllegalArgumentException("Standard zmienił się w innym oknie. Zachowano pola.");
        }
        audit(c.token(),"STANDARD",id,c,before,snapshot("equipment_usage_standard","id",id),actor);return id;
    }
    /** Called inside booking creation; a failure rolls back the entire booking. */
    @Transactional public void snapshotNewJob(long id) {
        lock();snapshotJob(id,"BOOKING","Zlecenie");
    }
    private void snapshotJob(long id,String source,String actor) {
        if(db.update("INSERT INTO booking_equipment_state(booking_id,prepared_source,prepared_by) VALUES (?,?,?) ON CONFLICT DO NOTHING",id,source,actor)==0)return;
        db.update("""
            INSERT INTO booking_equipment_snapshot(booking_id,item_position,counter_id,standard_id,standard_version,unit,quantity,minutes_per_unit)
            SELECT i.booking_id,i.position,s.counter_id,s.id,s.version,i.unit,i.quantity,s.minutes_per_unit
            FROM booking_item i JOIN equipment_usage_standard s ON s.service_id=i.service_id AND s.unit=i.unit AND s.active
            JOIN equipment_counter c ON c.id=s.counter_id JOIN equipment_item a ON a.id=c.asset_id
            WHERE i.booking_id=? AND a.active AND a.kind='EQUIPMENT' AND c.unit='h' AND c.fuel_target IS NULL
            """,id);
    }
    private Map<String,Object> booking(long id) {
        return db.queryForList("SELECT id,version,day,status,historical FROM booking WHERE id=? AND removed_at IS NULL",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Nieznane zlecenie."));
    }
    private void bookingVersion(long id,long expected) {
        if(number(booking(id),"version")!=expected)throw new IllegalArgumentException("Zlecenie zmieniło się w innym oknie. Otwórz aktualny formularz.");
    }
    @Transactional public long prepare(PrepareCommand c,String actor) {
        lock();var replay=replay(c.token(),"PREPARE",c);if(replay!=null)return replay;bookingVersion(c.bookingId(),c.bookingVersion());
        if(db.queryForObject("SELECT count(*) FROM booking_equipment_state WHERE booking_id=?",Integer.class,c.bookingId())>0)
            throw new IllegalArgumentException("Szacunek był już przygotowany. Użyj korekty, aby zachować zapisane standardy.");
        snapshotJob(c.bookingId(),"EXPLICIT",actor);
        audit(c.token(),"PREPARE",c.bookingId(),c,null,snapshot("booking_equipment_snapshot","booking_id",c.bookingId()),actor);return c.bookingId();
    }
    @Transactional public long override(OverrideCommand c,String actor) {
        if(c.minutes()==null || c.minutes().size()>200)throw new IllegalArgumentException("Sprawdź listę liczników.");
        c.minutes().values().forEach(v->minutes(v,new BigDecimal("100800000")));
        if(c.noEquipment() && !c.minutes().isEmpty())throw new IllegalArgumentException("Wybierz Nie użyto sprzętu albo podaj czasy.");
        lock();var replay=replay(c.token(),"OVERRIDE",c);if(replay!=null)return replay;bookingVersion(c.bookingId(),c.bookingVersion());
        var state=db.queryForList("SELECT * FROM booking_equipment_state WHERE booking_id=?",c.bookingId()).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Najpierw przygotuj szacunek ze standardów."));
        if(number(state,"version")!=c.version())throw new IllegalArgumentException("Korekta zmieniła się w innym oknie. Zachowano pola.");
        for(long id:c.minutes().keySet()) {
            var counter=counter(id);
            if(!"h".equals(counter.get("unit")) || "CAR".equals(counter.get("kind")))throw new IllegalArgumentException("Czas dotyczy licznika godzin sprzętu lub agregatu.");
            if(!Boolean.TRUE.equals(counter.get("asset_active")) && db.queryForObject("SELECT count(*) FROM equipment_job_usage WHERE booking_id=? AND counter_id=?",Integer.class,c.bookingId(),id)==0)
                throw new IllegalArgumentException("Nie dodawaj nowego użycia archiwalnego sprzętu.");
        }
        String before=jobSnapshot(c.bookingId());
        db.update("UPDATE booking_equipment_override SET active=FALSE WHERE booking_id=?",c.bookingId());
        for(var row:c.minutes().entrySet())db.update("INSERT INTO booking_equipment_override(booking_id,counter_id,minutes) VALUES (?,?,?) ON CONFLICT(booking_id,counter_id) DO UPDATE SET minutes=EXCLUDED.minutes,active=TRUE",c.bookingId(),row.getKey(),row.getValue());
        db.update("UPDATE booking_equipment_state SET no_equipment=?,version=version+1 WHERE booking_id=?",c.noEquipment(),c.bookingId());
        audit(c.token(),"OVERRIDE",c.bookingId(),c,before,jobSnapshot(c.bookingId()),actor);return c.bookingId();
    }
    private String jobSnapshot(long id) {
        return db.queryForObject("SELECT jsonb_build_object('state',(SELECT to_jsonb(s) FROM booking_equipment_state s WHERE booking_id=?),'overrides',(SELECT COALESCE(jsonb_agg(to_jsonb(o) ORDER BY counter_id),'[]') FROM booking_equipment_override o WHERE booking_id=?))::text",String.class,id,id);
    }
    public Job job(long id) {
        var b=booking(id);var states=db.queryForList("SELECT * FROM booking_equipment_state WHERE booking_id=?",id);
        boolean prepared=!states.isEmpty(),zero=prepared && Boolean.TRUE.equals(states.get(0).get("no_equipment"));
        int missing=zero?0:db.queryForObject("SELECT count(*) FROM booking_item i WHERE booking_id=? AND NOT EXISTS (SELECT 1 FROM booking_equipment_snapshot s WHERE s.booking_id=i.booking_id AND s.item_position=i.position)",Integer.class,id);
        var usages=db.queryForList("SELECT u.*,a.name||' — '||c.name name,a.kind FROM equipment_job_usage u JOIN equipment_counter c ON c.id=u.counter_id JOIN equipment_item a ON a.id=c.asset_id WHERE u.booking_id=? ORDER BY u.counter_id",id)
                .stream().map(r->new Usage(number(r,"counter_id"),number(r,"asset_id"),(String)r.get("name"),(String)r.get("kind"),(BigDecimal)r.get("standard_minutes"),(BigDecimal)r.get("override_minutes"),(BigDecimal)r.get("minutes"))).toList();
        return new Job(id,number(b,"version"),day(b.get("day")),(String)b.get("status"),(Boolean)b.get("historical"),prepared,prepared?number(states.get(0),"version"):-1,zero,prepared?(String)states.get(0).get("prepared_source"):"",missing,usages);
    }
    /** Absolute anchors are never added to older jobs. Only following calendar days contribute. */
    public Estimate estimate(long counter,LocalDate at,BigDecimal actual,LocalDate actualOn,BigDecimal manual,LocalDate manualOn) {
        boolean manualAnchor=manual!=null && (actual==null || manualOn.isAfter(actualOn));
        var anchorDay=manualAnchor?manualOn:actualOn;var anchor=manualAnchor?manual:actual;
        var total=db.queryForMap("SELECT COALESCE(SUM(minutes),0) minutes,count(*) FILTER(WHERE minutes>0) jobs FROM equipment_job_usage WHERE counter_id=? AND status='DONE' AND removed_at IS NULL AND day<=?",counter,at);
        var later=db.queryForMap("SELECT COALESCE(SUM(minutes),0) minutes,max(day) FILTER(WHERE minutes>0) AS usage_day FROM equipment_job_usage WHERE counter_id=? AND status='DONE' AND removed_at IS NULL AND day<=? AND (?::date IS NULL OR day>?)",counter,at,anchorDay,anchorDay);
        var addition=(BigDecimal)later.get("minutes");
        var value=addition.signum()>0?(anchor==null?BigDecimal.ZERO:anchor).add(hours(addition)):manual;
        var estimateDay=addition.signum()>0?day(later.get("usage_day")):manualOn;
        return new Estimate(value,estimateDay,hours((BigDecimal)total.get("minutes")),((Number)total.get("jobs")).intValue());
    }
    public static BigDecimal hours(BigDecimal minutes) {return minutes.divide(new BigDecimal("60"),2,RoundingMode.HALF_UP);}
    public List<Map<String,Object>> history(long asset,String filter,LocalDate boundary) {
        if(!List.of("all","historical","current").contains(filter))throw new IllegalArgumentException("Sprawdź zakres historii.");
        return db.queryForList("SELECT u.*,c.name counter_name FROM equipment_job_usage u JOIN equipment_counter c ON c.id=u.counter_id WHERE u.asset_id=? AND u.removed_at IS NULL ORDER BY u.day DESC,u.booking_id DESC,u.counter_id",asset).stream()
                .filter(r->boundary==null || filter.equals("all") || day(r.get("day")).isBefore(boundary)==filter.equals("historical")).toList();
    }
}
