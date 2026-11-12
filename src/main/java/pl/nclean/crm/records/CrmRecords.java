package pl.nclean.crm.records;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class CrmRecords {
 private final JdbcTemplate db;
 private final ChemistryService chemistry;
 public CrmRecords(JdbcTemplate db,ChemistryService chemistry){this.db=db;this.chemistry=chemistry;}
 public List<ChemistryService.Material> materials(){return chemistry.materials();}
 public Map<Long,ChemistryService.Recipe> recipes(){return chemistry.recipes();}
 public Map<Long,BigDecimal> standardCosts(){return chemistry.standardCosts();}
 public List<Map<String,Object>> customers(String query){
  String q=query==null?"":query.strip();
  return db.queryForList("SELECT * FROM crm_customer WHERE position(lower(?) in lower(name))>0 OR position(? in phone)>0 ORDER BY active DESC,name,id",q,q.replaceAll("[\\s()-]",""));
 }
 public List<Map<String,Object>> categories(){return db.queryForList("SELECT * FROM service_category ORDER BY name,id");}
 public List<Map<String,Object>> services(){return db.queryForList("SELECT s.*,c.name category FROM service_definition s JOIN service_category c ON c.id=s.category_id ORDER BY c.name,s.name,s.id");}
 public List<Map<String,Object>> bookings(LocalDate day,Long customer){
  return db.queryForList("SELECT b.*,COALESCE((SELECT SUM(amount) FROM booking_payment WHERE booking_id=b.id),0) paid,COALESCE((SELECT string_agg(name,', ' ORDER BY position) FROM booking_item WHERE booking_id=b.id),'') service_label FROM booking b WHERE (?::date IS NULL OR day=?) AND (?::bigint IS NULL OR customer_id=?) ORDER BY day DESC,starts,id DESC",day,day,customer,customer);
 }
 public List<Map<String,Object>> monthBookings(YearMonth month){
  return db.queryForList("SELECT b.*, COALESCE((SELECT string_agg(i.name, ', ' ORDER BY i.position) FROM booking_item i WHERE i.booking_id=b.id),'') service_label FROM booking b WHERE day>=? AND day<? ORDER BY day,starts,id",month.atDay(1),month.plusMonths(1).atDay(1));
 }
 public List<Map<String,Object>> monthItems(YearMonth month){return db.queryForList("SELECT i.* FROM booking_item i JOIN booking b ON b.id=i.booking_id WHERE b.day>=? AND b.day<? ORDER BY i.booking_id,i.position",month.atDay(1),month.plusMonths(1).atDay(1));}
 public List<Map<String,Object>> chemicals(long booking){return db.queryForList("SELECT c.*,i.name service_name FROM booking_chemical c JOIN booking_item i ON i.booking_id=c.booking_id AND i.position=c.item_position WHERE c.booking_id=? ORDER BY c.item_position,c.chemical_id",booking);}
 public List<Map<String,Object>> monthChemicals(YearMonth month){return db.queryForList("SELECT c.*,i.name service_name FROM booking_chemical c JOIN booking_item i ON i.booking_id=c.booking_id AND i.position=c.item_position JOIN booking b ON b.id=c.booking_id WHERE b.day>=? AND b.day<? ORDER BY c.booking_id,c.item_position,c.chemical_id",month.atDay(1),month.plusMonths(1).atDay(1));}
 public List<Map<String,Object>> actuals(long booking){return db.queryForList("SELECT * FROM booking_actual_chemical WHERE booking_id=? ORDER BY item_position,chemical_id",booking);}
 public List<Map<String,Object>> monthActuals(YearMonth month){return db.queryForList("SELECT a.* FROM booking_actual_chemical a JOIN booking b ON b.id=a.booking_id WHERE b.day>=? AND b.day<? ORDER BY a.booking_id,a.item_position,a.chemical_id",month.atDay(1),month.plusMonths(1).atDay(1));}
 public Map<String,Map<Long,BigDecimal>> actualFields(Map<Long,List<Map<String,Object>>> lines,List<Map<String,Object>> forecasts,List<Map<String,Object>> actuals){
  var result=new HashMap<String,Map<Long,BigDecimal>>();var units=new HashMap<Long,String>();materials().forEach(m->units.put(m.id(),m.unit()));
  for(var entry:lines.entrySet())for(var item:entry.getValue()){
   String key=entry.getKey()+":"+item.get("position");var values=new LinkedHashMap<Long,BigDecimal>();
   var source=((Number)item.get("actual_version")).longValue()==0?forecasts:actuals;
   for(var row:source)if(((Number)row.get("booking_id")).longValue()==entry.getKey()&&row.get("item_position").equals(item.get("position"))){
    long chemical=((Number)row.get("chemical_id")).longValue();if(Objects.equals(units.get(chemical),row.get("unit")))values.put(chemical,(BigDecimal)row.get("quantity"));
   }
   result.put(key,values);
  }
  return result;
 }
 public List<Map<String,Object>> items(long booking){return db.queryForList("SELECT * FROM booking_item WHERE booking_id=? ORDER BY position",booking);}
 static String text(String v,int max,boolean required){
  String s=v==null?"":v.strip();if(s.length()>max||(required&&s.isBlank()))throw new IllegalArgumentException("Uzupełnij wymagane pola i sprawdź długość tekstu.");return s;
 }
 static String phone(String value){String p=text(value,30,true).replaceAll("[\\s()-]","");if(p.matches("[1-9][0-9]{8}"))p="+48"+p;if(!p.matches("\\+[1-9][0-9]{6,14}"))throw new IllegalArgumentException("Podaj poprawny telefon z prefiksem lub polski numer 9-cyfrowy.");return p;}
 static BigDecimal money(String value){try{BigDecimal n=new BigDecimal(value.replace(',','.')).setScale(2,RoundingMode.UNNECESSARY);if(n.signum()<0||n.compareTo(new BigDecimal("9999999999.99"))>0)throw new IllegalArgumentException();return n;}catch(Exception ex){throw new IllegalArgumentException("Podaj cenę od 0 do 9999999999,99, z najwyżej dwoma miejscami po przecinku.");}}
 static void changed(int rows){if(rows!=1)throw new IllegalArgumentException("Rekord zmienił się lub nie istnieje. Odśwież stronę przed ponowną edycją.");}
 @Transactional public long saveCustomer(Long id,long version,String name,String number,String address,String notes,boolean active){
  name=text(name,120,true);number=phone(number);address=text(address,240,false);notes=text(notes,2000,false);
  if(id==null)return db.queryForObject("INSERT INTO crm_customer(name,phone,address,notes) VALUES (?,?,?,?) RETURNING id",Long.class,name,number,address,notes);
  changed(db.update("UPDATE crm_customer SET name=?,phone=?,address=?,notes=?,active=?,version=version+1 WHERE id=? AND version=?",name,number,address,notes,active,id,version));return id;
 }
 @Transactional public void category(Long id,String name){name=text(name,100,true);if(id==null)db.update("INSERT INTO service_category(name) VALUES (?)",name);else changed(db.update("UPDATE service_category SET name=? WHERE id=?",name,id));}
 @Transactional public void service(Long id,long version,long category,String name,String unit,String price,int minutes,boolean active){
  name=text(name,120,true);unit=text(unit,20,true);BigDecimal amount=money(price);
  if(minutes<1||minutes>1440||db.queryForObject("SELECT count(*) FROM service_category WHERE id=?",Integer.class,category)!=1)throw new IllegalArgumentException("Wybierz kategorię i czas od 1 do 1440 minut.");
  if(id==null)db.update("INSERT INTO service_definition(category_id,name,unit,price,minutes) VALUES (?,?,?,?,?)",category,name,unit,amount,minutes);
  else changed(db.update("UPDATE service_definition SET category_id=?,name=?,unit=?,price=?,minutes=?,active=?,version=version+1 WHERE id=? AND version=?",category,name,unit,amount,minutes,active,id,version));
 }
 @Transactional public long book(UUID token,LocalDate day,LocalTime starts,Long customer,String name,String number,String address,Map<Long,BigDecimal> quantities){
  return book(token,day,starts,customer,name,number,address,quantities,Map.of(),Map.of());
 }
 @Transactional public long book(UUID token,LocalDate day,LocalTime starts,Long customer,String name,String number,String address,Map<Long,BigDecimal> quantities,Map<Long,BigDecimal> prices,Map<Long,ChemistryService.Forecast> forecasts){
  return book(token,day,starts,customer,name,number,address,quantities,prices,forecasts,"UNKNOWN","","");
 }
 @Transactional public long book(UUID token,LocalDate day,LocalTime starts,Long customer,String name,String number,String address,Map<Long,BigDecimal> quantities,Map<Long,BigDecimal> prices,Map<Long,ChemistryService.Forecast> forecasts,String source,String referral,String note){
  if(day==null||day.getYear()<2000||day.getYear()>2100||starts==null||token==null)throw new IllegalArgumentException("Wybierz poprawny dzień i godzinę.");
  db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
  var existing=db.queryForList("SELECT id FROM booking WHERE request_token=?",Long.class,token);if(!existing.isEmpty())return existing.get(0);
  address=text(address,240,true);source=pl.nclean.crm.context.ContextService.source(source);referral=source.equals("REFERRAL")?text(referral,200,false):"";note=text(note,2000,false);
  if(quantities==null||quantities.isEmpty()||quantities.size()>30)throw new IllegalArgumentException("Wybierz od 1 do 30 usług.");
  if(prices==null||forecasts==null||!quantities.keySet().containsAll(prices.keySet())||!quantities.keySet().containsAll(forecasts.keySet()))throw new IllegalArgumentException("Cena lub prognoza dotyczy niewybranej usługi.");
  var quotes=new ArrayList<ChemistryService.Quote>();boolean allDefined=true;BigDecimal chemistryTotal=BigDecimal.ZERO;
  var lines=new ArrayList<Map<String,Object>>();BigDecimal total=BigDecimal.ZERO;
  for(var entry:quantities.entrySet()){
   BigDecimal qty=entry.getValue();if(qty==null||qty.signum()<=0||qty.scale()>2||qty.compareTo(new BigDecimal("10000"))>0)throw new IllegalArgumentException("Ilość musi być dodatnia, maksymalnie 10000 i dwa miejsca po przecinku.");
   var found=db.queryForList("SELECT * FROM service_definition WHERE id=? AND active=TRUE",entry.getKey());if(found.isEmpty())throw new IllegalArgumentException("Usługa została wyłączona lub nie istnieje. Odśwież cennik.");
   var line=new LinkedHashMap<>(found.get(0));line.put("suggested_price",line.get("price"));
   if(prices.containsKey(entry.getKey())){if(prices.get(entry.getKey())==null)throw new IllegalArgumentException("Podaj cenę uzgodnioną.");line.put("price",money(prices.get(entry.getKey()).toPlainString()));}
   var quote=chemistry.quote(entry.getKey(),qty,forecasts.get(entry.getKey()));quotes.add(quote);allDefined&=quote.defined();if(quote.cost()!=null)chemistryTotal=chemistryTotal.add(quote.cost());
   BigDecimal amount=((BigDecimal)line.get("price")).multiply(qty).setScale(2,RoundingMode.HALF_UP);line.put("quantity",qty);line.put("amount",amount);lines.add(line);total=total.add(amount);
  }
  if(customer==null){customer=saveCustomer(null,0,name,number,address,"",true);db.update("UPDATE crm_customer SET source=?,referral_note=? WHERE id=?",source,referral,customer);}
  var people=db.queryForList("SELECT * FROM crm_customer WHERE id=? AND active=TRUE",customer);if(people.isEmpty())throw new IllegalArgumentException("Wybierz aktywnego klienta.");
  var person=people.get(0);
  long id=db.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,total) VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long.class,token,customer,day,starts,name==null||name.isBlank()?person.get("name"):text(name,120,true),number==null||number.isBlank()?person.get("phone"):phone(number),address,total);
  db.update("UPDATE booking SET work_note=? WHERE id=?",note,id);
  for(int i=0;i<lines.size();i++){
   var line=lines.get(i);var quote=quotes.get(i);
   db.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount,suggested_price,forecast_defined,chemical_cost) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,i,line.get("id"),line.get("name"),line.get("unit"),line.get("quantity"),line.get("price"),line.get("amount"),line.get("suggested_price"),quote.defined(),quote.cost());
   for(var usage:quote.usages())db.update("INSERT INTO booking_chemical(booking_id,item_position,chemical_id,name,unit,quantity,unit_cost,amount) VALUES (?,?,?,?,?,?,?,?)",id,i,usage.chemicalId(),usage.name(),usage.unit(),usage.quantity(),usage.unitCost(),usage.amount());
  }
  db.update("UPDATE booking SET chemical_cost=? WHERE id=?",allDefined?chemistryTotal:null,id);
  return id;
 }
 @Transactional public void reschedule(long id,long version,LocalDate day,LocalTime starts,String status){
  if(day==null||day.getYear()<2000||day.getYear()>2100||starts==null||status==null||!Set.of("PLANNED","DONE","CANCELLED").contains(status))throw new IllegalArgumentException("Niepoprawny termin lub status.");
  db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
  var before=db.queryForList("SELECT to_jsonb(b)::text snapshot,b.timer_started,b.day,b.work_time_source FROM booking b WHERE id=? FOR UPDATE",id);if(before.isEmpty())throw new IllegalArgumentException("Nieznane zlecenie.");
  if(status.equals("CANCELLED")&&db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM booking_payment WHERE booking_id=?",BigDecimal.class,id).signum()>0)throw new IllegalArgumentException("Najpierw rozlicz wpłatę w szczegółach zlecenia. Anulowanie nie usuwa należności ani historii wpłat.");
  changed(db.update("UPDATE booking SET day=?,starts=?,status=?,work_minutes=CASE WHEN timer_started IS NOT NULL AND ?<>'PLANNED' THEN LEAST(10080,COALESCE(work_minutes,0)+GREATEST(0,CEIL(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP-timer_started))/60))::integer) ELSE work_minutes END,work_time_source=CASE WHEN timer_started IS NOT NULL THEN 'TIMER' ELSE work_time_source END,timer_started=CASE WHEN ?<>'PLANNED' THEN NULL ELSE timer_started END,version=version+1 WHERE id=? AND version=?",day,starts,status,status,status,id,version));
  boolean photoMoved="PHOTO".equals(before.get(0).get("work_time_source"))&&!((java.sql.Date)before.get(0).get("day")).toLocalDate().equals(day);
  boolean stopped=before.get(0).get("timer_started")!=null&&!status.equals("PLANNED");
  if(photoMoved)db.update("UPDATE booking SET work_minutes=NULL,work_started_at=NULL,work_finished_at=NULL,work_time_source='UNKNOWN' WHERE id=?",id);
  if(photoMoved||stopped){db.update("UPDATE booking_item SET work_minutes=NULL WHERE booking_id=?",id);if(stopped&&db.queryForObject("SELECT count(*) FROM booking_item WHERE booking_id=?",Integer.class,id)==1)db.update("UPDATE booking_item SET work_minutes=(SELECT work_minutes FROM booking WHERE id=?) WHERE booking_id=?",id,id);}
  db.update("INSERT INTO booking_audit(booking_id,before_data,after_data,actor) SELECT ?,?::jsonb,to_jsonb(b),'OWNER' FROM booking b WHERE id=?",id,before.get(0).get("snapshot"),id);
 }
}
