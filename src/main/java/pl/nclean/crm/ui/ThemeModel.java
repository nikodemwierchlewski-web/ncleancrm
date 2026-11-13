package pl.nclean.crm.ui;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** One shared production appearance for public and owner screens. */
@ControllerAdvice
public class ThemeModel {
    @ModelAttribute("pixelTycoon")
    public boolean pixelTycoon() { return true; }
}
