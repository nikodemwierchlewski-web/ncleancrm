package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CalendarController {
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private final CalendarService service;
    private final pl.nclean.crm.records.CrmRecords records;
    public CalendarController(CalendarService service, pl.nclean.crm.records.CrmRecords records) { this.service = service; this.records = records; }

    @InitBinder("dayAreasForm")
    void bindDay(WebDataBinder binder) { binder.setAllowedFields("date", "districtIds"); }

    @GetMapping("/")
    String home() { return "redirect:/calendar"; }

    @GetMapping("/calendar")
    String calendar(@RequestParam(required = false) String month,
                    @RequestParam(required = false) String day,
                    @RequestParam(required = false) String saved, Model model) {
        var selected = parseMonth(month);
        LocalDate selectedDate = parseDay(day);
        if (selectedDate != null) selected = YearMonth.from(selectedDate);
        populate(model, selected, selectedDate, null, List.of());
        model.addAttribute("dayEditorOpen", selectedDate != null && !"1".equals(saved));
        model.addAttribute("notice", "1".equals(saved) ? "Zapisano zmiany." : null);
        return "calendar/month";
    }

    @PostMapping("/calendar/areas")
    String assignAreas(@Valid @ModelAttribute("dayAreasForm") DayAreasForm form, BindingResult binding,
                       Model model, HttpServletResponse response) {
        var errors = binding.getAllErrors().stream().map(error -> error.getDefaultMessage()).toList();
        if (errors.isEmpty()) {
            try {
                CalendarService.validateDate(form.getDate());
                service.assign(form.getDate(), form.getDistrictIds());
                return "redirect:/calendar?month=" + YearMonth.from(form.getDate()) + "&day=" + form.getDate() + "&saved=1";
            } catch (IllegalArgumentException ex) { errors = List.of(ex.getMessage()); }
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        LocalDate selectedDate = form.getDate();
        if (selectedDate == null || selectedDate.getYear() < 2000 || selectedDate.getYear() > 2100) {
            selectedDate = LocalDate.now(WARSAW);
        }
        var selectedIds = form.getDistrictIds() == null ? Set.<Long>of() : form.getDistrictIds().stream()
            .filter(Objects::nonNull).collect(Collectors.toSet());
        populate(model, YearMonth.from(selectedDate), selectedDate, selectedIds, errors);
        return "calendar/month";
    }

    @PostMapping(value="/calendar/areas", params="calendar", produces="application/json")
    @ResponseBody
    java.util.Map<String,Boolean> saveFromCalendar(@RequestParam LocalDate date,
        @RequestParam(required=false) List<Long> districtIds) {
        service.assign(date, districtIds == null ? List.of() : districtIds);
        return java.util.Map.of("saved",true);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    java.util.Map<String,String> invalidSelection(IllegalArgumentException ex) { return java.util.Map.of("message", ex.getMessage()); }

    // Keep existing V1 form submissions working during the interface transition.
    @PostMapping("/calendar/district")
    String assign(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                  @RequestParam(required = false) Long districtId) {
        if (date.getYear() < 2000 || date.getYear() > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data poza zakresem");
        }
        try { service.assign(date, districtId == null ? List.of() : List.of(districtId)); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany obszar"); }
        return "redirect:/calendar?month=" + YearMonth.from(date);
    }

    private void populate(Model model, YearMonth month, LocalDate selectedDate, Set<Long> selectedIds, List<String> errors) {
        var orders = records.monthBookings(month);
        var byDate = new java.util.LinkedHashMap<String, java.util.List<java.util.Map<String,Object>>>();
        var lines = new java.util.HashMap<Long, java.util.List<java.util.Map<String,Object>>>();
        for (var order : orders) {
            byDate.computeIfAbsent(order.get("day").toString(), k -> new java.util.ArrayList<>()).add(order);

        }
        for (var line : records.monthItems(month)) lines.computeIfAbsent(((Number)line.get("booking_id")).longValue(), k -> new java.util.ArrayList<>()).add(line);
        model.addAttribute("ordersByDay", byDate);
        model.addAttribute("orders", orders);
        model.addAttribute("lines", lines);
        model.addAttribute("categories", records.categories());
        model.addAttribute("services", records.services());
        model.addAttribute("token", java.util.UUID.randomUUID());
        model.addAttribute("month", month);
        model.addAttribute("defaultDay", month.equals(YearMonth.now(WARSAW)) ? LocalDate.now(WARSAW) : month.atDay(1));
        model.addAttribute("monthLabel", month.format(DateTimeFormatter.ofPattern("LLLL yyyy", POLISH)));
        model.addAttribute("days", service.days(month));
        model.addAttribute("districts", service.districts());
        model.addAttribute("allDistricts", service.allDistricts());
        model.addAttribute("offset", month.atDay(1).getDayOfWeek().getValue() - 1);
        model.addAttribute("today", LocalDate.now(WARSAW));
        model.addAttribute("previousMonth", month.equals(YearMonth.of(2000, 1)) ? null : month.minusMonths(1));
        model.addAttribute("nextMonth", month.equals(YearMonth.of(2100, 12)) ? null : month.plusMonths(1));
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dayEditorOpen", selectedDate != null);
        model.addAttribute("selectedDateLabel", selectedDate == null ? "" : selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", POLISH)));
        model.addAttribute("selectedAreaIds", selectedIds != null ? selectedIds
            : selectedDate == null ? Set.of() : service.assignedAreaIds(selectedDate));
        model.addAttribute("dayErrors", errors);
        model.addAttribute("polishLocale", POLISH);
    }

    private static YearMonth parseMonth(String value) {
        try {
            var month = value == null ? YearMonth.now(WARSAW) : YearMonth.parse(value);
            CalendarService.validateDate(month.atDay(1));
            return month;
        } catch (java.time.DateTimeException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny miesiąc");
        }
    }
    private static LocalDate parseDay(String value) {
        if (value == null) return null;
        try {
            var day = LocalDate.parse(value);
            CalendarService.validateDate(day);
            return day;
        } catch (java.time.DateTimeException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny dzień");
        }
    }
}
