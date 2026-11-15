package pl.nclean.crm.notes;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import pl.nclean.crm.shared.BusinessDates;

/** Calendar recurrence is local to Warsaw; instants resolve DST gaps/overlaps consistently. */
public final class ReminderSchedule {
    private ReminderSchedule() {}
    public record Occurrence(LocalDate day,Instant at) {}
    public record Config(String frequency,LocalDate starts,LocalTime time,int weekdays,int intervalDays) {
        public Config {
            if(frequency==null||!Set.of("NONE","ONCE","DAILY","WEEKDAYS","INTERVAL").contains(frequency))throw new IllegalArgumentException("Wybierz częstotliwość przypomnienia.");
            BusinessDates.period(starts,"day");
            if(time==null||time.getSecond()!=0||time.getNano()!=0||weekdays<0||weekdays>127||intervalDays<1||intervalDays>365||frequency.equals("WEEKDAYS")&&weekdays==0)throw new IllegalArgumentException("Sprawdź godzinę, dni tygodnia i odstęp od 1 do 365 dni.");
        }
        public Optional<Occurrence> latest(Instant now) {
            if(frequency.equals("NONE"))return Optional.empty();
            var limit=now.atZone(BusinessDates.WARSAW).toLocalDate();
            if(limit.isBefore(starts))return Optional.empty();
            var candidate=switch(frequency){
                case "ONCE" -> starts;
                case "INTERVAL" -> starts.plusDays(ChronoUnit.DAYS.between(starts,limit)/intervalDays*intervalDays);
                default -> limit;
            };
            if(at(candidate).isAfter(now))candidate=candidate.minusDays(frequency.equals("INTERVAL")?intervalDays:1);
            if(frequency.equals("ONCE")&&!candidate.equals(starts))return Optional.empty();
            if(frequency.equals("WEEKDAYS"))for(int i=0;i<7&&(weekdays&(1<<(candidate.getDayOfWeek().getValue()-1)))==0;i++)candidate=candidate.minusDays(1);
            return candidate.isBefore(starts)?Optional.empty():Optional.of(new Occurrence(candidate,at(candidate)));
        }
        public boolean onWeekday(int day){return (weekdays&(1<<(day-1)))!=0;}
        private Instant at(LocalDate day){return day.atTime(time).atZone(BusinessDates.WARSAW).toInstant();}
    }
}
