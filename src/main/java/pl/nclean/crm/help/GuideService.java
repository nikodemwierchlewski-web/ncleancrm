package pl.nclean.crm.help;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.nclean.crm.dashboard.DashboardService;
import pl.nclean.crm.work.DayReviewService;
import pl.nclean.crm.work.WorkService;

/** Contextual help reads the existing completeness rules. It never writes business data. */
@Service
public class GuideService {
    private final DayReviewService days;
    private final WorkService work;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public GuideService(DayReviewService days,WorkService work){this(days,work,Clock.system(ZoneId.of("Europe/Warsaw")));}
    GuideService(DayReviewService days,WorkService work,Clock clock){this.days=days;this.work=work;this.clock=clock;}
    public Map<String,Object> context(LocalDate requested,Long order){
        var now=LocalDateTime.now(clock);var today=now.toLocalDate();var day=requested==null?today:requested;
        if(order!=null){var actual=((java.sql.Date)work.order(order).get("day")).toLocalDate();if(requested!=null&&!actual.equals(requested))throw new ResponseStatusException(HttpStatus.NOT_FOUND);day=actual;}
        DashboardService.period(day,"day");var review=days.review(day);var result=new LinkedHashMap<String,Object>();
        result.put("date",day.toString());result.put("today",today.toString());result.put("total",review.orders().size());result.put("complete",review.complete());result.put("closed",review.closed());
        if(day.isAfter(today))return result;
        if(order!=null){final long id=order;var row=review.orders().stream().filter(o->((Number)o.get("id")).longValue()==id).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            var missing=(List<?>)row.get("missing");if(!missing.isEmpty())result.put("focused",Map.of("message","Do uzupełnienia: "+String.join(", ",missing.stream().map(Object::toString).toList())+".","url","/day-closeout?date="+day+"&order="+id));
        }
        boolean outcomesKnown=review.orders().stream().noneMatch(o->"PLANNED".equals(o.get("status")));
        if(!review.orders().isEmpty()&&!review.closed()&&(day.isBefore(today)||now.getHour()>=18||outcomesKnown))result.put("reminder",reminder(review));
        else if(day.equals(today))days.latestOpenBefore(today).ifPresent(previous->result.put("reminder",reminder(previous)));
        return result;
    }
    private Map<String,Object> reminder(DayReviewService.Review review){
        int missing=review.orders().size()-review.complete();String url="/day-closeout?date="+review.day();
        var first=review.orders().stream().filter(o->!Boolean.TRUE.equals(o.get("complete"))).findFirst();if(first.isPresent())url+="&order="+first.get().get("id");
        return Map.of("date",review.day().toString(),"missing",missing,"message",missing>0?"W dniu "+review.day()+" pozostało wizyt do uzupełnienia: "+missing+".":"Dzień "+review.day()+" jest uzupełniony. Możesz go zamknąć.","url",url);
    }
}
