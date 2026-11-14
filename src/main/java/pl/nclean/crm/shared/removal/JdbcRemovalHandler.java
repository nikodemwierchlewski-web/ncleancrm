package pl.nclean.crm.shared.removal;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Technical protocol shared by feature-owned policies: lock, replay, version and audit.
 * Table identifiers are constructor constants, never request data. Domain validation
 * and non-timestamp mutations remain in the implementing feature.
 */
public abstract class JdbcRemovalHandler implements RemovalHandler {
    public enum Guard {
        WORK("work_write_guard"), FUEL("fuel_write_guard");
        private final String table;
        Guard(String table) { this.table = table; }
    }

    public enum Marker { TIMESTAMP, CANCELLED, INACTIVE }

    protected final JdbcTemplate db;
    private final String kind;
    private final String table;
    private final Guard guard;
    private final Marker marker;

    protected JdbcRemovalHandler(JdbcTemplate db, String kind, String table, Guard guard, Marker marker) {
        if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Invalid table identifier");
        this.db = db;
        this.kind = kind;
        this.table = table;
        this.guard = guard;
        this.marker = marker;
    }

    @Override
    public String kind() { return kind; }

    /** Calling a handler directly without the coordinator's transaction is an error. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RemovalResult change(RemovalCommand command) {
        if (!kind.equals(command.kind())) throw new IllegalArgumentException("Niepoprawny rodzaj wpisu.");
        db.queryForObject("SELECT id FROM " + guard.table + " WHERE id=1 FOR UPDATE", Integer.class);
        Map<String, Object> row = db.queryForList("SELECT * FROM " + table + " WHERE id=? FOR UPDATE", command.id())
                .stream().findFirst().orElseThrow(RemovalNotFoundException::new);
        boolean removed = switch (marker) {
            case TIMESTAMP -> row.get("removed_at") != null;
            case CANCELLED -> Boolean.TRUE.equals(row.get("cancelled"));
            case INACTIVE -> !Boolean.TRUE.equals(row.get("active"));
        };
        long current = ((Number) row.get("version")).longValue();
        if (removed == !command.restore() && (command.version() == current || command.version() + 1 == current)) {
            return RemovalResult.saved(command, current, removed, day(row));
        }
        if (command.version() != current) {
            throw new IllegalStateException("Wpis zmienił się w innym oknie. Odśwież dane przed usunięciem lub przywróceniem.");
        }
        validate(command, row);
        String before = db.queryForObject("SELECT to_jsonb(t)::text FROM " + table + " t WHERE id=?",
                String.class, command.id());
        perform(command, row);
        db.update("INSERT INTO removal_audit(kind,entity_id,action,before_data,after_data,actor) "
                        + "SELECT ?,?,?,?::jsonb,to_jsonb(t),? FROM " + table + " t WHERE id=?",
                kind, command.id(), command.restore() ? "RESTORE" : "REMOVE", before, command.actor(), command.id());
        return RemovalResult.saved(command, current + 1, !command.restore(), day(row));
    }

    protected void validate(RemovalCommand command, Map<String, Object> row) {}

    protected void perform(RemovalCommand command, Map<String, Object> row) {
        int changed = db.update("UPDATE " + table + " SET removed_at="
                        + (command.restore() ? "NULL" : "CURRENT_TIMESTAMP")
                        + ",version=version+1 WHERE id=? AND version=?", command.id(), command.version());
        if (changed != 1) throw new IllegalStateException("Wpis zmienił się w innym oknie.");
    }

    protected String day(Map<String, Object> row) { return null; }
}
