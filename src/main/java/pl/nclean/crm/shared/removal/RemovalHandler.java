package pl.nclean.crm.shared.removal;

/** A feature opts in by registering a bean; the coordinator does not know its tables. */
public interface RemovalHandler {
    String kind();
    RemovalResult change(RemovalCommand command);
}
