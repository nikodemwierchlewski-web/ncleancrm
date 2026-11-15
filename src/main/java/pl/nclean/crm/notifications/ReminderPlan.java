package pl.nclean.crm.notifications;

import java.time.*;
import java.util.*;
import pl.nclean.crm.shared.BusinessDates;

/** Transport-independent commands, shared by note forms and the versioned API. */
public final class ReminderPlan {
    private ReminderPlan() {}
    public record Spec(UUID id,Long offsetMinutes,Instant absoluteAt) {
        public Spec {
            if(id==null || (offsetMinutes==null)==(absoluteAt==null))throw new IllegalArgumentException("Wybierz wyprzedzenie albo datę przypomnienia.");
            if(offsetMinutes!=null&&(offsetMinutes<0||offsetMinutes>5256000))throw new IllegalArgumentException("Wyprzedzenie: od 0 minut do 3650 dni.");
            if(absoluteAt!=null){absoluteAt=absoluteAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);BusinessDates.period(absoluteAt.atZone(BusinessDates.WARSAW).toLocalDate(),"day");}
        }
        public Instant scheduled(Instant anchor){return offsetMinutes==null?absoluteAt:anchor==null?null:anchor.minusSeconds(Math.multiplyExact(offsetMinutes,60));}
    }
    public record Edit(long version,List<Spec> reminders){
        public Edit {
            reminders=List.copyOf(reminders);
            if(version<0||reminders.size()>10||reminders.stream().map(Spec::id).distinct().count()!=reminders.size())throw new IllegalArgumentException("Dodaj do 10 różnych przypomnień.");
        }
    }
    public static Edit parse(Map<String,String> p){
        if(!p.containsKey("entity_reminders_present"))return null;
        var specs=new ArrayList<Spec>();
        for(var key:p.keySet())if(key.startsWith("entity_id_")){int index=Integer.parseInt(key.substring(10));if(index<0||index>=10)throw new IllegalArgumentException("Dodaj do 10 przypomnień.");}
        for(int i=0;i<10;i++)if(p.containsKey("entity_id_"+i)){
            var id=UUID.fromString(p.get("entity_id_"+i));var mode=p.get("entity_mode_"+i);
            if("OFFSET".equals(mode)){
                long unit=switch(p.getOrDefault("entity_unit_"+i,"minutes")){case "minutes"->1;case "hours"->60;case "days"->1440;default->throw new IllegalArgumentException("Wybierz minuty, godziny albo dni.");};
                long amount=Long.parseLong(p.get("entity_amount_"+i));
                if(amount<0||amount>5256000/unit)throw new IllegalArgumentException("Wyprzedzenie: od 0 minut do 3650 dni.");
                specs.add(new Spec(id,amount*unit,null));
            }else if("AT".equals(mode)){
                var local=LocalDateTime.parse(p.get("entity_at_"+i));
                if(BusinessDates.WARSAW.getRules().getValidOffsets(local).isEmpty())throw new IllegalArgumentException("Ta godzina nie istnieje przy zmianie czasu. Wybierz inną.");
                specs.add(new Spec(id,null,local.atZone(BusinessDates.WARSAW).toInstant()));
            }else throw new IllegalArgumentException("Wybierz sposób przypomnienia.");
        }
        return new Edit(Long.parseLong(p.getOrDefault("entity_version","0")),specs);
    }
}
