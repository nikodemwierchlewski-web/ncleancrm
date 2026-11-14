package pl.nclean.crm.records;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class ChemicalRemovalHandler extends JdbcRemovalHandler {
    public ChemicalRemovalHandler(JdbcTemplate db) {
        super(db, "chemical", "chemical", Guard.WORK, Marker.TIMESTAMP);
    }

    @Override
    protected void validate(RemovalCommand command, Map<String, Object> row) {
        if (!command.restore() && db.queryForObject("""
                SELECT (SELECT count(*) FROM service_chemical WHERE chemical_id=?)
                  +(SELECT count(*) FROM booking_chemical c JOIN booking b ON b.id=c.booking_id
                    WHERE c.chemical_id=? AND b.removed_at IS NULL AND b.status='PLANNED')
                """, Integer.class, command.id(), command.id()) > 0) {
            throw new IllegalArgumentException("Środek jest w standardzie lub prognozie zaplanowanej wizyty. Najpierw zmień te powiązania; historia pozostanie zachowana.");
        }
    }
}
