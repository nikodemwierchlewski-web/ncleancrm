package pl.nclean.crm.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Called under the domain write lock and in the same transaction as the command. */
@Component
public class RequestReplay {
    private final JdbcTemplate db;
    private final ObjectMapper json;

    public RequestReplay(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    public String payload(Object command) {
        try {
            return json.writeValueAsString(command);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Nie można odczytać formularza.", ex);
        }
    }

    public long existing(String scope, UUID token, String payload, long id) {
        var matches = db.queryForList("SELECT payload=?::jsonb FROM request_replay WHERE scope=? AND token=? AND entity_id=?",
                Boolean.class, payload, scope, token, id);
        if (matches.isEmpty() || !matches.get(0)) {
            String href = switch (scope) {
                case "BOOKING" -> "/work/" + id;
                case "NOTE" -> "/notes?entry=" + id;
                case "COST" -> "/costs/editor?id=" + id;
                default -> "/fuel?purchase=" + id;
            };
            throw new Conflict(id, href);
        }
        return id;
    }

    public void remember(String scope, UUID token, String payload, long id) {
        db.update("INSERT INTO request_replay(scope,token,payload,entity_id) VALUES (?,?,?::jsonb,?)", scope, token, payload, id);
    }

    public static class Conflict extends IllegalArgumentException {
        private final String href;
        public Conflict(long id, String href) {
            super("Ten formularz został już zapisany (wpis nr " + id + "). Zmienione pola pozostają tutaj. Otwórz zapisany wpis przed korektą.");
            this.href = href;
        }
        public String href() { return href; }
    }
}
