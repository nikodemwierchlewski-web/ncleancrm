package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public class DayAreasForm {
    @NotNull(message = "Wybierz poprawny dzień.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    @Size(max = 100, message = "Możesz przypisać najwyżej 100 obszarów do dnia.")
    private List<@NotNull @Positive Long> districtIds = new ArrayList<>();

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<Long> getDistrictIds() { return districtIds; }
    public void setDistrictIds(List<Long> districtIds) { this.districtIds = districtIds; }
}
