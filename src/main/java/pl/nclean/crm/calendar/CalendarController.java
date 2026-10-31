package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CalendarController {
    private final CalendarService service;
    public CalendarController(CalendarService service) { this.service = service; }

    @GetMapping("/")
    String home() { return "redirect:/calendar"; }

    @GetMapping("/calendar")
    String calendar(@RequestParam(required = false) String month, Model model) {
        YearMonth selected;
        try { selected = month == null ? YearMonth.now(java.time.ZoneId.of("Europe/Warsaw")) : YearMonth.parse(month); }
        catch (java.time.DateTimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny miesiąc"); }
        if (selected.getYear() < 2000 || selected.getYear() > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Miesiąc poza zakresem");
        }
        model.addAttribute("month", selected);
        model.addAttribute("days", service.days(selected));
        model.addAttribute("districts", service.districts());
        model.addAttribute("offset", selected.atDay(1).getDayOfWeek().getValue() - 1);
        return "calendar/month";
    }

    @PostMapping("/calendar/district")
    String assign(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                  @RequestParam(required = false) Long districtId) {
        if (date.getYear() < 2000 || date.getYear() > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data poza zakresem");
        }
        try { service.assign(date, districtId); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany obszar"); }
        return "redirect:/calendar?month=" + YearMonth.from(date);
    }
}
