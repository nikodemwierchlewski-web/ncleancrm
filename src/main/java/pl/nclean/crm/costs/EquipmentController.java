package pl.nclean.crm.costs;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.nclean.crm.shared.BusinessDates;

@Controller
public class EquipmentController {
    private final EquipmentService equipment;
    public EquipmentController(EquipmentService equipment) { this.equipment=equipment; }

    @GetMapping("/equipment") String list(@RequestParam(defaultValue="active") String status,
            @RequestParam(defaultValue="") String q,Model model) {
        var assets=equipment.assets(status,q);
        var counters=new LinkedHashMap<Long,List<EquipmentService.Counter>>();
        var next=new LinkedHashMap<Long,EquipmentService.Plan>();
        var plans=equipment.plans(null,BusinessDates.today());
        for(var asset:assets){
            counters.put(asset.id(),equipment.counters(asset.id(),BusinessDates.today()));
            plans.stream().filter(p->p.assetId()==asset.id() && p.active()).findFirst().ifPresent(p->next.put(asset.id(),p));
        }
        model.addAttribute("assets",assets);model.addAttribute("counterSummary",counters);model.addAttribute("nextService",next);model.addAttribute("status",status);
        model.addAttribute("q",q);model.addAttribute("attention",equipment.attention(BusinessDates.today()));
        model.addAttribute("token",UUID.randomUUID());return "equipment/index";
    }
    @GetMapping("/equipment/{id}") String detail(@PathVariable long id,
            @RequestParam(defaultValue="all") String history,Model model) {
        model.addAttribute("asset",equipment.asset(id));model.addAttribute("counters",equipment.counters(id,BusinessDates.today()));
        model.addAttribute("plans",equipment.plans(id,BusinessDates.today()));model.addAttribute("readings",equipment.readings(id,history));
        model.addAttribute("executions",equipment.executions(id,history));model.addAttribute("expenses",equipment.expenses(id,history));
        model.addAttribute("history",history);model.addAttribute("token",UUID.randomUUID());return "equipment/detail";
    }
    @GetMapping("/equipment/editor") String editor(@RequestParam String type,@RequestParam(required=false) Long id,
            @RequestParam(required=false) Long asset,Model model) {
        if(!List.of("asset","counter","reading","plan","execute","snooze","cancel").contains(type))throw new IllegalArgumentException("Nieznany formularz.");
        Long assetId=asset;
        if(type.equals("asset") && id!=null)assetId=id;
        if(type.equals("reading")) {var c=equipment.counter(requiredId(id),BusinessDates.today());assetId=c.assetId();model.addAttribute("counter",c);}
        if(List.of("execute","snooze","cancel").contains(type)) {
            var plan=equipment.plan(requiredId(id),BusinessDates.today());assetId=plan.assetId();model.addAttribute("plan",plan);
            if(plan.counterId()!=null)model.addAttribute("counter",equipment.counter(plan.counterId(),BusinessDates.today()));
        }
        if(assetId==null && !type.equals("asset"))throw new IllegalArgumentException("Wybierz sprzęt.");
        if(assetId!=null) {model.addAttribute("asset",equipment.asset(assetId));model.addAttribute("counters",equipment.counters(assetId,BusinessDates.today()));}
        model.addAttribute("type",type);model.addAttribute("token",UUID.randomUUID());model.addAttribute("today",BusinessDates.today());
        model.addAttribute("tomorrow",BusinessDates.today().plusDays(1));return "equipment/editor";
    }
    @PostMapping("/equipment/asset") @ResponseBody ResponseEntity<?> asset(@ModelAttribute EquipmentService.AssetCommand c,Principal p) {
        return save(()->equipment.saveAsset(c,p.getName()));
    }
    @PostMapping("/equipment/archive") @ResponseBody ResponseEntity<?> archive(@ModelAttribute EquipmentService.ArchiveCommand c,Principal p) {
        return save(()->equipment.archive(c,p.getName()));
    }
    @PostMapping("/equipment/counter") @ResponseBody ResponseEntity<?> counter(@ModelAttribute EquipmentService.CounterCommand c,Principal p) {
        return save(()->equipment.addCounter(c,p.getName()));
    }
    @PostMapping("/equipment/reading") @ResponseBody ResponseEntity<?> reading(@ModelAttribute EquipmentService.ReadingCommand c,Principal p) {
        return save(()->equipment.addReading(c,p.getName()));
    }
    @PostMapping("/equipment/plan") @ResponseBody ResponseEntity<?> plan(@ModelAttribute EquipmentService.PlanCommand c,Principal p) {
        return save(()->equipment.addPlan(c,p.getName()));
    }
    @PostMapping("/equipment/execute") @ResponseBody ResponseEntity<?> execute(@ModelAttribute EquipmentService.ExecutionCommand c,Principal p) {
        return save(()->equipment.execute(c,p.getName()));
    }
    @PostMapping("/equipment/snooze") @ResponseBody ResponseEntity<?> snooze(@ModelAttribute EquipmentService.PlanAction c,Principal p) {
        return save(()->equipment.snooze(c,p.getName()));
    }
    @PostMapping("/equipment/cancel") @ResponseBody ResponseEntity<?> cancel(@ModelAttribute EquipmentService.PlanAction c,Principal p) {
        return save(()->equipment.cancelPlan(c,p.getName()));
    }
    private static long requiredId(Long id) { if(id==null)throw new IllegalArgumentException("Wybierz licznik lub plan.");return id; }
    private ResponseEntity<?> save(LongSupplier operation) {
        return ResponseEntity.ok(Map.of("saved",true,"id",operation.getAsLong()));
    }
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class,org.springframework.validation.BindException.class})
    @ResponseBody ResponseEntity<?> invalid(Exception ex) {
        String message=ex instanceof org.springframework.validation.BindException binding?"Sprawdź pola: "+String.join(", ",binding.getFieldErrors().stream().map(org.springframework.validation.FieldError::getField).distinct().toList())+".":Objects.toString(ex.getMessage(),"Sprawdź pola formularza.");
        return ResponseEntity.badRequest().body(Map.of("message",message));
    }
}
