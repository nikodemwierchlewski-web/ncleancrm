package pl.nclean.crm.calendar;

import java.util.List;
import java.util.Locale;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/areas")
public class AreaSettingsController {
    private final CalendarService service;
    public AreaSettingsController(CalendarService service) { this.service = service; }

    @InitBinder("areaForm")
    void bindArea(WebDataBinder binder) { binder.setAllowedFields("name", "color"); }

    @GetMapping
    String settings(@RequestParam(required = false) Long edit, @RequestParam(required = false) String saved,@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate validFrom, Model model) {
        var form = new AreaForm();
        if (edit != null) {
            var area = service.area(edit);
            form.setName(area.name()); form.setColor(area.color());
        }
        populate(model, form, edit, List.of());
        if(validFrom!=null){CalendarService.validateDate(validFrom);model.addAttribute("weeklyStart",validFrom);model.addAttribute("weeklyDays",service.weeklyDays(validFrom));}
        model.addAttribute("notice", "1".equals(saved) ? "Zapisano ustawienia obszarów." : null);
        return "settings/areas";
    }

    @PostMapping("/weekly")
    String weekly(@RequestParam org.springframework.util.MultiValueMap<String,String> values,Model model,HttpServletResponse response) {
        try {
            var value=values.getFirst("validFrom");if(value==null||value.isBlank())throw new IllegalArgumentException("Wybierz datę rozpoczęcia planu.");
            var date=java.time.LocalDate.parse(value);
            var choices=new java.util.LinkedHashMap<Integer,List<Long>>();
            for(int i=1;i<=7;i++)choices.put(i,values.getOrDefault("weekday_"+i,List.of()).stream().map(Long::valueOf).toList());
            service.saveWeekly(date,choices);
            return "redirect:/settings/areas?saved=1&validFrom="+date;
        } catch(IllegalArgumentException|java.time.DateTimeException ex) {
            response.setStatus(400);populate(model,new AreaForm(),null,List.of());
            var rows=new java.util.ArrayList<CalendarService.Weekday>();for(var day:service.weeklyDays(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Warsaw")))){
                var selected=new java.util.LinkedHashSet<Long>();for(var value:values.getOrDefault("weekday_"+day.number(),List.of()))try{selected.add(Long.valueOf(value));}catch(NumberFormatException ignored){}
                rows.add(new CalendarService.Weekday(day.number(),day.label(),selected));
            }model.addAttribute("weeklyDays",rows);
            model.addAttribute("weeklyErrors",List.of(ex.getMessage()==null?"Sprawdź plan i datę.":ex.getMessage()));
            return "settings/areas";
        }
    }

    @PostMapping
    String create(@Valid @ModelAttribute("areaForm") AreaForm form, BindingResult binding, Model model,
                  HttpServletResponse response) {
        return save(null, form, binding, model, response);
    }

    @PostMapping("/{id}")
    String update(@PathVariable long id, @Valid @ModelAttribute("areaForm") AreaForm form, BindingResult binding,
                  Model model, HttpServletResponse response) {
        return save(id, form, binding, model, response);
    }

    @PostMapping("/{id}/active")
    String active(@PathVariable long id, @RequestParam boolean active) {
        service.setAreaActive(id, active);
        return "redirect:/settings/areas?saved=1";
    }

    private String save(Long id, AreaForm form, BindingResult binding, Model model, HttpServletResponse response) {
        var errors = binding.getAllErrors().stream().map(error -> error.getDefaultMessage()).toList();
        if (errors.isEmpty()) {
            try {
                if (id == null) service.createArea(form); else service.updateArea(id, form);
                return "redirect:/settings/areas?saved=1";
            } catch (IllegalArgumentException ex) {
                errors = List.of(ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors = List.of("Obszar o takiej nazwie już istnieje.");
            }
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        populate(model, form, id, errors);
        return "settings/areas";
    }

    private void populate(Model model, AreaForm form, Long editingId, List<String> errors) {
        model.addAttribute("areas", service.allDistricts());
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Warsaw"));
        model.addAttribute("weeklyMin",today);model.addAttribute("weeklyVersions",service.weeklyVersions());model.addAttribute("weeklyStart",today);model.addAttribute("weeklyDays",service.weeklyDays(today));model.addAttribute("weeklyErrors",List.of());
        model.addAttribute("areaForm", form);
        model.addAttribute("editingAreaId", editingId);
        model.addAttribute("areaErrors", errors);
        model.addAttribute("polishLocale", Locale.forLanguageTag("pl-PL"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> invalidPlan(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(ex.getMessage()); }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() { return "error/4xx"; }
}
