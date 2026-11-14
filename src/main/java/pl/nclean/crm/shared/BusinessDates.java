package pl.nclean.crm.shared;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Calendar conventions shared by features; no database or dashboard dependency. */
public final class BusinessDates {
    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private BusinessDates() {}

    public static LocalDate today() {
        return LocalDate.now(WARSAW);
    }

    public record Period(LocalDate start, LocalDate end, String range) {
        public LocalDate previous() {
            return switch (range) {
                case "week" -> start.minusWeeks(1);
                case "month" -> start.minusMonths(1);
                default -> start.minusDays(1);
            };
        }

        public LocalDate next() {
            return switch (range) {
                case "week" -> start.plusWeeks(1);
                case "month" -> start.plusMonths(1);
                default -> start.plusDays(1);
            };
        }
    }

    public static Period period(LocalDate date, String range) {
        if (date == null || date.getYear() < 2000 || date.getYear() > 2100) {
            throw new IllegalArgumentException("Wybierz datę w latach 2000–2100.");
        }
        return switch (range) {
            case "day" -> new Period(date, date, "day");
            case "week" -> new Period(
                    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), "week");
            case "month" -> new Period(date.withDayOfMonth(1), YearMonth.from(date).atEndOfMonth(), "month");
            default -> throw new IllegalArgumentException("Wybierz dzień, tydzień lub miesiąc.");
        };
    }
}
