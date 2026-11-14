package pl.nclean.crm.records;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;

@Component
public class CategoryRemovalHandler extends JdbcRemovalHandler {
    public CategoryRemovalHandler(JdbcTemplate db) {
        super(db, "category", "service_category", Guard.WORK, Marker.TIMESTAMP);
    }
}
