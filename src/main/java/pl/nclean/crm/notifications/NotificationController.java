package pl.nclean.crm.notifications;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final EntityReminderService service;
    private final pl.nclean.crm.notes.ReminderService legacy;
    public NotificationController(EntityReminderService service,pl.nclean.crm.notes.ReminderService legacy){this.service=service;this.legacy=legacy;}
    @GetMapping("/reminders") ResponseEntity<?> plan(@RequestParam String sourceType,@RequestParam long sourceId){return noStore(service.plan(sourceType,sourceId));}
    @PostMapping("/reminders") ResponseEntity<?> save(@RequestParam String sourceType,@RequestParam long sourceId,@RequestParam Map<String,String> fields){var edit=ReminderPlan.parse(fields);if(edit==null)throw new IllegalArgumentException("Brakuje listy przypomnień.");return noStore(service.save(sourceType,sourceId,edit));}
    @GetMapping("/notifications") ResponseEntity<?> due(){return noStore(Map.of("notifications",service.due(),"legacyNotifications",legacy.pending(),"channel","IN_APP","zone","Europe/Warsaw"));}
    @PostMapping("/notifications/{id}") ResponseEntity<?> acknowledge(@PathVariable UUID id,@RequestParam long version,@RequestParam String action,@RequestParam(defaultValue="30") int minutes){return noStore(Map.of("saved",true,"claimed",service.acknowledge(id,version,action,minutes)));}
    private static ResponseEntity<?> noStore(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) ResponseEntity<?> invalid(Exception ex){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message",Objects.toString(ex.getMessage(),"Sprawdź przypomnienie.")));}
}
