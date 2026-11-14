package pl.nclean.crm.fuel;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.nclean.crm.shared.removal.JdbcRemovalHandler;
import pl.nclean.crm.shared.removal.RemovalCommand;

@Component
public class FuelPurchaseRemovalHandler extends JdbcRemovalHandler {
    private final FuelJournalService fuel;

    public FuelPurchaseRemovalHandler(JdbcTemplate db, FuelJournalService fuel) {
        super(db, "fuel", "fuel_purchase", Guard.FUEL, Marker.CANCELLED);
        this.fuel = fuel;
    }

    @Override
    protected void perform(RemovalCommand command, Map<String, Object> row) {
        if (command.restore()) fuel.restorePurchase(command.id(), command.version(), command.actor());
        else fuel.cancelPurchase(command.id(), command.version(), "Usunięto błędny wpis", command.actor());
    }
}
