package pl.nclean.crm.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalogue standards and immutable booking forecasts; no stock or actual consumption. */
@Service
@Transactional(readOnly=true)
public class ChemistryService {
    private final JdbcTemplate db;
    public ChemistryService(JdbcTemplate db) { this.db=db; }
    public record Material(long id,long version,String name,String packageUnit,BigDecimal packageQuantity,
                           BigDecimal packagePrice,String unit,BigDecimal unitCost) {}
    public record Recipe(boolean defined,Map<Long,BigDecimal> quantities) {}
    public record Forecast(boolean defined,Map<Long,BigDecimal> quantities) {}
    public record Usage(long chemicalId,String name,String unit,BigDecimal quantity,BigDecimal unitCost,BigDecimal amount) {}
    public record Quote(boolean defined,List<Usage> usages,BigDecimal cost) {}

    static String baseUnit(String unit) {
        if(unit==null)throw new IllegalArgumentException("Wybierz jednostkę środka.");
        return switch(unit) { case "l","ml"->"ml";case "kg","g"->"g";case "szt"->"szt";default->throw new IllegalArgumentException("Wybierz jednostkę ml, l, g, kg lub szt."); };
    }
    static BigDecimal factor(String unit) { baseUnit(unit);return Set.of("l","kg").contains(unit)?new BigDecimal("1000"):BigDecimal.ONE; }
    static BigDecimal quantity(String value,boolean positive) {
        try {
            var n=new BigDecimal(value.replace(',','.')).setScale(3,RoundingMode.UNNECESSARY);
            if(n.signum()<0||(positive&&n.signum()==0)||n.compareTo(new BigDecimal("99999999999.999"))>0)throw new IllegalArgumentException();
            return n;
        } catch(Exception ex) { throw new IllegalArgumentException("Podaj ilość do trzech miejsc po przecinku, nie mniejszą od "+(positive?"0,001.":"zera.")); }
    }
    private Material view(Map<String,Object> row) {
        var unit=(String)row.get("package_unit");var quantity=(BigDecimal)row.get("package_quantity");var price=(BigDecimal)row.get("package_price");
        return new Material(((Number)row.get("id")).longValue(),((Number)row.get("version")).longValue(),(String)row.get("name"),unit,quantity,price,baseUnit(unit),price.divide(quantity.multiply(factor(unit)),16,RoundingMode.HALF_UP));
    }
    public List<Material> materials() { return db.queryForList("SELECT * FROM chemical ORDER BY name,id").stream().map(this::view).toList(); }
    public Map<Long,Recipe> recipes() {
        var result=new LinkedHashMap<Long,Recipe>();
        for(var row:db.queryForList("SELECT id,chemistry_defined FROM service_definition"))result.put(((Number)row.get("id")).longValue(),new Recipe((Boolean)row.get("chemistry_defined"),new LinkedHashMap<>()));
        for(var row:db.queryForList("SELECT * FROM service_chemical ORDER BY service_id,chemical_id"))result.get(((Number)row.get("service_id")).longValue()).quantities().put(((Number)row.get("chemical_id")).longValue(),(BigDecimal)row.get("quantity"));
        return result;
    }
    @Transactional
    public long saveMaterial(Long id,long version,String name,String packageUnit,String quantity,String price) {
        name=CrmRecords.text(name,120,true);baseUnit(packageUnit);var amount=quantity(quantity,true);var money=CrmRecords.money(price);
        if(id==null)return db.queryForObject("INSERT INTO chemical(name,package_unit,package_quantity,package_price) VALUES (?,?,?,?) RETURNING id",Long.class,name,packageUnit,amount,money);
        var rows=db.queryForList("SELECT * FROM chemical WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty())throw new IllegalArgumentException("Nieznany środek chemiczny.");
        if(!baseUnit((String)rows.get(0).get("package_unit")).equals(baseUnit(packageUnit)) && db.queryForObject("SELECT (SELECT count(*) FROM service_chemical WHERE chemical_id=?)+(SELECT count(*) FROM booking_actual_chemical WHERE chemical_id=?)",Integer.class,id,id)>0)
            throw new IllegalArgumentException("Ten środek ma przypisany standard lub zapisane rzeczywiste zużycie. Dla innego rodzaju jednostki dodaj osobny środek.");
        CrmRecords.changed(db.update("UPDATE chemical SET name=?,package_unit=?,package_quantity=?,package_price=?,version=version+1 WHERE id=? AND version=?",name,packageUnit,amount,money,id,version));return id;
    }
    @Transactional
    public void saveRecipe(long service,long version,boolean defined,Map<Long,BigDecimal> quantities) {
        if(quantities.size()>100)throw new IllegalArgumentException("Standard może zawierać do 100 środków.");
        var known=new HashSet<>(materials().stream().map(Material::id).toList());
        for(var entry:quantities.entrySet())if(!known.contains(entry.getKey())||entry.getValue()==null)throw new IllegalArgumentException("Wybierz istniejący środek.");
        var normalized=new LinkedHashMap<Long,BigDecimal>();
        quantities.forEach((id,q)->normalized.put(id,quantity(q.toPlainString(),false)));
        CrmRecords.changed(db.update("UPDATE service_definition SET chemistry_defined=?,version=version+1 WHERE id=? AND version=?",defined||!normalized.isEmpty(),service,version));
        db.update("DELETE FROM service_chemical WHERE service_id=?",service);
        for(var entry:normalized.entrySet())db.update("INSERT INTO service_chemical(service_id,chemical_id,quantity) VALUES (?,?,?)",service,entry.getKey(),entry.getValue());
    }
    public Quote quote(long service,BigDecimal serviceQuantity,Forecast supplied) {
        Recipe standard=recipes().get(service);
        if(standard==null)throw new IllegalArgumentException("Nieznana usługa.");
        boolean defined=supplied==null?standard.defined():supplied.defined();
        var quantities=new LinkedHashMap<Long,BigDecimal>();
        if(supplied==null)standard.quantities().forEach((id,q)->quantities.put(id,q.multiply(serviceQuantity).setScale(3,RoundingMode.HALF_UP)));
        else if(supplied.quantities()!=null)quantities.putAll(supplied.quantities());
        if(!defined){if(!quantities.isEmpty())throw new IllegalArgumentException("Uzupełnij prognozę albo usuń jej ilości.");return new Quote(false,List.of(),null);}
        if(quantities.size()>100)throw new IllegalArgumentException("Prognoza pozycji może zawierać do 100 środków.");
        var known=new HashMap<Long,Material>();materials().forEach(m->known.put(m.id(),m));
        var usages=new ArrayList<Usage>();var total=BigDecimal.ZERO.setScale(2);
        for(var entry:quantities.entrySet()) {
            var material=known.get(entry.getKey());if(material==null)throw new IllegalArgumentException("Nieznany środek w prognozie.");
            var q=quantity(entry.getValue().toPlainString(),false);
            var amount=material.unitCost().multiply(q).setScale(2,RoundingMode.HALF_UP);
            if(amount.compareTo(new BigDecimal("999999999999.99"))>0)throw new IllegalArgumentException("Koszt prognozy jest zbyt duży.");
            total=total.add(amount);usages.add(new Usage(material.id(),material.name(),material.unit(),q,material.unitCost(),amount));
        }
        return new Quote(true,List.copyOf(usages),total);
    }
    public Map<Long,BigDecimal> standardCosts() {
        var result=new LinkedHashMap<Long,BigDecimal>();
        for(var entry:recipes().entrySet())if(entry.getValue().defined())result.put(entry.getKey(),quote(entry.getKey(),BigDecimal.ONE,null).cost());
        return result;
    }
}
