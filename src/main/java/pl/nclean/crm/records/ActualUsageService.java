package pl.nclean.crm.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suggestions use only explicitly recorded quantities from completed bookings. */
@Service
@Transactional(readOnly=true)
public class ActualUsageService {
    public static final int MIN_SAMPLES=5;
    private final JdbcTemplate db;
    private final ChemistryService chemistry;
    public ActualUsageService(JdbcTemplate db,ChemistryService chemistry){this.db=db;this.chemistry=chemistry;}
    public record Suggestion(long serviceId,long chemicalId,String name,String unit,BigDecimal quantity,
                             int samples,BigDecimal minimum,BigDecimal maximum,LocalDate lastDate) {}

    @Transactional
    public void record(long booking,int position,long version,Map<Long,BigDecimal> quantities){
        var items=db.queryForList("SELECT i.*,b.status FROM booking_item i JOIN booking b ON b.id=i.booking_id WHERE i.booking_id=? AND i.position=? FOR UPDATE OF b,i",booking,position);
        if(items.isEmpty())throw new IllegalArgumentException("Nieznana pozycja zlecenia.");
        if(!"DONE".equals(items.get(0).get("status")))throw new IllegalArgumentException("Rzeczywiste zużycie zapisz po oznaczeniu zlecenia jako wykonane.");
        if(quantities==null||quantities.size()>100)throw new IllegalArgumentException("Podaj do 100 pozycji rzeczywistego zużycia.");
        var materials=new HashMap<Long,ChemistryService.Material>();chemistry.materials().forEach(m->materials.put(m.id(),m));
        var normalized=new LinkedHashMap<Long,BigDecimal>();
        for(var entry:quantities.entrySet()){
            if(!materials.containsKey(entry.getKey())||entry.getValue()==null)throw new IllegalArgumentException("Wybierz istniejący środek.");
            normalized.put(entry.getKey(),ChemistryService.quantity(entry.getValue().toPlainString(),false));
        }
        CrmRecords.changed(db.update("UPDATE booking_item SET actual_defined=?,actual_version=actual_version+1 WHERE booking_id=? AND position=? AND actual_version=?",!normalized.isEmpty(),booking,position,version));
        db.update("DELETE FROM booking_actual_chemical WHERE booking_id=? AND item_position=?",booking,position);
        for(var entry:normalized.entrySet()){
            var material=materials.get(entry.getKey());
            db.update("INSERT INTO booking_actual_chemical(booking_id,item_position,chemical_id,name,unit,quantity) VALUES (?,?,?,?,?,?)",booking,position,material.id(),material.name(),material.unit(),entry.getValue());
        }
    }

    public Map<Long,List<Suggestion>> suggestions(){
        var materials=new HashMap<Long,ChemistryService.Material>();chemistry.materials().forEach(m->materials.put(m.id(),m));
        // One observation per booking/service/chemical, including explicit zeros.
        var rows=db.queryForList("SELECT b.id booking_id,i.service_id,a.chemical_id,a.unit,SUM(a.quantity) amount,SUM(i.quantity) service_quantity,b.day FROM booking_actual_chemical a JOIN booking_item i ON i.booking_id=a.booking_id AND i.position=a.item_position JOIN booking b ON b.id=i.booking_id JOIN service_definition s ON s.id=i.service_id WHERE b.status='DONE' AND i.actual_defined=TRUE AND i.unit=s.unit GROUP BY b.id,i.service_id,a.chemical_id,a.unit,b.day");
        record Key(long service,long chemical){}
        record Sample(BigDecimal quantity,LocalDate day){}
        var groups=new LinkedHashMap<Key,List<Sample>>();
        for(var row:rows){
            long chemical=((Number)row.get("chemical_id")).longValue();var material=materials.get(chemical);
            if(material==null||!material.unit().equals(row.get("unit")))continue;
            var normalized=((BigDecimal)row.get("amount")).divide((BigDecimal)row.get("service_quantity"),12,RoundingMode.HALF_UP);
            var key=new Key(((Number)row.get("service_id")).longValue(),chemical);
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(new Sample(normalized,((java.sql.Date)row.get("day")).toLocalDate()));
        }
        var result=new LinkedHashMap<Long,List<Suggestion>>();
        for(var entry:groups.entrySet()){
            var samples=entry.getValue();if(samples.size()<MIN_SAMPLES)continue;
            var values=samples.stream().map(Sample::quantity).sorted().toList();int size=values.size();
            var median=size%2==1?values.get(size/2):values.get(size/2-1).add(values.get(size/2)).divide(new BigDecimal("2"));
            var key=entry.getKey();var material=materials.get(key.chemical());
            var suggestion=new Suggestion(key.service(),key.chemical(),material.name(),material.unit(),median.setScale(3,RoundingMode.HALF_UP),size,values.get(0).setScale(3,RoundingMode.HALF_UP),values.get(size-1).setScale(3,RoundingMode.HALF_UP),samples.stream().map(Sample::day).max(LocalDate::compareTo).orElseThrow());
            result.computeIfAbsent(key.service(),k->new ArrayList<>()).add(suggestion);
        }
        result.values().forEach(list->list.sort(Comparator.comparing(Suggestion::name).thenComparingLong(Suggestion::chemicalId)));
        return result;
    }

    @Transactional
    public void apply(long service,long chemical,long version){
        var suggestion=suggestions().getOrDefault(service,List.of()).stream().filter(s->s.chemicalId()==chemical).findFirst().orElseThrow(()->new IllegalArgumentException("Za mało potwierdzonych danych do propozycji."));
        var recipe=chemistry.recipes().get(service);if(recipe==null)throw new IllegalArgumentException("Nieznana usługa.");
        var quantities=new LinkedHashMap<>(recipe.quantities());quantities.put(chemical,suggestion.quantity());
        chemistry.saveRecipe(service,version,true,quantities);
    }
}
