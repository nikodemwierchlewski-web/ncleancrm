package pl.nclean.crm.costs;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class ExpenseRemovalHandler extends JdbcRemovalHandler {
    private final CostService costs;

    public ExpenseRemovalHandler(JdbcTemplate db, CostService costs) {
        super(db, "expense", "cost_entry", Guard.FUEL, Marker.CANCELLED);
        this.costs = costs;
    }

    @Override
    protected void perform(RemovalCommand command, Map<String, Object> row) {
        if (command.restore()) costs.restore(command.id(), command.version(), command.actor());
        else costs.cancel(command.id(), command.version(), "Usunięto błędny wpis", command.actor());
    }
}
