package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
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

    public record District(long id, String name, String color, boolean active) {}
    public record Day(LocalDate date, List<District> areas) {
        public Day { areas = List.copyOf(areas); }
    }

    public List<District> districts() {
        return districts.findByActiveTrueOrderByNameAsc().stream()
            .map(this::view).toList();
    }

    public List<District> allDistricts() {
        return districts.findAllByOrderByActiveDescNameAsc().stream().map(this::view).toList();
    }

    public District area(long id) { return view(requiredArea(id)); }

    public List<Day> days(YearMonth month) {
        validateDate(month.atDay(1));
        var byDate = assignments.findByDayGreaterThanEqualAndDayLessThan(month.atDay(1), month.plusMonths(1).atDay(1))
            .stream().collect(Collectors.groupingBy(DayDistrictEntity::getDay,
                Collectors.mapping(a -> view(a.getDistrict()), Collectors.toList())));
        return month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
            .map(date -> new Day(date, byDate.getOrDefault(date, List.of()))).toList();
    }

    public Set<Long> assignedAreaIds(LocalDate date) {
        validateDate(date);
        return assignments.findByDay(date).stream().map(a -> a.getDistrict().getId()).collect(Collectors.toSet());
    }

    @Transactional
    public void assign(LocalDate date, List<Long> districtIds) {
        validateDate(date);
        var selected = new LinkedHashSet<>(districtIds == null ? List.of() : districtIds);
        if (selected.size() > 100 || selected.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Wybierz poprawne obszary pracy.");
        }
        var existing = assignments.findByDay(date);
        var existingIds = existing.stream().map(a -> a.getDistrict().getId()).collect(Collectors.toSet());
        // Validate every selection before altering anything. Inactive historical
        // assignments may remain selected or be removed, but cannot be newly added.
        var chosenAreas = selected.stream().map(id -> {
            var district = districts.findById(id).orElseThrow(() -> new IllegalArgumentException("Nieznany obszar pracy."));
            if (!district.isActive() && !existingIds.contains(id)) {
                throw new IllegalArgumentException("Nie można przypisać nieaktywnego obszaru do nowego dnia.");
            }
            return district;
        }).toList();
        assignments.deleteAll(existing.stream().filter(a -> !selected.contains(a.getDistrict().getId())).toList());
        assignments.saveAll(chosenAreas.stream().filter(d -> !existingIds.contains(d.getId()))
            .map(d -> new DayDistrictEntity(date, d)).toList());
        assignments.flush();
    }

    @Transactional
    public long createArea(AreaForm form) {
        String name = checkedName(form.getName()), color = checkedColor(form.getColor());
        if (districts.existsByNameIgnoreCase(name)) throw duplicateName();
        return districts.saveAndFlush(new DistrictEntity(name, color)).getId();
    }

    @Transactional
    public void updateArea(long id, AreaForm form) {
        var area = requiredArea(id);
        String name = checkedName(form.getName()), color = checkedColor(form.getColor());
        if (districts.existsByNameIgnoreCaseAndIdNot(name, id)) throw duplicateName();
        area.update(name, color);
        districts.flush();
    }

    @Transactional
    public void setAreaActive(long id, boolean active) {
        requiredArea(id).setActive(active);
        districts.flush();
    }

    public static void validateDate(LocalDate date) {
        if (date == null || date.getYear() < 2000 || date.getYear() > 2100) {
            throw new IllegalArgumentException("Wybierz dzień z lat 2000–2100.");
        }
    }

    private DistrictEntity requiredArea(long id) {
        return districts.findById(id).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono obszaru pracy."));
    }
    private District view(DistrictEntity district) {
        return new District(district.getId(), district.getName(), district.getColor(), district.isActive());
    }
    private static String checkedName(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 100) {
            throw new IllegalArgumentException("Podaj nazwę obszaru o długości od 1 do 100 znaków.");
        }
        return value.strip();
    }
    private static String checkedColor(String value) {
        if (value == null || !value.strip().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Podaj kolor HEX w formacie #A1B2C3.");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
    private static IllegalArgumentException duplicateName() {
        return new IllegalArgumentException("Obszar o takiej nazwie już istnieje.");
    }
}
