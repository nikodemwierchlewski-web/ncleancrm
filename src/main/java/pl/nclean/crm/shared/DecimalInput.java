package pl.nclean.crm.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Bounded decimal input; each feature still owns its scale and business limits. */
public final class DecimalInput {
    private DecimalInput() {}

    public static BigDecimal parse(String value, int scale, BigDecimal max, boolean nullable) {
        if (value == null || value.isBlank()) {
            if (nullable) return null;
            throw new IllegalArgumentException("Uzupełnij wymagane kwoty i parametry.");
        }
        try {
            BigDecimal number = new BigDecimal(value.replace(',', '.'))
                    .setScale(scale, RoundingMode.UNNECESSARY);
            if (number.signum() < 0 || number.compareTo(max) > 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Podaj nieujemną liczbę, maksymalnie " + max
                    + ", do " + scale + " miejsc po przecinku.");
        }
    }
}
