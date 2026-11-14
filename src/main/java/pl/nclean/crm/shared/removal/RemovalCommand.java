package pl.nclean.crm.shared.removal;

/** Stable command contract; actor is supplied by the authenticated HTTP adapter. */
public record RemovalCommand(String kind, long id, long version, boolean restore, String actor) {
    public RemovalCommand {
        if (kind == null || !kind.matches("[a-z][a-z0-9_-]{0,19}") || id < 1 || version < 0) {
            throw new IllegalArgumentException("Niepoprawny wpis lub wersja.");
        }
        if (actor == null || actor.isBlank() || actor.length() > 120) {
            throw new IllegalArgumentException("Niepoprawny właściciel operacji.");
        }
    }
}
