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
    @GetMapping("/notes") String page(@RequestParam(defaultValue="active") String state,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="false") boolean pinned,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean todayPinned,Model m){m.addAttribute("notes",service.list(state,q,pinned,todayPinned,page));m.addAttribute("todayPinned",todayPinned);m.addAttribute("state",state);m.addAttribute("q",q);m.addAttribute("pinned",pinned);m.addAttribute("noteDraft",service.draft());return "notes/index";}
    @GetMapping("/notes/editor") String editor(@RequestParam(required=false) Long id,Model m){m.addAttribute("noteDraft",id==null?service.draft():service.get(id));m.addAttribute("reminder",service.reminderConfiguration(id));m.addAttribute("dueTime",service.dueTime(id));return "notes/editor";}
    @GetMapping("/notes/main-goal") @ResponseBody ResponseEntity<?> mainGoal(){var goal=service.mainGoalView();return ResponseEntity.ok(goal==null?Map.of("selected",false):Map.of("selected",true,"goal",goal));}
    @PostMapping("/notes") @ResponseBody ResponseEntity<?> save(@RequestParam Map<String,String> p){try{
        var steps=new ArrayList<NoteService.Step>();for(var key:p.keySet())if(key.startsWith("step_text_")){int index=Integer.parseInt(key.substring(10));if(index<0||index>=30)throw new IllegalArgumentException("Dodaj do 30 kroków.");}
        for(int i=0;i<30;i++)if(p.containsKey("step_text_"+i))steps.add(new NoteService.Step(UUID.fromString(Objects.toString(p.get("step_id_"+i),"")),p.get("step_text_"+i),p.containsKey("step_done_"+i)));
        Long id=p.get("id")==null||p.get("id").isBlank()?null:Long.valueOf(p.get("id"));String date=p.get("due");long saved=service.save(id,Long.parseLong(p.getOrDefault("version","0")),UUID.fromString(Objects.toString(p.get("token"),"")),p.get("kind"),p.get("title"),p.get("content"),date==null||date.isBlank()?null:LocalDate.parse(date),p.containsKey("pinned"),steps,p.containsKey("main_choice_present")?p.containsKey("mainGoal"):null,new NoteService.Options(p.containsKey("today_choice_present")?p.containsKey("todayPinned"):null,reminder(p,id),p.getOrDefault("due_time","").isBlank()?null:LocalTime.parse(p.get("due_time")),p.containsKey("due_time_present"),pl.nclean.crm.notifications.ReminderPlan.parse(p)));return ResponseEntity.ok(Map.of("saved",true,"id",saved));
    }catch(IllegalArgumentException|DateTimeException ex){return error(ex);}}
    @PostMapping("/notes/{id}/action") @ResponseBody ResponseEntity<?> action(@PathVariable long id,@RequestParam long version,@RequestParam String action,@RequestParam(required=false) UUID step,@RequestParam(defaultValue="false") boolean checked){try{boolean completed=service.action(id,version,action,step,checked);return ResponseEntity.ok(Map.of("saved",true,"motion",completed?"goal":action.equals("step")?"step":"updated"));}catch(IllegalArgumentException ex){return error(ex);}}
    private ReminderSchedule.Config reminder(Map<String,String> p,Long id){
        if(!p.containsKey("reminder_config_present"))return null;
        var frequency=p.getOrDefault("reminder_frequency","NONE");
        if(frequency.equals("NONE")){var old=service.reminderConfiguration(id);return new ReminderSchedule.Config("NONE",old.starts(),old.time(),old.weekdays(),old.intervalDays());}
        int days=0;for(int day=1;day<=7;day++)if(p.containsKey("reminder_weekday_"+day))days|=1<<(day-1);
        return new ReminderSchedule.Config(frequency,LocalDate.parse(p.get("reminder_starts")),LocalTime.parse(p.get("reminder_time")),frequency.equals("WEEKDAYS")?days:0,frequency.equals("INTERVAL")?Integer.parseInt(p.get("reminder_interval")):1);
    }
    private static ResponseEntity<?> error(Exception ex){if(ex instanceof pl.nclean.crm.persistence.RequestReplay.Conflict conflict)return ResponseEntity.status(409).header("X-Nclean-Replay",conflict.href()).body(Map.of("message",conflict.getMessage(),"href",conflict.href()));return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Sprawdź dane wpisu.":ex.getMessage()));}
    @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) String invalid(Model m){m.addAttribute("message","Sprawdź filtr, datę lub identyfikator wpisu.");return "records/error";}
}
