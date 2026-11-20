package pl.nclean.crm.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/** Cumulative rounding: daily shares always sum to the exact monthly amount. */
public final class MonthlyAllocation {
    private MonthlyAllocation() {}
    public static BigDecimal forDay(BigDecimal amount, LocalDate day) {
        var length = BigDecimal.valueOf(YearMonth.from(day).lengthOfMonth());
        return amount.multiply(BigDecimal.valueOf(day.getDayOfMonth())).divide(length, 2, RoundingMode.HALF_UP)
                .subtract(amount.multiply(BigDecimal.valueOf(day.getDayOfMonth() - 1)).divide(length, 2, RoundingMode.HALF_UP));
    }
}
