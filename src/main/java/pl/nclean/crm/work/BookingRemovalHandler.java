package pl.nclean.crm.work;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class BookingRemovalHandler extends JdbcRemovalHandler {
    public BookingRemovalHandler(JdbcTemplate db) {
        super(db, "booking", "booking", Guard.WORK, Marker.TIMESTAMP);
    }

    @Override
    protected void validate(RemovalCommand command, Map<String, Object> row) {
        if (command.restore()) return;
        if (row.get("timer_started") != null) {
            throw new IllegalArgumentException("Najpierw zatrzymaj pomiar czasu tego zlecenia.");
        }
        var paid = db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM booking_payment WHERE booking_id=?",
                BigDecimal.class, command.id());
        if (paid.signum() != 0) {
            throw new IllegalArgumentException("Zlecenie ma wpłatę. Najpierw skoryguj ją w płatnościach, aby nie zgubić rozliczenia.");
        }
    }

    @Override
    protected String day(Map<String, Object> row) { return row.get("day").toString(); }
}
