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
 private final ActualUsageService actualUsage;
 public RecordsController(CrmRecords data,ChemistryService chemistry,ActualUsageService actualUsage){this.data=data;this.chemistry=chemistry;this.actualUsage=actualUsage;}
 static Long id(Map<String,String> p,String key){String v=p.get(key);return v==null||v.isBlank()?null:Long.valueOf(v);}
 static long version(Map<String,String> p){return Long.parseLong(p.getOrDefault("version","0"));}
 static String required(Map<String,String> p,String key){String value=p.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Uzupełnij wymagane dane formularza.");return value;}
 void common(Model m){m.addAttribute("categories",data.categories());m.addAttribute("services",data.services());m.addAttribute("chemicals",data.materials());m.addAttribute("recipes",data.recipes());m.addAttribute("standardCosts",data.standardCosts());m.addAttribute("usageSuggestions",actualUsage.suggestions());}
 @GetMapping("/customers") String customers(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page,Model m){if(query.length()>120)throw new IllegalArgumentException("Szukaj po nazwie lub telefonie — do 120 znaków.");var rows=data.customerPage(query,page,false);m.addAttribute("hasNext",rows.size()>25);m.addAttribute("page",page);m.addAttribute("customers",rows.stream().limit(25).toList());m.addAttribute("query",query);return "records/customers";}
 @PostMapping("/customers/search") String search(@RequestParam String query,Model m){if(query.length()>120)throw new IllegalArgumentException("Szukaj po nazwie lub telefonie — do 120 znaków.");return "redirect:/customers?query="+java.net.URLEncoder.encode(query,java.nio.charset.StandardCharsets.UTF_8);}
 @PostMapping("/customers/lookup") @ResponseBody List<Map<String,Object>> lookup(@RequestParam String query){return data.customerPage(query,0,true).stream().map(p->Map.of("id",p.get("id"),"name",p.get("name"),"phone",p.get("phone"),"address",p.get("address"))).toList();}
 @PostMapping("/customers") String customer(@RequestParam Map<String,String> p){data.saveCustomerProfile(new CustomerProfileCommand(id(p,"id"),version(p),p.get("name"),p.get("phone"),p.get("address"),p.get("notes"),p.containsKey("active"),p.containsKey("source"),p.get("source"),p.get("referral_note")));return "redirect:/customers";}
 @GetMapping("/more") String more(){return "settings/more";}
 @GetMapping("/settings") String settings(){return "settings/index";}
 @GetMapping("/settings/services") String services(Model m){common(m);m.addAttribute("removedCatalogue",data.removedCatalogue());return "records/services";}
 @PostMapping("/settings/services/chemicals") String chemical(@RequestParam Map<String,String> p){chemistry.saveMaterial(id(p,"id"),version(p),p.get("name"),p.get("packageUnit"),p.get("packageQuantity"),p.get("packagePrice"));return "redirect:/settings/services";}
 @PostMapping("/settings/services/{id}/chemistry") String recipe(@PathVariable long id,@RequestParam Map<String,String> p){
  var quantities=new LinkedHashMap<Long,BigDecimal>();p.forEach((key,value)->{if(key.startsWith("standard_")&&!value.isBlank()){var q=ChemistryService.quantity(value,false);if(q.signum()>0)quantities.put(Long.valueOf(key.substring(9)),q);}});
  chemistry.saveRecipe(id,version(p),p.containsKey("defined"),quantities);return "redirect:/settings/services";
 }
 @PostMapping("/settings/services/{id}/suggestion") String applySuggestion(@PathVariable long id,@RequestParam Map<String,String> p){actualUsage.apply(id,Long.parseLong(required(p,"chemical")),version(p));return "redirect:/settings/services";}
 @PostMapping(value="/orders/{id}/items/{position}/actual",produces="application/json") @ResponseBody
 Map<String,Boolean> actual(@PathVariable long id,@PathVariable int position,@RequestParam Map<String,String> p){
  var amounts=new LinkedHashMap<Long,BigDecimal>();p.forEach((key,value)->{if(key.startsWith("actual_")&&!value.isBlank())amounts.put(Long.valueOf(key.substring(7)),ChemistryService.quantity(value,false));});
  actualUsage.record(id,position,version(p),amounts);return Map.of("saved",true);
 }
 @PostMapping("/settings/services/category") String category(@RequestParam Map<String,String> p){data.category(id(p,"id"),p.get("name"));return "redirect:/settings/services";}
 @PostMapping("/settings/services") String service(@RequestParam Map<String,String> p){data.service(id(p,"id"),version(p),Long.parseLong(p.get("category")),p.get("name"),p.get("unit"),p.get("price"),Integer.parseInt(p.get("minutes")),p.containsKey("active"));return "redirect:/settings/services";}
 @GetMapping("/orders") String orders(@RequestParam Map<String,String> p,Model m){
  common(m);m.addAttribute("removedOrders",data.removedBookings());LocalDate day=p.containsKey("day")?LocalDate.parse(p.get("day")):null;
  int page=Integer.parseInt(p.getOrDefault("page","0"));var found=data.bookingPage(day,id(p,"customer"),page);var orders=found.stream().limit(25).toList();m.addAttribute("orders",orders);m.addAttribute("page",page);m.addAttribute("hasNext",found.size()>25);m.addAttribute("filterDay",day);m.addAttribute("filterCustomer",id(p,"customer"));
  var ids=orders.stream().map(o->((Number)o.get("id")).longValue()).toList();
  var lines=data.bookingRows("items",ids).stream().collect(java.util.stream.Collectors.groupingBy(o->((Number)o.get("booking_id")).longValue()));m.addAttribute("lines",lines);
  var chemicalLines=data.bookingRows("chemicals",ids).stream().collect(java.util.stream.Collectors.groupingBy(o->((Number)o.get("booking_id")).longValue()));ids.forEach(id->{lines.computeIfAbsent(id,k->new ArrayList<>());chemicalLines.computeIfAbsent(id,k->new ArrayList<>());});m.addAttribute("bookingChemicals",chemicalLines);
  var actualRows=data.bookingRows("actuals",ids);
  m.addAttribute("actualRows",actualRows);m.addAttribute("actualFields",data.actualFields(lines,chemicalLines.values().stream().flatMap(List::stream).toList(),actualRows));
  if(p.containsKey("repeat")){long source=Long.parseLong(p.get("repeat"));var previous=data.booking(source);var person=data.customer(((Number)previous.get("customer_id")).longValue());if(!Boolean.TRUE.equals(person.get("active")))throw new IllegalArgumentException("Klient jest nieaktywny.");m.addAttribute("repeatPerson",person);m.addAttribute("repeatAddress",previous.get("address"));m.addAttribute("repeatItems",data.items(source));}
  m.addAttribute("day",day==null?LocalDate.now(ZoneId.of("Europe/Warsaw")):day);m.addAttribute("token",UUID.randomUUID());return "records/orders";
 }
 @PostMapping("/orders") String book(@RequestParam Map<String,String> p,org.springframework.web.servlet.mvc.support.RedirectAttributes redirect){long id=createBooking(p);redirect.addFlashAttribute("tycoonSignal",Map.of("kind","booked","id",id));return "redirect:/orders?day="+p.get("day");}
 private long createBooking(Map<String,String> p){
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
  return data.book(UUID.fromString(required(p,"token")),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),id(p,"customer"),p.get("name"),p.get("phone"),p.get("address"),quantities,prices,forecasts,p.get("source"),p.getOrDefault("referral_note",""),p.getOrDefault("note",""));
 }
 @PostMapping(value="/orders",params="calendar",produces="application/json") @ResponseBody
 Map<String,Object> calendarBook(@RequestParam Map<String,String> p){return Map.of("saved",true,"id",createBooking(p),"motion","booked");}
 @PostMapping(value="/orders/{id}",params="calendar",produces="application/json") @ResponseBody
 Map<String,Object> calendarUpdate(@PathVariable long id,@RequestParam Map<String,String> p){reschedule(id,p);return Map.of("saved",true,"id",id,"motion",p.get("status").equals("DONE")?"completed":"updated");}
 @PostMapping("/orders/{id}") String update(@PathVariable long id,@RequestParam Map<String,String> p,org.springframework.web.servlet.mvc.support.RedirectAttributes redirect){reschedule(id,p);redirect.addFlashAttribute("tycoonSignal",Map.of("kind",p.get("status").equals("DONE")?"completed":"updated","id",id));return "redirect:/orders?day="+p.get("day");}
 private void reschedule(long id,Map<String,String> p){data.reschedule(id,version(p),LocalDate.parse(required(p,"day")),LocalTime.parse(required(p,"starts")),p.get("status"));}
 @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class,DataIntegrityViolationException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
 Object invalid(Exception ex,Model model,jakarta.servlet.http.HttpServletRequest request){model.addAttribute("message",ex instanceof NumberFormatException||ex instanceof DateTimeException?"Sprawdź daty i wartości liczbowe.":ex instanceof DataIntegrityViolationException?"Zapis nie powiódł się. Sprawdź listę przed ponownym dodaniem.":ex.getMessage());if(request.getParameter("calendar")!=null)return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message",model.getAttribute("message")));return "records/error";}
}
