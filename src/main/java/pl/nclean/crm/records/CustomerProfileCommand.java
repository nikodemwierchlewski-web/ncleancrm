package pl.nclean.crm.records;

/** A profile edit is one use case, including the optional acquisition-source update. */
public record CustomerProfileCommand(
        Long id, long version, String name, String phone, String address, String notes,
        boolean active, boolean updateSource, String source, String referralNote) {}
