package pl.nclean.crm.costs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Pure linear schedule in cents; the last months absorb cumulative rounding. */
public final class EconomicDepreciation {
    private EconomicDepreciation() {}
    public static List<BigDecimal> schedule(BigDecimal depreciable, int months) {
        if (depreciable == null || depreciable.signum() < 0 || depreciable.scale() > 2 || months < 1 || months > 1200)
            throw new IllegalArgumentException("Sprawdź wartość i okres amortyzacji.");
        var amounts = new ArrayList<BigDecimal>(months);
        BigDecimal previous = BigDecimal.ZERO.setScale(2);
        for (int i = 1; i <= months; i++) {
            var cumulative = depreciable.multiply(BigDecimal.valueOf(i)).divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            amounts.add(cumulative.subtract(previous));
            previous = cumulative;
        }
        return List.copyOf(amounts);
    }
}
