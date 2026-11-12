package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {
    private final pl.nclean.crm.work.DayReviewService review;
    private final pl.nclean.crm.notes.NoteService notes;
    private final DashboardService service;
    private final pl.nclean.crm.work.WorkService work;
    private final pl.nclean.crm.fuel.FuelJournalService fuel;
    public DashboardController(DashboardService service,pl.nclean.crm.fuel.FuelJournalService fuel,pl.nclean.crm.work.WorkService work,pl.nclean.crm.work.DayReviewService review,pl.nclean.crm.notes.NoteService notes){this.notes=notes;this.review=review;this.service=service;this.fuel=fuel;this.work=work;}
    @GetMapping("/dashboard") String dashboard(@RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="day") String range,@RequestParam(defaultValue="actual") String mode,Model m){
        m.addAttribute("pinnedNotes",notes.pinned());m.addAttribute("pinnedNoteCount",notes.pinnedCount());var selected=date==null?DashboardService.today():date;var s=service.summary(selected,range,mode);if(range.equals("day"))m.addAttribute("dayReview",review.review(selected));m.addAttribute("s",s);m.addAttribute("unconfirmedPrices",s.orders().stream().filter(o->"DONE".equals(o.get("status"))&&o.get("final_total")==null).count());
        var performed=mode.equals("actual")?s:service.summary(selected,range,"actual");m.addAttribute("finance",work.finance(s.period().start(),s.period().end(),performed.revenue(),performed.remaining(),performed.complete()));m.addAttribute("analytics",work.analytics(s.period().start(),s.period().end()));
        long length=java.time.temporal.ChronoUnit.DAYS.between(s.period().start(),s.period().end())+1;var previousStart=s.period().start().minusDays(length);var previousEnd=s.period().start().minusDays(1);m.addAttribute("previousStart",previousStart);m.addAttribute("previousEnd",previousEnd);
        java.math.BigDecimal previousRevenue=work.revenue(previousStart,previousEnd,mode);boolean previousComplete=work.previousComplete(previousStart,previousEnd);m.addAttribute("previousRevenue",previousRevenue);m.addAttribute("previousComplete",previousComplete);m.addAttribute("revenueChange",previousRevenue.signum()==0?null:s.revenue().subtract(previousRevenue).multiply(new java.math.BigDecimal("100")).divide(previousRevenue,2,java.math.RoundingMode.HALF_UP));m.addAttribute("date",selected);m.addAttribute("today",DashboardService.today());m.addAttribute("legacyFuel",fuel.legacyFuel(selected));m.addAttribute("journal",fuel.daily(selected));m.addAttribute("previousReading",fuel.previous(selected));m.addAttribute("nextReading",fuel.next(selected));m.addAttribute("fuelUsage",fuel.usage(selected,selected).get(selected));m.addAttribute("fuelTotals",fuel.totals(s.period().start(),s.period().end()));m.addAttribute("fuelParameters",fuel.parameters(selected));return "dashboard/index";
    }
    private String costs(YearMonth month,LocalDate day,Model m){
        if(month.getYear()<2000||month.getYear()>2100)throw new IllegalArgumentException("Wybierz miesiąc w latach 2000–2100.");
        DashboardService.period(day,"day");
        var labels=new LinkedHashMap<String,String>();labels.put("MARKETING","Marketing");labels.put("LEASE","Leasing");labels.put("INSURANCE","Ubezpieczenie");labels.put("ACCOUNTING","Księgowość");labels.put("PHONE","Telefon");labels.put("SOFTWARE","Oprogramowanie");labels.put("OTHER","Inne");m.addAttribute("categories",labels);
        int slots=Math.min(50,Math.max(3,service.monthCosts(month).size()+1));
        if(m.getAttribute("postedMonth") instanceof Map<?,?> posted)for(int i=0;i<50;i++)if(posted.containsKey("name_"+i))slots=Math.max(slots,i+1);
        m.addAttribute("slots",slots);m.addAttribute("month",month);m.addAttribute("day",day);m.addAttribute("costs",service.monthCosts(month));m.addAttribute("version",service.monthVersion(month));m.addAttribute("fuel",service.fuel(day));m.addAttribute("defaults",service.fuelDefaults());m.addAttribute("locked",month.isBefore(YearMonth.from(DashboardService.today())));return "dashboard/costs";
    }
    @GetMapping("/settings/costs") String costsPage(@RequestParam(required=false) YearMonth month,@RequestParam(required=false) LocalDate day,Model m){return costs(month==null?YearMonth.from(DashboardService.today()):month,day==null?DashboardService.today():day,m);}
    @PostMapping("/settings/costs/month") String saveMonth(@RequestParam Map<String,String> p,Model m,HttpServletResponse response){
        var month=YearMonth.parse(p.get("month"));var day=LocalDate.parse(p.get("day"));
        try{
            var costs=new ArrayList<DashboardService.Cost>();
            for(int i=0;i<50;i++){String name=p.get("name_"+i),amount=p.get("amount_"+i);if((name==null||name.isBlank())&&(amount==null||amount.isBlank()))continue;costs.add(new DashboardService.Cost(name,p.get("category_"+i),DashboardService.number(amount,2,new BigDecimal("9999999999.99"),false)));}
            service.saveMonth(month,Long.parseLong(p.get("version")),costs);
            return "redirect:/settings/costs?month="+month+"&day="+day+"&saved=month";
        }catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("error",ex.getMessage());m.addAttribute("postedMonth",p);return costs(month,day,m);}
    }
    @PostMapping("/settings/costs/fuel") String saveFuel(@RequestParam Map<String,String> p,Model m,HttpServletResponse response){
        var day=LocalDate.parse(p.get("day"));var month=YearMonth.from(day);
        try{
            service.saveFuel(day,Long.parseLong(p.get("version")),DashboardService.number(p.get("consumption"),3,new BigDecimal("99999.999"),false),DashboardService.number(p.get("litre_price"),3,new BigDecimal("99999.999"),false),DashboardService.number(p.get("planned_km"),3,new BigDecimal("9999999.999"),true),DashboardService.number(p.get("actual_km"),3,new BigDecimal("9999999.999"),true),DashboardService.number(p.get("actual_cost"),2,new BigDecimal("9999999999.99"),true));
            return "redirect:/settings/costs?month="+month+"&day="+day+"&saved=fuel";
        }catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("error",ex.getMessage());m.addAttribute("postedFuel",p);return costs(month,day,m);}
    }
    @ExceptionHandler({IllegalArgumentException.class,DateTimeParseException.class})
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    String invalid(Model m){m.addAttribute("message","Nieprawidłowy okres lub parametry. Wróć i popraw dane.");return "records/error";}
}
