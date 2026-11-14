package pl.nclean.crm.records;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Customer-owned vocabulary used by booking, customer editing and contact screens. */
public enum CustomerAcquisitionSource {
    UNKNOWN("Nieustalone"), GOOGLE("Google / Mapy"), FACEBOOK("Facebook"),
    REFERRAL("Polecenie"), OLX("OLX"), OTHER("Inne");

    private final String label;

    CustomerAcquisitionSource(String label) {
        this.label = label;
    }

    public static final Map<String, String> LABELS;

    static {
        var labels = new LinkedHashMap<String, String>();
        for (var source : values()) labels.put(source.name(), source.label);
        LABELS = Collections.unmodifiableMap(labels);
    }

    public static String normalize(String value) {
        String code = value == null || value.isBlank() ? "UNKNOWN" : value;
        if (!LABELS.containsKey(code)) throw new IllegalArgumentException("Wybierz źródło klienta.");
        return code;
    }
}
