package pl.nclean.crm.notes;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Controller
public class NoteController {
    private final NoteService service;
    public NoteController(NoteService service){this.service=service;}
    @GetMapping("/notes") String page(@RequestParam(defaultValue="active") String state,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="false") boolean pinned,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("notes",service.list(state,q,pinned,page));m.addAttribute("state",state);m.addAttribute("q",q);m.addAttribute("pinned",pinned);m.addAttribute("noteDraft",service.draft());return "notes/index";}
    @GetMapping("/notes/editor") String editor(@RequestParam(required=false) Long id,Model m){m.addAttribute("noteDraft",id==null?service.draft():service.get(id));return "notes/editor";}
    @PostMapping("/notes") @ResponseBody ResponseEntity<?> save(@RequestParam Map<String,String> p){try{
        var steps=new ArrayList<NoteService.Step>();for(var key:p.keySet())if(key.startsWith("step_text_")){int index=Integer.parseInt(key.substring(10));if(index<0||index>=30)throw new IllegalArgumentException("Dodaj do 30 kroków.");}
        for(int i=0;i<30;i++)if(p.containsKey("step_text_"+i))steps.add(new NoteService.Step(UUID.fromString(Objects.toString(p.get("step_id_"+i),"")),p.get("step_text_"+i),p.containsKey("step_done_"+i)));
        Long id=p.get("id")==null||p.get("id").isBlank()?null:Long.valueOf(p.get("id"));String date=p.get("due");long saved=service.save(id,Long.parseLong(p.getOrDefault("version","0")),UUID.fromString(Objects.toString(p.get("token"),"")),p.get("kind"),p.get("title"),p.get("content"),date==null||date.isBlank()?null:LocalDate.parse(date),p.containsKey("pinned"),steps);return ResponseEntity.ok(Map.of("saved",true,"id",saved));
    }catch(IllegalArgumentException|DateTimeException ex){return error(ex);}}
    @PostMapping("/notes/{id}/action") @ResponseBody ResponseEntity<?> action(@PathVariable long id,@RequestParam long version,@RequestParam String action,@RequestParam(required=false) UUID step,@RequestParam(defaultValue="false") boolean checked){try{service.action(id,version,action,step,checked);return ResponseEntity.ok(Map.of("saved",true));}catch(IllegalArgumentException ex){return error(ex);}}
    private static ResponseEntity<?> error(Exception ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Sprawdź dane wpisu.":ex.getMessage()));}
    @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) String invalid(Model m){m.addAttribute("message","Sprawdź filtr, datę lub identyfikator wpisu.");return "records/error";}
}
