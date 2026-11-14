package pl.nclean.crm.fuel;

import pl.nclean.crm.shared.BusinessDates;
import java.time.*;
import java.util.*;
import java.security.Principal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Controller
public class FuelController {
    private final FuelJournalService fuel;private final ReceiptService receipts;
    public FuelController(FuelJournalService fuel,ReceiptService receipts){this.fuel=fuel;this.receipts=receipts;}
    public void populate(LocalDate date,BusinessDates.Period period,Model m){
        m.addAttribute("legacyFuel",fuel.legacyFuel(date));m.addAttribute("journal",fuel.daily(date));m.addAttribute("previousReading",fuel.previous(date));m.addAttribute("nextReading",fuel.next(date));m.addAttribute("fuelUsage",fuel.usage(date,date).get(date));m.addAttribute("fuelTotals",fuel.totals(period.start(),period.end()));m.addAttribute("fuelParameters",fuel.parameters(date));
    }
    private String page(LocalDate date,Long purchase,Model m){
        BusinessDates.period(date,"day");m.addAttribute("date",date);populate(date,BusinessDates.period(date,"month"),m);m.addAttribute("journalHistory",fuel.history(date));m.addAttribute("journalAudit",fuel.audit(date));m.addAttribute("purchases",fuel.purchases(date.withDayOfMonth(1),YearMonth.from(date).atEndOfMonth()));m.addAttribute("comparisons",fuel.comparisons(date));m.addAttribute("parameterHistory",fuel.parameters());m.addAttribute("today",BusinessDates.today());
        m.addAttribute("purchase",purchase==null?Map.of("version",-1,"request_token",UUID.randomUUID(),"purchased_at",date.atTime(LocalTime.now(ZoneId.of("Europe/Warsaw")).withSecond(0).withNano(0)),"target","CAR","fuel_type",fuel.parameters(date).getOrDefault("car_fuel","PETROL"),"full_tank",false,"records_complete",false):fuel.purchaseForEdit(purchase));
        m.addAttribute("parameterToken",UUID.randomUUID());return "fuel/index";
    }
    @GetMapping("/fuel") String index(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) Long purchase,Model m){return page(date==null?BusinessDates.today():date,purchase,m);}
    @PostMapping("/fuel/day") String saveDay(@RequestParam Map<String,String> p,Principal principal,Model m,HttpServletResponse response){
        LocalDate date=LocalDate.parse(p.get("day"));try{fuel.saveDay(date,Long.parseLong(p.get("version")),p.get("odometer"),Integer.parseInt(p.get("hours")),Integer.parseInt(p.get("minutes")),p.containsKey("confirmed"),principal.getName());return "redirect:/dashboard?date="+date+"&fuelSaved=1";}
        catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("fuelError",ex.getMessage());m.addAttribute("postedDay",p);return page(date,null,m);}
    }
    @PostMapping("/fuel/parameters") String parameters(@RequestParam Map<String,String> p,Model m,HttpServletResponse response){
        LocalDate date=LocalDate.parse(p.get("effective_day"));try{fuel.saveParameters(UUID.fromString(p.get("token")),date,p.get("car_rate"),p.get("generator_rate"),p.get("car_fuel"),p.get("generator_fuel"),p.get("car_price"),p.get("generator_price"));return "redirect:/fuel?date="+date+"&saved=1";}
        catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("fuelError",ex.getMessage());m.addAttribute("postedParameters",p);return page(date,null,m);}
    }
    @PostMapping("/fuel/purchases") String purchase(@RequestParam Map<String,String> p,Principal principal,Model m,HttpServletResponse response){
        LocalDateTime time=LocalDateTime.parse(p.get("purchased_at"));Long id=blank(p.get("id"))?null:Long.valueOf(p.get("id"));
        try{fuel.savePurchase(new FuelJournalService.Purchase(id,Long.parseLong(p.get("version")),UUID.fromString(p.get("request_token")),time,p.get("target"),p.get("fuel_type"),p.get("amount"),p.get("litres"),p.get("litre_price"),p.containsKey("full_tank"),p.get("odometer"),p.containsKey("records_complete"),p.getOrDefault("note",""),blank(p.get("receipt_id"))?null:UUID.fromString(p.get("receipt_id"))),principal.getName());return "redirect:/fuel?date="+time.toLocalDate()+"&saved=1#purchases";}
        catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("fuelError",ex.getMessage());m.addAttribute("postedPurchase",p);return page(time.toLocalDate(),id,m);}
    }
    private static boolean blank(String s){return s==null||s.isBlank();}
    @PostMapping("/fuel/receipts") @ResponseBody ResponseEntity<Map<String,Object>> upload(@RequestParam("image") MultipartFile image){try{var id=receipts.upload(image);return ResponseEntity.ok(Map.of("id",id,"url","/fuel/receipts/"+id));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}}
    @GetMapping("/fuel/receipts/{id}") ResponseEntity<byte[]> image(@PathVariable UUID id){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline; filename=receipt-"+id+".jpg").body(receipts.image(id));}
    @PostMapping("/fuel/receipts/{id}/discard") @ResponseBody Map<String,Boolean> discard(@PathVariable UUID id){receipts.discard(id);return Map.of("ok",true);}
    @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseBody ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.badRequest().body(Map.of("error","Zdjęcie jest za duże. Wybierz JPEG lub PNG do 8 MB."));}
    @ExceptionHandler({IllegalArgumentException.class,java.time.format.DateTimeParseException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalid(Model m){m.addAttribute("message","Sprawdź daty i pola formularza paliwa.");return "records/error";}
}
