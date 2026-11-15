package pl.nclean.crm.notes;

import java.time.*;
import org.junit.jupiter.api.Test;
import pl.nclean.crm.shared.BusinessDates;
import static org.junit.jupiter.api.Assertions.*;

class ReminderScheduleTest {
    private static Instant at(String value){return LocalDateTime.parse(value).atZone(BusinessDates.WARSAW).toInstant();}
    @Test void dailyBeforeTimeUsesPreviousOccurrenceAndNeverGeneratesABacklog(){
        var s=new ReminderSchedule.Config("DAILY",LocalDate.parse("2026-11-01"),LocalTime.of(9,0),0,1);
        assertEquals(LocalDate.parse("2026-11-14"),s.latest(at("2026-11-15T08:59")).orElseThrow().day());
        assertEquals(LocalDate.parse("2026-11-15"),s.latest(at("2026-11-15T09:00")).orElseThrow().day());
        assertEquals(LocalDate.parse("2027-02-10"),s.latest(at("2027-02-10T12:00")).orElseThrow().day());
        assertTrue(s.latest(at("2026-11-01T08:59")).isEmpty());
    }
    @Test void weekdaysAndIntervalAreAnchoredToTheConfiguredStart(){
        var weekdays=new ReminderSchedule.Config("WEEKDAYS",LocalDate.parse("2026-11-01"),LocalTime.of(9,0),1|16,1);
        assertEquals(LocalDate.parse("2026-11-13"),weekdays.latest(at("2026-11-16T08:00")).orElseThrow().day());
        assertEquals(LocalDate.parse("2026-11-16"),weekdays.latest(at("2026-11-16T09:00")).orElseThrow().day());
        var every=new ReminderSchedule.Config("INTERVAL",LocalDate.parse("2026-11-01"),LocalTime.of(9,0),0,3);
        assertEquals(LocalDate.parse("2026-11-07"),every.latest(at("2026-11-10T08:00")).orElseThrow().day());
        assertEquals(LocalDate.parse("2026-11-10"),every.latest(at("2026-11-10T09:00")).orElseThrow().day());
    }
    @Test void oneOffAndDisabledDoNotInventAnOccurrence(){
        var once=new ReminderSchedule.Config("ONCE",LocalDate.parse("2026-11-15"),LocalTime.of(9,0),0,1);
        assertTrue(once.latest(at("2026-11-15T08:00")).isEmpty());
        assertEquals(LocalDate.parse("2026-11-15"),once.latest(at("2026-12-15T08:00")).orElseThrow().day());
        assertTrue(new ReminderSchedule.Config("NONE",once.starts(),once.time(),0,1).latest(at("2026-12-15T08:00")).isEmpty());
    }
    @Test void springGapAndAutumnOverlapUseOneWarsawOccurrence(){
        var spring=new ReminderSchedule.Config("DAILY",LocalDate.parse("2026-03-28"),LocalTime.of(2,30),0,1);
        assertEquals(LocalDate.parse("2026-03-28"),spring.latest(Instant.parse("2026-03-29T01:29:00Z")).orElseThrow().day());
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"),spring.latest(Instant.parse("2026-03-29T01:30:00Z")).orElseThrow().at());
        var autumn=new ReminderSchedule.Config("DAILY",LocalDate.parse("2026-10-24"),LocalTime.of(2,30),0,1);
        assertEquals(autumn.latest(Instant.parse("2026-10-25T00:30:00Z")),autumn.latest(Instant.parse("2026-10-25T01:30:00Z")));
    }
    @Test void invalidConfigurationIsRejected(){
        assertThrows(IllegalArgumentException.class,()->new ReminderSchedule.Config("WEEKDAYS",LocalDate.now(),LocalTime.NOON,0,1));
        assertThrows(IllegalArgumentException.class,()->new ReminderSchedule.Config("INTERVAL",LocalDate.now(),LocalTime.NOON,0,0));
        assertThrows(IllegalArgumentException.class,()->new ReminderSchedule.Config("DAILY",LocalDate.now(),null,0,1));
        assertThrows(IllegalArgumentException.class,()->new ReminderSchedule.Config("UNKNOWN",LocalDate.now(),LocalTime.NOON,0,1));
    }
}
