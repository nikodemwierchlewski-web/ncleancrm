package pl.nclean.crm.work;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.notes.NoteService;
import pl.nclean.crm.shared.BusinessDates;

/** Composes existing read models; financial calculations stay in DashboardService. */
@Controller
public class TodayController {
    private final WorkService work;
    private final DayReviewService reviews;
    private final DashboardService dashboard;
    private final NoteService notes;
    private final pl.nclean.crm.costs.EquipmentService equipment;
    private final pl.nclean.crm.costs.DepreciationService depreciation;
    public TodayController(WorkService work,DayReviewService reviews,DashboardService dashboard,NoteService notes,pl.nclean.crm.costs.EquipmentService equipment,pl.nclean.crm.costs.DepreciationService depreciation){this.depreciation=depreciation;this.equipment=equipment;this.work=work;this.reviews=reviews;this.dashboard=dashboard;this.notes=notes;}
    @GetMapping("/today") String today(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) String range,@RequestParam(required=false) String finance,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) LocalDate periodDate,@RequestParam(name="depreciation",defaultValue="false") boolean includeDep,Model model){
        var day=date==null?BusinessDates.today():date;BusinessDates.period(day,"day");
        var review=reviews.review(day);var orders=work.orders(day);orders.forEach(o->o.put("navigation",Navigation.destination((String)o.get("address"))));
        model.addAttribute("date",day);model.addAttribute("today",BusinessDates.today());model.addAttribute("orders",orders);model.addAttribute("review",review);
        model.addAttribute("missingOrder",review.orders().stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))).findFirst().orElse(null));
        model.addAttribute("active",work.active());
        var focus=periodDate==null?day:periodDate;var filter=finance==null?(range==null?"month-to-date":range):finance;
        model.addAttribute("periodFocus",focus);model.addAttribute("financeFilter",filter);var trend=dashboard.trend(focus,filter,from,to);var economic=depreciation.report(trend.summary().period().start(),trend.summary().period().end());
        model.addAttribute("trend",trend);model.addAttribute("economicReport",economic);model.addAttribute("includeDep",includeDep);
        model.addAttribute("beforeDep",trend.summary().remaining());model.addAttribute("afterDep",economic.after(trend.summary().remaining()));
        model.addAttribute("displayedRemaining",includeDep?economic.after(trend.summary().remaining()):trend.summary().remaining());
        model.addAttribute("displayedPoints",includeDep?trend.points().stream().map(p->new DashboardService.Point(p.day(),p.revenue(),economic.afterOn(p.day(),p.profit()))).toList():trend.points());
        model.addAttribute("todayNotes",notes.todayPinned());model.addAttribute("todayNoteCount",notes.todayPinnedCount());
        model.addAttribute("equipmentAttention",equipment.attention(BusinessDates.today()));
        return "work/today";
    }
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    String invalid(Model model){model.addAttribute("message","Sprawdź daty i zakres podsumowania (maksymalnie 366 dni).");return "records/error";}
}
