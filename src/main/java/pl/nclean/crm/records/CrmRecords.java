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
 public CrmRecords(JdbcTemplate db){this.db=db;}
 public List<Map<String,Object>> customers(String query){
  String q=query==null?"":query.strip();
  return db.queryForList("SELECT * FROM crm_customer WHERE position(lower(?) in lower(name))>0 OR position(? in phone)>0 ORDER BY active DESC,name,id",q,q.replaceAll("[\\s()-]",""));
 }
 public List<Map<String,Object>> categories(){return db.queryForList("SELECT * FROM service_category ORDER BY name,id");}
 public List<Map<String,Object>> services(){return db.queryForList("SELECT s.*,c.name category FROM service_definition s JOIN service_category c ON c.id=s.category_id ORDER BY c.name,s.name,s.id");}
 public List<Map<String,Object>> bookings(LocalDate day,Long customer){
  return db.queryForList("SELECT * FROM booking WHERE (?::date IS NULL OR day=?) AND (?::bigint IS NULL OR customer_id=?) ORDER BY day DESC,starts,id DESC",day,day,customer,customer);
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
  if(day==null||day.getYear()<2000||day.getYear()>2100||starts==null||token==null)throw new IllegalArgumentException("Wybierz poprawny dzień i godzinę.");
  var existing=db.queryForList("SELECT id FROM booking WHERE request_token=?",Long.class,token);if(!existing.isEmpty())return existing.get(0);
  address=text(address,240,true);
  if(quantities==null||quantities.isEmpty()||quantities.size()>30)throw new IllegalArgumentException("Wybierz od 1 do 30 usług.");
  var lines=new ArrayList<Map<String,Object>>();BigDecimal total=BigDecimal.ZERO;
  for(var entry:quantities.entrySet()){
   BigDecimal qty=entry.getValue();if(qty==null||qty.signum()<=0||qty.scale()>2||qty.compareTo(new BigDecimal("10000"))>0)throw new IllegalArgumentException("Ilość musi być dodatnia, maksymalnie 10000 i dwa miejsca po przecinku.");
   var found=db.queryForList("SELECT * FROM service_definition WHERE id=? AND active=TRUE",entry.getKey());if(found.isEmpty())throw new IllegalArgumentException("Usługa została wyłączona lub nie istnieje. Odśwież cennik.");
   var line=new LinkedHashMap<>(found.get(0));BigDecimal amount=((BigDecimal)line.get("price")).multiply(qty).setScale(2,RoundingMode.HALF_UP);line.put("quantity",qty);line.put("amount",amount);lines.add(line);total=total.add(amount);
  }
  if(customer==null)customer=saveCustomer(null,0,name,number,address,"",true);
  var people=db.queryForList("SELECT * FROM crm_customer WHERE id=? AND active=TRUE",customer);if(people.isEmpty())throw new IllegalArgumentException("Wybierz aktywnego klienta.");
  var person=people.get(0);
  long id=db.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,total) VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long.class,token,customer,day,starts,person.get("name"),person.get("phone"),address,total);
  for(int i=0;i<lines.size();i++){var line=lines.get(i);db.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount) VALUES (?,?,?,?,?,?,?,?)",id,i,line.get("id"),line.get("name"),line.get("unit"),line.get("quantity"),line.get("price"),line.get("amount"));}
  return id;
 }
 @Transactional public void reschedule(long id,long version,LocalDate day,LocalTime starts,String status){
  if(day==null||day.getYear()<2000||day.getYear()>2100||starts==null||status==null||!Set.of("PLANNED","DONE","CANCELLED").contains(status))throw new IllegalArgumentException("Niepoprawny termin lub status.");
  changed(db.update("UPDATE booking SET day=?,starts=?,status=?,version=version+1 WHERE id=? AND version=?",day,starts,status,id,version));
 }
}
