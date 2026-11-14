package pl.nclean.crm.removal;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.nclean.crm.costs.CostService;
import pl.nclean.crm.fuel.FuelJournalService;

/** Reversible corrections. Snapshots and foreign-key relations are never deleted. */
@Service
public class RemovalService {
 private static final Map<String,String> TABLES=Map.of("booking","booking","category","service_category","service","service_definition","chemical","chemical","expense","cost_entry","fuel","fuel_purchase","equipment","equipment_item");
 private final JdbcTemplate db;private final CostService costs;private final FuelJournalService fuel;
 public RemovalService(JdbcTemplate db,CostService costs,FuelJournalService fuel){this.db=db;this.costs=costs;this.fuel=fuel;}
 @Transactional public Map<String,Object> change(String kind,long id,long version,boolean restore,String actor){
  String table=TABLES.get(kind);if(table==null||id<1||version<0)throw new IllegalArgumentException("Niepoprawny wpis lub wersja.");
  boolean financial=Set.of("expense","fuel","equipment").contains(kind);
  db.queryForObject("SELECT id FROM "+(financial?"fuel_write_guard":"work_write_guard")+" WHERE id=1 FOR UPDATE",Integer.class);
  var row=db.queryForList("SELECT * FROM "+table+" WHERE id=? FOR UPDATE",id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  boolean removed=financial?(kind.equals("equipment")?!Boolean.TRUE.equals(row.get("active")):Boolean.TRUE.equals(row.get("cancelled"))):row.get("removed_at")!=null;
  long current=((Number)row.get("version")).longValue();
  if(removed==!restore&&(version==current||version+1==current))return result(kind,id,current,removed,row);
  if(version!=current)throw new IllegalStateException("Wpis zmienił się w innym oknie. Odśwież dane przed usunięciem lub przywróceniem.");
  if(kind.equals("booking")&&!restore){
   if(row.get("timer_started")!=null)throw new IllegalArgumentException("Najpierw zatrzymaj pomiar czasu tego zlecenia.");
   if(db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM booking_payment WHERE booking_id=?",BigDecimal.class,id).signum()!=0)throw new IllegalArgumentException("Zlecenie ma wpłatę. Najpierw skoryguj ją w płatnościach, aby nie zgubić rozliczenia.");
  }
  if(kind.equals("chemical")&&!restore&&db.queryForObject("SELECT (SELECT count(*) FROM service_chemical WHERE chemical_id=?)+(SELECT count(*) FROM booking_chemical c JOIN booking b ON b.id=c.booking_id WHERE c.chemical_id=? AND b.removed_at IS NULL AND b.status='PLANNED')",Integer.class,id,id)>0)throw new IllegalArgumentException("Środek jest w standardzie lub prognozie zaplanowanej wizyty. Najpierw zmień te powiązania; historia pozostanie zachowana.");
  if(kind.equals("service")&&restore&&db.queryForObject("SELECT count(*) FROM service_category WHERE id=? AND removed_at IS NULL",Integer.class,row.get("category_id"))==0)throw new IllegalArgumentException("Najpierw przywróć kategorię tej usługi.");
  String before=db.queryForObject("SELECT to_jsonb(t)::text FROM "+table+" t WHERE id=?",String.class,id);
  if(kind.equals("expense")){if(restore)costs.restore(id,version,actor);else costs.cancel(id,version,"Usunięto błędny wpis",actor);}
  else if(kind.equals("fuel")){if(restore)fuel.restorePurchase(id,version,actor);else fuel.cancelPurchase(id,version,"Usunięto błędny wpis",actor);}
  else if(kind.equals("equipment"))db.update("UPDATE equipment_item SET active=?,version=version+1 WHERE id=? AND version=?",restore,id,version);
  else db.update("UPDATE "+table+" SET removed_at="+(restore?"NULL":"CURRENT_TIMESTAMP")+",version=version+1 WHERE id=? AND version=?",id,version);
  db.update("INSERT INTO removal_audit(kind,entity_id,action,before_data,after_data,actor) SELECT ?,?,?,?::jsonb,to_jsonb(t),? FROM "+table+" t WHERE id=?",kind,id,restore?"RESTORE":"REMOVE",before,actor,id);
  return result(kind,id,version+1,!restore,row);
 }
 private Map<String,Object> result(String kind,long id,long version,boolean removed,Map<String,Object> row){var out=new LinkedHashMap<String,Object>();out.put("saved",true);out.put("kind",kind);out.put("id",id);out.put("version",version);out.put("removed",removed);if(kind.equals("booking"))out.put("day",row.get("day").toString());return out;}
}
