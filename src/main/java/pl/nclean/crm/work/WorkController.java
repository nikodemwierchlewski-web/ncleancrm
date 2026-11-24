package pl.nclean.crm.work;

import pl.nclean.crm.shared.DecimalInput;
import pl.nclean.crm.shared.BusinessDates;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.security.Principal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.nclean.crm.records.*;

@Controller
public class WorkController {
    private final pl.nclean.crm.records.CompletionDefaults defaults;
    private final pl.nclean.crm.context.ContextService context;
    private final pl.nclean.crm.costs.EquipmentUsageService equipmentUsage;
    private final DayReviewService review;private final WorkService work;private final CrmRecords records;
    public WorkController(pl.nclean.crm.records.CompletionDefaults defaults,WorkService work,CrmRecords records,pl.nclean.crm.context.ContextService context,DayReviewService review,pl.nclean.crm.costs.EquipmentUsageService equipmentUsage){this.defaults=defaults;this.equipmentUsage=equipmentUsage;this.review=review;this.context=context;this.work=work;this.records=records;}
    private String panel(long id,Model m){var order=work.order(id);m.addAttribute("equipmentJob",equipmentUsage.job(id));m.addAttribute("returnToOrder",true);m.addAttribute("timeHint",defaults.time(id));m.addAttribute("forecastHints",records.forecastHints(id));m.addAttribute("photos",context.photos(id));m.addAttribute("photoTime",context.time(id));m.addAttribute("photoToken",UUID.randomUUID());m.addAttribute("order",order);m.addAttribute("date",order.get("day"));m.addAttribute("items",records.items(id));m.addAttribute("chemicals",records.materialsForBooking(id));m.addAttribute("fields",records.actualFields(Map.of(id,records.items(id)),records.chemicals(id),records.actuals(id)).getOrDefault(id+":0",Map.of()));m.addAttribute("actualFields",records.actualFields(Map.of(id,records.items(id)),records.chemicals(id),records.actuals(id)));m.addAttribute("payments",work.payments(id));m.addAttribute("audit",work.history(id));m.addAttribute("chemistryHistory",work.chemistryHistory(id));m.addAttribute("today",BusinessDates.today());return "work/order";}
    @GetMapping("/work/{id}") String order(@PathVariable long id,Model m){return panel(id,m);}
    @PostMapping("/work/{id}/timer") String timer(@PathVariable long id,@RequestParam long version,@RequestParam String action,Principal actor,Model m,HttpServletResponse response){try{if(!Set.of("start","stop").contains(action))throw new IllegalArgumentException("Wybierz rozpoczęcie lub zakończenie pomiaru.");work.timer(id,version,action.equals("start"),actor.getName());return "redirect:/work/"+id;}catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("error",ex.getMessage());return panel(id,m);}}
    @PostMapping("/work/{id}/complete") String complete(@PathVariable long id,@RequestParam Map<String,String> p,Principal actor,Model m,HttpServletResponse response,org.springframework.web.servlet.mvc.support.RedirectAttributes redirect){try{
        var prices=new LinkedHashMap<Integer,BigDecimal>();var times=new LinkedHashMap<Integer,Integer>();var measurements=new ArrayList<WorkService.Measurement>();
        for(var item:records.items(id)){int pos=((Number)item.get("position")).intValue();String price=p.get("amount_"+pos),time=p.get("time_"+pos);if(price!=null&&!price.isBlank())prices.put(pos,WorkService.money(price));if(time!=null&&!time.isBlank())times.put(pos,WorkService.minutes(time));if(p.containsKey("confirm_"+pos)){var values=new LinkedHashMap<Long,BigDecimal>();String prefix="usage_"+pos+"_";p.forEach((key,value)->{if(key.startsWith(prefix)&&!value.isBlank())values.put(Long.valueOf(key.substring(prefix.length())),DecimalInput.parse(value,3,new BigDecimal("99999999999.999"),false));});measurements.add(new WorkService.Measurement(pos,Long.parseLong(p.get("actualVersion_"+pos)),values));}}
        work.complete(id,Long.parseLong(p.get("version")),p.get("final_total"),p.get("minutes"),p.getOrDefault("note",""),prices,times,measurements,actor.getName());redirect.addFlashAttribute("tycoonSignal",Map.of("kind","completed","id",id));return "redirect:/work/"+id+"?saved=1";
    }catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("error",ex.getMessage());m.addAttribute("posted",p);return panel(id,m);}}
    /** Old browser tabs must not silently create a second settlement ledger. */
    @PostMapping({"/work/{id}/payments","/work/{id}/payments/{payment}/reverse"})
    @ResponseStatus(org.springframework.http.HttpStatus.GONE)
    String retiredPayments(@PathVariable long id,Model m){
        m.addAttribute("error","Osobne wpłaty zostały wyłączone. Wykonane zlecenie oznacza zapłaconą kwotę końcową. Dawne wpłaty pozostają w archiwum.");
        return panel(id,m);
    }
    @GetMapping("/customers/{id}") String customer(@PathVariable long id,Model m){var customer=records.customer(id);m.addAttribute("contacts",context.contacts(id));m.addAttribute("person",customer);m.addAttribute("orders",records.bookings(null,id));return "work/customer";}
    @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) String invalid(Model m){m.addAttribute("message","Sprawdź datę i parametry zlecenia.");return "records/error";}
}
