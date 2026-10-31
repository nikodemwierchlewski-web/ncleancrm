package pl.nclean.crm.calendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AreaForm {
    @NotBlank(message = "Podaj nazwę obszaru.")
    @Size(max = 100, message = "Nazwa może mieć najwyżej 100 znaków.")
    private String name = "";
    @NotBlank(message = "Podaj kolor obszaru.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Podaj kolor HEX w formacie #A1B2C3.")
    private String color = "#208BE3";

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.strip(); }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color == null ? null : color.strip(); }
}
