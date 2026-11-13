package pl.nclean.crm.help;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import pl.nclean.crm.work.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GuideServiceTest {
    final DayReviewService days=mock(DayReviewService.class);
    final WorkService work=mock(WorkService.class);
    final LocalDate today=LocalDate.of(2026,11,13);
    GuideService at(int hour){return new GuideService(days,work,Clock.fixed(today.atTime(hour,0).atZone(ZoneId.of("Europe/Warsaw")).toInstant(),ZoneId.of("Europe/Warsaw")));}
    DayReviewService.Review review(LocalDate day,String status,List<String> missing,boolean closed){
        Map<String,Object> order=new LinkedHashMap<>(Map.of("id",7L,"status",status,"missing",missing,"complete",missing.isEmpty()));
        return new DayReviewService.Review(day,List.of(order),List.of(),missing.isEmpty()?1:0,"fingerprint",closed,false);
    }
    @Test void plansWaitUntilEveningAndOfferOnlyActualMissingData(){
        when(days.review(today)).thenReturn(review(today,"PLANNED",List.of("potwierdź, czy usługa się odbyła"),false));when(days.latestOpenBefore(today)).thenReturn(Optional.empty());
        assertFalse(at(17).context(null,null).containsKey("reminder"));var reminder=(Map<?,?>)at(18).context(null,null).get("reminder");assertEquals(1,reminder.get("missing"));assertEquals("/day-closeout?date=2026-11-13&order=7",reminder.get("url"));
    }
    @Test void emptyAndFutureDaysNeverBecomeTheirOwnOverdueWork(){
        when(days.review(today)).thenReturn(new DayReviewService.Review(today,List.of(),List.of(),0,"empty",false,false));when(days.latestOpenBefore(today)).thenReturn(Optional.empty());assertFalse(at(20).context(null,null).containsKey("reminder"));
        when(days.review(today.plusDays(1))).thenReturn(review(today.plusDays(1),"PLANNED",List.of("status"),false));assertFalse(at(20).context(today.plusDays(1),null).containsKey("reminder"));
    }
    @Test void completedOrCancelledOrdersCanCloseBeforeEveningWithoutFabricatedMeasurements(){
        when(days.review(today)).thenReturn(review(today,"CANCELLED",List.of(),false));var reminder=(Map<?,?>)at(10).context(null,null).get("reminder");assertEquals(0,reminder.get("missing"));assertTrue(reminder.get("message").toString().contains("Możesz go zamknąć"));
    }
    @Test void closedDayStaysQuietButARealOlderOpenDayCanBeOffered(){
        when(days.review(today)).thenReturn(review(today,"DONE",List.of(),true));when(days.latestOpenBefore(today)).thenReturn(Optional.empty());assertFalse(at(20).context(null,null).containsKey("reminder"));
        var old=review(today.minusDays(3),"DONE",List.of("zdjęcie po"),false);when(days.latestOpenBefore(today)).thenReturn(Optional.of(old));var reminder=(Map<?,?>)at(20).context(null,null).get("reminder");assertEquals("2026-11-10",reminder.get("date"));
    }
    @Test void focusedHelpUsesCanonicalMissingListAndNeverSubstitutesAnotherOrder(){
        when(work.order(7)).thenReturn(Map.of("day",java.sql.Date.valueOf(today)));when(days.review(today)).thenReturn(review(today,"DONE",List.of("rzeczywista chemia","zdjęcie po"),false));
        var context=at(15).context(null,7L);assertTrue(context.get("focused").toString().contains("rzeczywista chemia, zdjęcie po"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->at(15).context(today.minusDays(1),7L));
    }
}
