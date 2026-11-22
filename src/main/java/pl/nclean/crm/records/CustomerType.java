package pl.nclean.crm.records;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum CustomerType {
    UNKNOWN("Nieustalony"), PRIVATE("Prywatny"), BUSINESS("Biznesowy");

    private final String label;
    CustomerType(String label) { this.label = label; }
    public static final Map<String,String> LABELS;
    static {
        var labels = new LinkedHashMap<String,String>();
        for (var type : values()) labels.put(type.name(),type.label);
        LABELS = Collections.unmodifiableMap(labels);
    }
    public static CustomerType parse(String value) {
        if (value == null || !LABELS.containsKey(value))
            throw new IllegalArgumentException("Wybierz rodzaj klienta: prywatny, biznesowy lub nieustalony.");
        return valueOf(value);
    }
    /** Version protects a profile change made as part of creating a visit. */
    public record Choice(CustomerType type, Long customerVersion) {
        public Choice {
            if (type == null || customerVersion != null && customerVersion < 0)
                throw new IllegalArgumentException("Sprawdź rodzaj i wersję klienta.");
        }
    }
}
