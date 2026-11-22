package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerInsightMathTest {
    @Test void medianDoesNotBecomeTheMeanOfAnOutlier(){
        var s=CustomerInsights.values(List.of(new BigDecimal("100"),new BigDecimal("200"),new BigDecimal("900")));
        assertEquals(new BigDecimal("400.00"),s.average());assertEquals(new BigDecimal("200.00"),s.median());
    }
    @Test void evenSampleUsesExactDecimalArithmeticAndRoundsOnce(){
        var s=CustomerInsights.values(List.of(new BigDecimal("0.01"),new BigDecimal("0.02")));
        assertEquals(new BigDecimal("0.02"),s.average());assertEquals(new BigDecimal("0.02"),s.median());
        assertNull(CustomerInsights.values(List.of()).median());
    }
}
