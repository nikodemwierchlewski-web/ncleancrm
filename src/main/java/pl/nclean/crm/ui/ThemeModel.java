package pl.nclean.crm.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Presentation experiment only; the classic interface remains the default. */
@ControllerAdvice
public class ThemeModel {
    private final boolean pixelTycoon;

    public ThemeModel(@Value("${crm.ui.pixel-tycoon:false}") boolean pixelTycoon) {
        this.pixelTycoon = pixelTycoon;
    }

    @ModelAttribute("pixelTycoon")
    public boolean pixelTycoon() {
        return pixelTycoon;
    }
}
