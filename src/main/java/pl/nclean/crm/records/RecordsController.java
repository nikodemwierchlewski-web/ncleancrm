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
 private final ChemistryService chemistry;
 public RecordsController(CrmRecords data,ChemistryService chemistry){this.data=data;this.chemistry=chemistry;}
 static Long id(Map<String,String> p,String key){String v=p.get(key);return v==null||v.isBlank()?null:Long.valueOf(v);}
 static long version(Map<String,String> p){return Long.parseLong(p.getOrDefault("version","0"));}
 static String required(Map<String,String> p,String key){String value=p.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Uzupełnij wymagane dane formularza.");return value;}
 void common(Model m){m.addAttribute("categories",data.categories());m.addAttribute("services",data.services());m.addAttribute("chemicals",data.materials());m.addAttribute("recipes",data.recipes());m.addAttribute("standardCosts",data.standardCosts());}
 @GetMapping("/customers") String customers(Model m){m.addAttribute("customers",data.customers(""));return "records/customers";}
 @PostMapping("/customers/search") String search(@RequestParam String query,Model m){m.addAttribute("customers",data.customers(query));return "records/customers";}
 @PostMapping("/customers/lookup") @ResponseBody List<Map<String,Object>> lookup(@RequestParam String query){return data.customers(query).stream().filter(p->Boolean.TRUE.equals(p.get("active"))).limit(50).map(p->Map.of("id",p.get("id"),"name",p.get("name"),"phone",p.get("phone"),"address",p.get("address"))).toList();}
 @PostMapping("/customers") String customer(@RequestParam Map<String,String> p){data.saveCustomer(id(p,"id"),version(p),p.get("name"),p.get("phone"),p.get("address"),p.get("notes"),p.containsKey("active"));return "redirect:/customers";}
 @GetMapping("/settings") String settings(){return "settings/index";}
 @GetMapping("/settings/services") String services(Model m){common(m);return "records/services";}
 @PostMapping("/settings/services/chemicals") String chemical(@RequestParam Map<String,String> p){chemistry.saveMaterial(id(p,"id"),version(p),p.get("name"),p.get("packageUnit"),p.get("packageQuantity"),p.get("packagePrice"));return "redirect:/settings/services";}
 @PostMapping("/settings/services/{id}/chemistry") String recipe(@PathVariable long id,@RequestParam Map<String,String> p){
  var quantities=new LinkedHashMap<Long,BigDecimal>();p.forEach((key,value)->{if(key.startsWith("standard_")&&!value.isBlank()){var q=ChemistryService.quantity(value,false);if(q.signum()>0)quantities.put(Long.valueOf(key.substring(9)),q);}});
  chemistry.saveRecipe(id,version(p),p.containsKey("defined"),quantities);return "redirect:/settings/services";
 }
 @PostMapping("/settings/services/category") String category(@RequestParam Map<String,String> p){data.category(id(p,"id"),p.get("name"));return "redirect:/settings/services";}
 @PostMapping("/settings/services") String service(@RequestParam Map<String,String> p){data.service(id(p,"id"),version(p),Long.parseLong(p.get("category")),p.get("name"),p.get("unit"),p.get("price"),Integer.parseInt(p.get("minutes")),p.containsKey("active"));return "redirect:/settings/services";}
 @GetMapping("/orders") String orders(@RequestParam Map<String,String> p,Model m){
  common(m);LocalDate day=p.containsKey("day")?LocalDate.parse(p.get("day")):null;
  var orders=data.bookings(day,id(p,"customer"));m.addAttribute("orders",orders);
  var lines=new HashMap<Long,List<Map<String,Object>>>();for(var order:orders)lines.put(((Number)order.get("id")).longValue(),data.items(((Number)order.get("id")).longValue()));m.addAttribute("lines",lines);var chemicalLines=new HashMap<Long,List<Map<String,Object>>>();for(var order:orders){long id=((Number)order.get("id")).longValue();chemicalLines.put(id,data.chemicals(id));}m.addAttribute("bookingChemicals",chemicalLines);
  m.addAttribute("day",day==null?LocalDate.now(ZoneId.of("Europe/Warsaw")):day);m.addAttribute("token",UUID.randomUUID());return "records/orders";
 }
 @PostMapping("/orders") String book(@RequestParam Map<String,String> p){
  var quantities=new LinkedHashMap<Long,BigDecimal>();
  p.forEach((key,value)->{if(key.startsWith("qty_")&&!value.isBlank()){var q=new BigDecimal(value.replace(',','.'));if(q.signum()!=0)quantities.put(Long.parseLong(key.substring(4)),q);}});
  var prices=new LinkedHashMap<Long,BigDecimal>();var forecasts=new LinkedHashMap<Long,ChemistryService.Forecast>();
  for(var service:quantities.keySet()){
   if(p.containsKey("price_"+service))prices.put(service,CrmRecords.money(required(p,"price_"+service)));
   if(p.containsKey("forecast_"+service)){
    String mode=required(p,"forecast_"+service);if(!Set.of("defined","unknown").contains(mode))throw new IllegalArgumentException("Niepoprawny stan prognozy.");
    var usage=new LinkedHashMap<Long,BigDecimal>();String prefix="chem_"+service+"_";
    p.forEach((key,value)->{if(key.startsWith(prefix)&&!value.isBlank())usage.put(Long.valueOf(key.substring(prefix.length())),ChemistryService.quantity(value,false));});
    forecasts.put(service,new ChemistryService.Forecast(mode.equals("defined"),usage));
   }
  }
  data.book(UUID.fromString(required(p,"token")),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),id(p,"customer"),p.get("name"),p.get("phone"),p.get("address"),quantities,prices,forecasts);return "redirect:/orders?day="+p.get("day");
 }
 @PostMapping(value="/orders",params="calendar",produces="application/json") @ResponseBody
 Map<String,Boolean> calendarBook(@RequestParam Map<String,String> p){book(p);return Map.of("saved",true);}
 @PostMapping(value="/orders/{id}",params="calendar",produces="application/json") @ResponseBody
 Map<String,Boolean> calendarUpdate(@PathVariable long id,@RequestParam Map<String,String> p){update(id,p);return Map.of("saved",true);}
 @PostMapping("/orders/{id}") String update(@PathVariable long id,@RequestParam Map<String,String> p){data.reschedule(id,version(p),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),p.get("status"));return "redirect:/orders?day="+p.get("day");}
 @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class,DataIntegrityViolationException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
 Object invalid(Exception ex,Model model,jakarta.servlet.http.HttpServletRequest request){model.addAttribute("message",ex instanceof NumberFormatException||ex instanceof DateTimeException?"Sprawdź daty i wartości liczbowe.":ex instanceof DataIntegrityViolationException?"Zapis nie powiódł się. Sprawdź listę przed ponownym dodaniem.":ex.getMessage());if(request.getParameter("calendar")!=null)return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message",model.getAttribute("message")));return "records/error";}
}
