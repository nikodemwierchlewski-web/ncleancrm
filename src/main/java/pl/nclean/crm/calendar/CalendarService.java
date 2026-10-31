package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CalendarService {
    private final DistrictRepository districts;
    private final DayDistrictRepository assignments;
    public CalendarService(DistrictRepository districts, DayDistrictRepository assignments) {
        this.districts = districts;
        this.assignments = assignments;
    }

    public record District(long id, String name, String color) {}
    public record Day(LocalDate date, String district, String color) {}

    public List<District> districts() {
        return districts.findByActiveTrueOrderByNameAsc().stream()
            .map(d -> new District(d.getId(), d.getName(), d.getColor())).toList();
    }

    public List<Day> days(YearMonth month) {
        var markedDays = assignments.findByDayGreaterThanEqualAndDayLessThan(month.atDay(1), month.plusMonths(1).atDay(1))
            .stream().map(a -> new Day(a.getDay(), a.getDistrict().getName(), a.getDistrict().getColor())).toList();
        var byDate = markedDays.stream().collect(java.util.stream.Collectors.toMap(Day::date, d -> d));
        return month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
            .map(date -> byDate.getOrDefault(date, new Day(date, "Bez oznaczenia", "#eeeeee"))).toList();
    }

    @Transactional
    public void assign(LocalDate date, Long districtId) {
        if (districtId == null) {
            assignments.deleteById(date);
            return;
        }
        var district = districts.findByIdAndActiveTrue(districtId)
            .orElseThrow(() -> new IllegalArgumentException("Nieznany obszar"));
        assignments.save(new DayDistrictEntity(date, district));
    }
}
