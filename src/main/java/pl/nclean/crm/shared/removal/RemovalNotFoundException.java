package pl.nclean.crm.shared.removal;

/** Domain-neutral absence; HTTP status mapping belongs to the web adapter. */
public class RemovalNotFoundException extends RuntimeException {
    public RemovalNotFoundException() {
        super("Nie znaleziono wpisu.");
    }
}
