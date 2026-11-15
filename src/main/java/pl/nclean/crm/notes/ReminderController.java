package pl.nclean.crm.notes;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReminderController {
    private final ReminderService reminders;
    public ReminderController(ReminderService reminders){this.reminders=reminders;}
    @GetMapping("/notes/reminders") public Map<String,Object> pending(){return Map.of("reminders",reminders.pending());}
    @PostMapping("/notes/reminders/{id}") public Map<String,Object> action(@PathVariable long id,@RequestParam long revision,@RequestParam LocalDate occurrence,@RequestParam String action,@RequestParam(defaultValue="60") int minutes){
        boolean changed=reminders.action(id,revision,occurrence,action,minutes);return Map.of("saved",true,"claimed",changed,"reminders",reminders.pending());
    }
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(Exception error){return Map.of("message",error.getMessage()==null?"Sprawdź przypomnienie.":error.getMessage());}
}
