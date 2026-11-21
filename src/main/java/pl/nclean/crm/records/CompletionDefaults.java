package pl.nclean.crm.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Human work-time suggestions are independent of machine operating-hour standards. */
@Service
@Transactional(readOnly=true)
public class CompletionDefaults {
    private final JdbcTemplate db;
    public CompletionDefaults(JdbcTemplate db){this.db=db;}
    public record TimeHint(Integer minutes,String source) {}
    public TimeHint time(long booking) {
        var items=db.queryForList("SELECT i.standard_time_origin,i.standard_work_minutes,CASE WHEN i.standard_work_minutes IS NOT NULL THEN i.standard_work_minutes WHEN i.unit=s.unit THEN s.minutes*i.quantity END suggested FROM booking_item i JOIN service_definition s ON s.id=i.service_id WHERE i.booking_id=?",booking);
        if(items.isEmpty() || items.stream().anyMatch(i->i.get("suggested")==null))return new TimeHint(null,"Brak zgodnego standardu czasu. Możesz podać własny czas.");
        var total=items.stream().map(i->(BigDecimal)i.get("suggested")).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(0,RoundingMode.HALF_UP);
        if(total.compareTo(new BigDecimal("10080"))>0)return new TimeHint(null,"Standard przekracza 7 dni. Podaj własny czas.");
        String source=items.stream().anyMatch(i->i.get("standard_work_minutes")==null)?"Podpowiedź z obecnego cennika — zapamiętamy ją przy zapisie.":items.stream().anyMatch(i->"COMPLETION".equals(i.get("standard_time_origin")))?"Standard zapamiętany przy uzupełnianiu zlecenia.":"Standard zapamiętany przy umawianiu.";
        return new TimeHint(total.intValueExact(),source);
    }
    @Transactional public Integer captureTime(long booking) {
        db.queryForObject("SELECT id FROM work_write_guard WHERE id=1 FOR UPDATE",Integer.class);
        db.update("UPDATE booking_item i SET standard_work_minutes=s.minutes*i.quantity,standard_time_origin='COMPLETION',standard_time_captured_at=CURRENT_TIMESTAMP FROM service_definition s WHERE i.booking_id=? AND i.service_id=s.id AND i.unit=s.unit AND i.standard_work_minutes IS NULL",booking);
        return time(booking).minutes();
    }
}
