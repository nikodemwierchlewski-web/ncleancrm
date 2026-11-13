package pl.nclean.crm.work;

import java.time.*;
import java.util.*;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.records.CrmRecords;
import pl.nclean.crm.context.ContextService;

@Controller
public class DayReviewController {
    private final DayReviewService service;private final WorkService work;private final CrmRecords records;private final ContextService context;private final DashboardService dashboard;
    public DayReviewController(DayReviewService service,WorkService work,CrmRecords records,ContextService context,DashboardService dashboard){this.service=service;this.work=work;this.records=records;this.context=context;this.dashboard=dashboard;}
    @GetMapping("/day-closeout") String page(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) Long order,Model m){
        var day=date==null?DashboardService.today():date;var review=service.review(day);m.addAttribute("review",review);m.addAttribute("date",day);m.addAttribute("today",DashboardService.today());m.addAttribute("summary",dashboard.summary(day,"day","actual"));
        if(order!=null&&review.orders().stream().noneMatch(o->((Number)o.get("id")).longValue()==order))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Wizyta nie należy do tego dnia lub nie istnieje.");
        var chosen=review.orders().stream().filter(o->order!=null&&((Number)o.get("id")).longValue()==order).findFirst().orElseGet(()->review.orders().stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))).findFirst().orElse(review.orders().isEmpty()?null:review.orders().get(0)));
        if(chosen!=null){long id=((Number)chosen.get("id")).longValue();var items=records.items(id);m.addAttribute("forecastHints",records.forecastHints(id));m.addAttribute("order",work.order(id));m.addAttribute("items",items);m.addAttribute("chemicals",records.materials());m.addAttribute("actualFields",records.actualFields(Map.of(id,items),records.chemicals(id),records.actuals(id)));m.addAttribute("photos",context.photos(id));m.addAttribute("photoTime",context.time(id));m.addAttribute("photoToken",UUID.randomUUID());}
        return "work/closeout";
    }
    @PostMapping("/day-closeout/{id}") @ResponseBody ResponseEntity<?> save(@PathVariable long id,@RequestParam LocalDate date,@RequestParam Map<String,String> p,Principal actor){try{service.save(id,date,p,actor.getName());var r=service.review(date);Long next=r.orders().stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))&&((Number)o.get("id")).longValue()!=id).map(o->((Number)o.get("id")).longValue()).findFirst().orElseGet(()->r.orders().isEmpty()?null:((Number)r.orders().get(0).get("id")).longValue());return ResponseEntity.ok(Map.of("url","order".equals(p.get("return_to"))?"/work/"+id+"?saved=1":"/day-closeout?date="+date+(next==null?"":"&order="+next)+"&saved=1","motion","DONE".equals(p.get("outcome"))?"completed":"updated","id",id));}catch(IllegalArgumentException|DateTimeException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Sprawdź dane.":ex.getMessage()));}}
    @PostMapping("/day-closeout/close") @ResponseBody ResponseEntity<?> close(@RequestParam LocalDate date,@RequestParam String fingerprint,Principal actor){try{service.close(date,fingerprint,actor.getName());return ResponseEntity.ok(Map.of("url","/day-closeout?date="+date+"&closed=1","motion","day","id",0));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}}
}
