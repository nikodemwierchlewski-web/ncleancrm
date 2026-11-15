package pl.nclean.crm.work;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.notes.NoteService;
import pl.nclean.crm.notes.ReminderService;
import pl.nclean.crm.shared.BusinessDates;

/** Composes existing read models; financial calculations stay in DashboardService. */
@Controller
public class TodayController {
    private final WorkService work;
    private final DayReviewService reviews;
    private final DashboardService dashboard;
    private final NoteService notes;
    private final ReminderService reminders;
    public TodayController(WorkService work,DayReviewService reviews,DashboardService dashboard,NoteService notes,ReminderService reminders){this.work=work;this.reviews=reviews;this.dashboard=dashboard;this.notes=notes;this.reminders=reminders;}
    @GetMapping("/today") String today(@RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="month") String range,@RequestParam(required=false) LocalDate periodDate,Model model){
        var day=date==null?BusinessDates.today():date;BusinessDates.period(day,"day");
        var review=reviews.review(day);var orders=work.orders(day);orders.forEach(o->o.put("navigation",Navigation.destination((String)o.get("address"))));
        model.addAttribute("date",day);model.addAttribute("today",BusinessDates.today());model.addAttribute("orders",orders);model.addAttribute("review",review);
        model.addAttribute("missingOrder",review.orders().stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))).findFirst().orElse(null));
        model.addAttribute("active",work.active());
        model.addAttribute("dueOrders",orders.stream().filter(o->"DONE".equals(o.get("status"))&&((java.math.BigDecimal)(o.get("final_total")==null?o.get("total"):o.get("final_total"))).compareTo((java.math.BigDecimal)o.get("paid"))>0).toList());
        var focus=periodDate==null?day:periodDate;model.addAttribute("periodFocus",focus);model.addAttribute("trend",dashboard.trend(focus,range));
        var pending=reminders.pending();var main=notes.mainGoal();var covered=pending.stream().map(ReminderService.Pending::id).collect(java.util.stream.Collectors.toSet());
        model.addAttribute("todayNotes",notes.todayPinned(covered));model.addAttribute("todayNoteCount",notes.todayPinnedCount(covered));
        model.addAttribute("reminders",pending.stream().filter(r->main==null||r.id()!=main.id()).toList());model.addAttribute("reminderCount",pending.size());
        model.addAttribute("mainGoalReminder",pending.stream().filter(r->main!=null&&r.id()==main.id()).findFirst().orElse(null));
        return "work/today";
    }
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    String invalid(Model model){model.addAttribute("message","Sprawdź datę i wybierz tydzień lub miesiąc.");return "records/error";}
}
