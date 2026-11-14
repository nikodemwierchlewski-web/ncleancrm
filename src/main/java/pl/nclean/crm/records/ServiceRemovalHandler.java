package pl.nclean.crm.records;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class ServiceRemovalHandler extends JdbcRemovalHandler {
    public ServiceRemovalHandler(JdbcTemplate db) {
        super(db, "service", "service_definition", Guard.WORK, Marker.TIMESTAMP);
    }

    @Override
    protected void validate(RemovalCommand command, Map<String, Object> row) {
        if (command.restore() && db.queryForObject(
                "SELECT count(*) FROM service_category WHERE id=? AND removed_at IS NULL",
                Integer.class, row.get("category_id")) == 0) {
            throw new IllegalArgumentException("Najpierw przywróć kategorię tej usługi.");
        }
    }
}
