package pl.nclean.crm.tycoon;

import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import pl.nclean.crm.notes.NoteService;
import pl.nclean.crm.work.DayReviewService;
import pl.nclean.crm.dashboard.DashboardService;

@RestController
public class TycoonController {
    private final AchievementService awards;private final NoteService notes;private final DayReviewService days;
    public TycoonController(AchievementService awards,NoteService notes,DayReviewService days){this.awards=awards;this.notes=notes;this.days=days;}
    @GetMapping("/tycoon/progress") public Map<String,Object> progress(@RequestParam(required=false) LocalDate date){
        var day=date==null?DashboardService.today():date;DashboardService.period(day,"day");var review=days.review(day);
        var result=new LinkedHashMap<String,Object>();result.put("date",day.toString());result.put("complete",review.complete());result.put("total",review.orders().size());result.put("closed",review.closed());result.put("performed",awards.performedCount());result.put("badges",awards.badges());
        var goal=notes.pinnedGoal();if(goal!=null)result.put("goal",Map.of("id",goal.id(),"title",goal.title(),"done",goal.doneSteps(),"total",goal.steps().size()));return result;
    }
    @PostMapping("/tycoon/achievements/{code}/ack") public Map<String,Boolean> acknowledge(@PathVariable String code){return Map.of("claimed",awards.acknowledge(code));}
    @ExceptionHandler({IllegalArgumentException.class,DateTimeException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(){return Map.of("message","Sprawdź datę lub odznakę.");}
}
