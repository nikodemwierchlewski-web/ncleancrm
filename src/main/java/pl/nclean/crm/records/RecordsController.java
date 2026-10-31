package pl.nclean.crm.records;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
public class RecordsController {
 private final CrmRecords data;
 public RecordsController(CrmRecords data){this.data=data;}
 static Long id(Map<String,String> p,String key){String v=p.get(key);return v==null||v.isBlank()?null:Long.valueOf(v);}
 static long version(Map<String,String> p){return Long.parseLong(p.getOrDefault("version","0"));}
 static String required(Map<String,String> p,String key){String value=p.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Uzupełnij wymagane dane formularza.");return value;}
 void common(Model m){m.addAttribute("categories",data.categories());m.addAttribute("services",data.services());}
 @GetMapping("/customers") String customers(Model m){m.addAttribute("customers",data.customers(""));return "records/customers";}
 @PostMapping("/customers/search") String search(@RequestParam String query,Model m){m.addAttribute("customers",data.customers(query));return "records/customers";}
 @PostMapping("/customers/lookup") @ResponseBody List<Map<String,Object>> lookup(@RequestParam String query){return data.customers(query).stream().filter(p->Boolean.TRUE.equals(p.get("active"))).limit(50).map(p->Map.of("id",p.get("id"),"name",p.get("name"),"phone",p.get("phone"),"address",p.get("address"))).toList();}
 @PostMapping("/customers") String customer(@RequestParam Map<String,String> p){data.saveCustomer(id(p,"id"),version(p),p.get("name"),p.get("phone"),p.get("address"),p.get("notes"),p.containsKey("active"));return "redirect:/customers";}
 @GetMapping("/settings/services") String services(Model m){common(m);return "records/services";}
 @PostMapping("/settings/services/category") String category(@RequestParam Map<String,String> p){data.category(id(p,"id"),p.get("name"));return "redirect:/settings/services";}
 @PostMapping("/settings/services") String service(@RequestParam Map<String,String> p){data.service(id(p,"id"),version(p),Long.parseLong(p.get("category")),p.get("name"),p.get("unit"),p.get("price"),Integer.parseInt(p.get("minutes")),p.containsKey("active"));return "redirect:/settings/services";}
 @GetMapping("/orders") String orders(@RequestParam Map<String,String> p,Model m){
  common(m);LocalDate day=p.containsKey("day")?LocalDate.parse(p.get("day")):null;
  var orders=data.bookings(day,id(p,"customer"));m.addAttribute("orders",orders);
  var lines=new HashMap<Long,List<Map<String,Object>>>();for(var order:orders)lines.put(((Number)order.get("id")).longValue(),data.items(((Number)order.get("id")).longValue()));m.addAttribute("lines",lines);
  m.addAttribute("day",day==null?LocalDate.now(ZoneId.of("Europe/Warsaw")):day);m.addAttribute("token",UUID.randomUUID());return "records/orders";
 }
 @PostMapping("/orders") String book(@RequestParam Map<String,String> p){
  var quantities=new LinkedHashMap<Long,BigDecimal>();
  p.forEach((key,value)->{if(key.startsWith("qty_")&&!value.isBlank()){var q=new BigDecimal(value.replace(',','.'));if(q.signum()!=0)quantities.put(Long.parseLong(key.substring(4)),q);}});
  data.book(UUID.fromString(required(p,"token")),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),id(p,"customer"),p.get("name"),p.get("phone"),p.get("address"),quantities);return "redirect:/orders?day="+p.get("day");
 }
 @PostMapping("/orders/{id}") String update(@PathVariable long id,@RequestParam Map<String,String> p){data.reschedule(id,version(p),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),p.get("status"));return "redirect:/orders?day="+p.get("day");}
 @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class,DataIntegrityViolationException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
 String invalid(Exception ex,Model model){model.addAttribute("message",ex instanceof NumberFormatException||ex instanceof DateTimeException?"Sprawdź daty i wartości liczbowe.":ex instanceof DataIntegrityViolationException?"Zapis nie powiódł się. Sprawdź listę przed ponownym dodaniem.":ex.getMessage());return "records/error";}
}
