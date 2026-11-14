package pl.nclean.crm.costs;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class EquipmentRemovalHandler extends JdbcRemovalHandler {
    public EquipmentRemovalHandler(JdbcTemplate db) {
        super(db, "equipment", "equipment_item", Guard.FUEL, Marker.INACTIVE);
    }

    @Override
    protected void perform(RemovalCommand command, Map<String, Object> row) {
        db.update("UPDATE equipment_item SET active=?,version=version+1 WHERE id=? AND version=?",
                command.restore(), command.id(), command.version());
    }
}
