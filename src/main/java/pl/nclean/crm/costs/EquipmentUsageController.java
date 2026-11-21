package pl.nclean.crm.costs;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.nclean.crm.shared.DecimalInput;

@Controller
public class EquipmentUsageController {
    private final EquipmentUsageService usage;
    public EquipmentUsageController(EquipmentUsageService usage){this.usage=usage;}
    @GetMapping("/equipment/standards") String standards(@RequestParam(required=false) Long service,Model m) {
        var services=usage.services();m.addAttribute("services",services);
        var selected=service==null?services.stream().findFirst().orElse(null):usage.service(service);
        m.addAttribute("selected",selected);var standards=selected==null?List.<EquipmentUsageService.Standard>of():usage.standards(selected.id());m.addAttribute("standards",standards);
        var tokens=new LinkedHashMap<Long,UUID>();standards.forEach(s->tokens.put(s.id(),UUID.randomUUID()));m.addAttribute("rowTokens",tokens);
        m.addAttribute("availableCounters",usage.counters(true));m.addAttribute("token",UUID.randomUUID());return "equipment/standards";
    }
    @PostMapping("/equipment/standards") @ResponseBody ResponseEntity<?> standard(@ModelAttribute EquipmentUsageService.StandardCommand c,Principal p) {
        return ResponseEntity.ok(Map.of("saved",true,"id",usage.saveStandard(c,p.getName())));
    }
    @GetMapping("/equipment/jobs/{id}") String job(@PathVariable long id,Model m) {
        var job=usage.job(id);m.addAttribute("job",job);m.addAttribute("availableCounters",usage.counters(false).stream().filter(c->job.usages().stream().noneMatch(u->u.counterId()==c.id())).toList());m.addAttribute("token",UUID.randomUUID());return "equipment/job-usage";
    }
    @PostMapping("/equipment/jobs/prepare") @ResponseBody ResponseEntity<?> prepare(@ModelAttribute EquipmentUsageService.PrepareCommand c,Principal p) {
        return ResponseEntity.ok(Map.of("saved",true,"id",usage.prepare(c,p.getName())));
    }
    @PostMapping("/equipment/jobs/usage") @ResponseBody ResponseEntity<?> override(@RequestParam Map<String,String> p,Principal actor) {
        var minutes=new LinkedHashMap<Long,BigDecimal>();
        for(var field:p.entrySet())if(field.getKey().startsWith("minutes_") && !field.getValue().isBlank())minutes.put(Long.valueOf(field.getKey().substring(8)),DecimalInput.parse(field.getValue(),2,new BigDecimal("100800000"),false));
        if(!p.getOrDefault("extraCounter","").isBlank()) {
            long id=Long.parseLong(p.get("extraCounter"));
            var value=DecimalInput.parse(p.get("extraMinutes"),2,new BigDecimal("100800000"),false);
            if(minutes.putIfAbsent(id,value)!=null)throw new IllegalArgumentException("Ten licznik jest już na liście.");
        } else if(!p.getOrDefault("extraMinutes","").isBlank())throw new IllegalArgumentException("Wybierz dodatkowy licznik.");
        var command=new EquipmentUsageService.OverrideCommand(Long.parseLong(p.get("bookingId")),Long.parseLong(p.get("bookingVersion")),Long.parseLong(p.get("version")),UUID.fromString(Objects.toString(p.get("token"),"")),"true".equals(p.get("noEquipment")),minutes);
        return ResponseEntity.ok(Map.of("saved",true,"id",usage.override(command,actor.getName())));
    }
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.validation.BindException.class}) @ResponseBody
    ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message",ex instanceof org.springframework.validation.BindException?"Sprawdź pola standardu i liczby.":Objects.toString(ex.getMessage(),"Sprawdź formularz.")));
    }
}
