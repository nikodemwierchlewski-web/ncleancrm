package pl.nclean.crm.removal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.shared.removal.RemovalCommand;
import pl.nclean.crm.shared.removal.RemovalHandler;
import pl.nclean.crm.shared.removal.RemovalResult;

/** Application coordinator. Features own data/policies; this class owns the transaction. */
@Service
public class RemovalService {
    private final Map<String, RemovalHandler> handlers;

    public RemovalService(List<RemovalHandler> registered) {
        var byKind = new LinkedHashMap<String, RemovalHandler>();
        for (var handler : registered) {
            if (byKind.putIfAbsent(handler.kind(), handler) != null) {
                throw new IllegalStateException("Duplicate removal handler: " + handler.kind());
            }
        }
        handlers = Map.copyOf(byKind);
    }

    @Transactional
    public RemovalResult change(String kind, long id, long version, boolean restore, String actor) {
        var command = new RemovalCommand(kind, id, version, restore, actor);
        var handler = handlers.get(command.kind());
        if (handler == null) throw new IllegalArgumentException("Niepoprawny wpis lub wersja.");
        return handler.change(command);
    }
}
