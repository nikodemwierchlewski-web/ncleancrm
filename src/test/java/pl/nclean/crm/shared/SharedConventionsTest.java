package pl.nclean.crm.shared;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedConventionsTest {
    @Test void calendarBoundariesCoverYearCrossingAndLeapMonths() {
        var week=BusinessDates.period(LocalDate.of(2027,1,1),"week");
        assertEquals(LocalDate.of(2026,12,28),week.start());
        assertEquals(LocalDate.of(2027,1,3),week.end());
        assertEquals(LocalDate.of(2026,12,21),week.previous());
        assertEquals(LocalDate.of(2027,1,4),week.next());
        var leap=BusinessDates.period(LocalDate.of(2028,2,12),"month");
        assertEquals(LocalDate.of(2028,2,29),leap.end());
        assertThrows(IllegalArgumentException.class,()->BusinessDates.period(LocalDate.of(1999,12,31),"day"));
    }

    @Test void boundedDecimalsKeepMissingDataDistinctFromZeroAndRejectRounding() {
        assertNull(DecimalInput.parse("",2,new BigDecimal("100"),true));
        assertEquals(new BigDecimal("0.00"),DecimalInput.parse("0",2,new BigDecimal("100"),false));
        assertEquals(new BigDecimal("12.35"),DecimalInput.parse("12,35",2,new BigDecimal("100"),false));
        for(String bad:new String[]{"", "-1", "100.01", "12.351"}) {
            assertThrows(IllegalArgumentException.class,()->DecimalInput.parse(bad,2,new BigDecimal("100"),false));
        }
    }
}
