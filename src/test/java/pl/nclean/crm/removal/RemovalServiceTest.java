package pl.nclean.crm.removal;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.nclean.crm.shared.removal.*;
import static org.junit.jupiter.api.Assertions.*;

class RemovalServiceTest {
    @Test void newKindIsRegisteredWithoutChangingTheCoordinator() {
        var handler = new SampleHandler("sample");
        var service = new RemovalService(List.of(handler));
        var result = service.change("sample", 12, 3, false, "owner");
        assertEquals(new RemovalCommand("sample",12,3,false,"owner"),handler.received);
        assertEquals(new RemovalResult(true,"sample",12,4,true,null),result);
    }

    @Test void duplicateRegistrationsFailAtStartupInsteadOfChoosingOneSilently() {
        assertThrows(IllegalStateException.class, () -> new RemovalService(
                List.of(new SampleHandler("sample"),new SampleHandler("sample"))));
    }

    @Test void invalidRequestsNeverReachAHandler() {
        var handler = new SampleHandler("sample");
        var service = new RemovalService(List.of(handler));
        assertThrows(IllegalArgumentException.class, () -> service.change("unknown",1,0,false,"owner"));
        assertThrows(IllegalArgumentException.class, () -> service.change("sample",-1,0,false,"owner"));
        assertThrows(IllegalArgumentException.class, () -> service.change("sample",1,-1,false,"owner"));
        assertThrows(IllegalArgumentException.class, () -> service.change("sample",1,0,false,""));
        assertNull(handler.received);
    }

    private static final class SampleHandler implements RemovalHandler {
        private final String kind;
        private RemovalCommand received;
        SampleHandler(String kind) { this.kind = kind; }
        public String kind() { return kind; }
        public RemovalResult change(RemovalCommand command) {
            received = command;
            return RemovalResult.saved(command,command.version()+1,!command.restore(),null);
        }
    }
}
