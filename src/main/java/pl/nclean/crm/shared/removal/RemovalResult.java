package pl.nclean.crm.shared.removal;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire-compatible with the existing removal JSON, with an optional booking day. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemovalResult(boolean saved, String kind, long id, long version, boolean removed, String day) {
    public static RemovalResult saved(RemovalCommand command, long version, boolean removed, String day) {
        return new RemovalResult(true, command.kind(), command.id(), version, removed, day);
    }
}
