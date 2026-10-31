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
    String settings(@RequestParam(required = false) Long edit, @RequestParam(required = false) String saved, Model model) {
        var form = new AreaForm();
        if (edit != null) {
            var area = service.area(edit);
            form.setName(area.name()); form.setColor(area.color());
        }
        populate(model, form, edit, List.of());
        model.addAttribute("notice", "1".equals(saved) ? "Zapisano ustawienia obszarów." : null);
        return "settings/areas";
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
        model.addAttribute("areaForm", form);
        model.addAttribute("editingAreaId", editingId);
        model.addAttribute("areaErrors", errors);
        model.addAttribute("polishLocale", Locale.forLanguageTag("pl-PL"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono obszaru pracy.");
    }
}
