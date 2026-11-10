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
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final DistrictRepository districts;
    private final DayDistrictRepository assignments;
    public CalendarService(DistrictRepository districts, DayDistrictRepository assignments, org.springframework.jdbc.core.JdbcTemplate db) {
        this.db = db;
        this.districts = districts;
        this.assignments = assignments;
    }

    public record District(long id, String name, String color, boolean active) {}
    public record Day(LocalDate date, List<District> areas, boolean overridden) {
        public Day(LocalDate date,List<District> areas) { this(date,areas,false); }
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
        var exceptions = new java.util.HashSet<>(db.queryForList("SELECT day FROM day_area_override WHERE day>=? AND day<?", java.sql.Date.class,month.atDay(1),month.plusMonths(1).atDay(1)).stream().map(java.sql.Date::toLocalDate).toList());
        var rules = db.queryForList("SELECT p.valid_from,w.weekday,d.* FROM weekly_area_plan p LEFT JOIN weekly_area w ON w.plan_id=p.id LEFT JOIN district d ON d.id=w.district_id WHERE p.valid_from<? ORDER BY p.valid_from",month.plusMonths(1).atDay(1));
        var versions = new java.util.TreeMap<LocalDate,java.util.Map<Integer,List<District>>>();
        for(var rule:rules){
            var weekdays=versions.computeIfAbsent(((java.sql.Date)rule.get("valid_from")).toLocalDate(),k->new java.util.HashMap<>());
            if(rule.get("id")!=null)weekdays.computeIfAbsent(((Number)rule.get("weekday")).intValue(),k->new java.util.ArrayList<>()).add(new District(((Number)rule.get("id")).longValue(),(String)rule.get("name"),(String)rule.get("color"),(Boolean)rule.get("active")));
        }
        return month.atDay(1).datesUntil(month.plusMonths(1).atDay(1)).map(date->{
            boolean overridden=exceptions.contains(date);
            var version=versions.floorEntry(date);
            var inherited=version==null?List.<District>of():version.getValue().getOrDefault(date.getDayOfWeek().getValue(),List.of());
            return new Day(date,overridden?byDate.getOrDefault(date,List.of()):inherited,overridden);
        }).toList();
    }

    public Set<Long> assignedAreaIds(LocalDate date) {
        validateDate(date);
        return days(YearMonth.from(date)).stream().filter(day->day.date().equals(date)).findFirst().orElseThrow().areas().stream().map(District::id).collect(Collectors.toSet());
    }

    public List<java.sql.Date> weeklyVersions(){return db.queryForList("SELECT valid_from FROM weekly_area_plan ORDER BY valid_from DESC",java.sql.Date.class);}
    public record Weekday(int number,String label,Set<Long> selected) {}
    public List<Weekday> weeklyDays(LocalDate date) {
        validateDate(date);
        var rows=db.queryForList("SELECT weekday,district_id FROM weekly_area WHERE plan_id=(SELECT id FROM weekly_area_plan WHERE valid_from<=? ORDER BY valid_from DESC LIMIT 1)",date);
        var selected=new java.util.HashMap<Integer,Set<Long>>();
        for(var row:rows)selected.computeIfAbsent(((Number)row.get("weekday")).intValue(),k->new LinkedHashSet<>()).add(((Number)row.get("district_id")).longValue());
        var labels=List.of("Poniedziałek","Wtorek","Środa","Czwartek","Piątek","Sobota","Niedziela");
        return java.util.stream.IntStream.rangeClosed(1,7).mapToObj(i->new Weekday(i,labels.get(i-1),selected.getOrDefault(i,Set.of()))).toList();
    }

    @Transactional
    public void saveWeekly(LocalDate from,java.util.Map<Integer,List<Long>> choices) {
        validateDate(from);
        if(from.isBefore(LocalDate.now(java.time.ZoneId.of("Europe/Warsaw"))))throw new IllegalArgumentException("Plan tygodniowy może obowiązywać od dziś lub daty przyszłej. Historia pozostaje bez zmian.");
        db.execute("SELECT pg_advisory_xact_lock(7194201)");
        var previous=weeklyDays(from).stream().collect(Collectors.toMap(Weekday::number,Weekday::selected));
        if(choices.keySet().stream().anyMatch(i->i==null||i<1||i>7))throw new IllegalArgumentException("Niepoprawny dzień tygodnia.");
        for(var entry:choices.entrySet()){
            var ids=new LinkedHashSet<>(entry.getValue());
            if(ids.size()>100)throw new IllegalArgumentException("Za dużo obszarów.");
            for(var id:ids){
                if(id==null||id<=0)throw new IllegalArgumentException("Niepoprawny obszar.");
                var area=requiredArea(id);
                if(!area.isActive()&&!previous.get(entry.getKey()).contains(id))throw new IllegalArgumentException("Nie można dodać nieaktywnego obszaru do planu.");
            }
        }
        var found=db.queryForList("SELECT id FROM weekly_area_plan WHERE valid_from=?",Long.class,from);
        long id=found.isEmpty()?db.queryForObject("INSERT INTO weekly_area_plan(valid_from) VALUES (?) RETURNING id",Long.class,from):found.get(0);
        db.update("DELETE FROM weekly_area WHERE plan_id=?",id);
        for(var entry:choices.entrySet())for(var area:new LinkedHashSet<>(entry.getValue()))db.update("INSERT INTO weekly_area(plan_id,weekday,district_id) VALUES (?,?,?)",id,entry.getKey(),area);
    }

    @Transactional
    public void restoreWeekly(LocalDate date) {
        validateDate(date);
        assignments.deleteAll(assignments.findByDay(date));assignments.flush();
        db.update("DELETE FROM day_area_override WHERE day=?",date);
    }

    @Transactional
    public void assign(LocalDate date, List<Long> districtIds) {
        validateDate(date);
        var selected = new LinkedHashSet<>(districtIds == null ? List.of() : districtIds);
        if (selected.size() > 100 || selected.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Wybierz poprawne obszary pracy.");
        }
        var existing = assignments.findByDay(date);
        var actualIds = existing.stream().map(a -> a.getDistrict().getId()).collect(Collectors.toSet());
        var existingIds = assignedAreaIds(date);
        // Validate every selection before altering anything. Inactive historical
        // assignments may remain selected or be removed, but cannot be newly added.
        var chosenAreas = selected.stream().map(id -> {
            var district = districts.findById(id).orElseThrow(() -> new IllegalArgumentException("Nieznany obszar pracy."));
            if (!district.isActive() && !existingIds.contains(id)) {
                throw new IllegalArgumentException("Nie można przypisać nieaktywnego obszaru do nowego dnia.");
            }
            return district;
        }).toList();
        db.update("INSERT INTO day_area_override(day) VALUES (?) ON CONFLICT DO NOTHING",date);
        assignments.deleteAll(existing.stream().filter(a -> !selected.contains(a.getDistrict().getId())).toList());
        assignments.saveAll(chosenAreas.stream().filter(d -> !actualIds.contains(d.getId()))
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
