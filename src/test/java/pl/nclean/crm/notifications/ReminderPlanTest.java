package pl.nclean.crm.notifications;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReminderPlanTest {
 private Map<String,String> fields(String amount,String unit){return new HashMap<>(Map.of("entity_reminders_present","true","entity_id_0",UUID.randomUUID().toString(),"entity_mode_0","OFFSET","entity_amount_0",amount,"entity_unit_0",unit));}
 @Test void minutesHoursAndDaysAreExact(){for(var pair:new String[][]{{"30","minutes","30"},{"3","hours","180"},{"17","days","24480"}}){var r=ReminderPlan.parse(fields(pair[0],pair[1])).reminders().get(0);assertEquals(Long.valueOf(pair[2]),r.offsetMinutes());assertEquals(Instant.parse("2026-11-15T12:00:00Z").minusSeconds(Long.parseLong(pair[2])*60),r.scheduled(Instant.parse("2026-11-15T12:00:00Z")));}}
 @Test void limitsRejectOverflowAndInvalidUnits(){for(var pair:new String[][]{{"-1","minutes"},{"9223372036854775807","days"},{"2","bad"},{"3651","days"}})assertThrows(IllegalArgumentException.class,()->ReminderPlan.parse(fields(pair[0],pair[1])));}
 @Test void absoluteDatesAreWarsawAndDstGapIsRejected(){var p=fields("30","minutes");p.put("entity_mode_0","AT");p.put("entity_at_0","2026-11-15T10:00");assertEquals(Instant.parse("2026-11-15T09:00:00Z"),ReminderPlan.parse(p).reminders().get(0).absoluteAt());p.put("entity_at_0","2027-03-28T02:30");assertThrows(IllegalArgumentException.class,()->ReminderPlan.parse(p));}
 @Test void missingMarkerPreservesOlderClientsAndIdsMustBeUnique(){assertNull(ReminderPlan.parse(Map.of()));var a=new ReminderPlan.Spec(UUID.randomUUID(),30L,null);assertThrows(IllegalArgumentException.class,()->new ReminderPlan.Edit(0,List.of(a,a)));}
}
