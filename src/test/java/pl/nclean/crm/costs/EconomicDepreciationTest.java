package pl.nclean.crm.costs;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pl.nclean.crm.shared.MonthlyAllocation;
import static org.junit.jupiter.api.Assertions.*;

class EconomicDepreciationTest {
    @Test void linearMonthsAndResidualAreExact() {
        var schedule=EconomicDepreciation.schedule(new BigDecimal("3600.00"),36);
        assertEquals(36,schedule.size());assertTrue(schedule.stream().allMatch(v->v.equals(new BigDecimal("100.00"))));
        assertEquals(new BigDecimal("1200.00"),schedule.subList(0,12).stream().reduce(new BigDecimal("0.00"),BigDecimal::add));
    }
    @Test void centAmountsNeverDisappearOrOverrunTheBase() {
        for(String value:new String[]{"0.00","0.01","1.00","900.01","9999999999.99"}) {
            var schedule=EconomicDepreciation.schedule(new BigDecimal(value),37);
            assertEquals(new BigDecimal(value),schedule.stream().reduce(new BigDecimal("0.00"),BigDecimal::add));
            assertTrue(schedule.stream().allMatch(v->v.signum()>=0 && v.scale()==2));
        }
    }
    @Test void dailySharesSumToMonthAndHandleLeapYears() {
        for(var month:new java.time.YearMonth[]{java.time.YearMonth.of(2028,2),java.time.YearMonth.of(2027,2),java.time.YearMonth.of(2026,11)}) {
            var sum=new BigDecimal("0.00");
            for(int i=1;i<=month.lengthOfMonth();i++)sum=sum.add(MonthlyAllocation.forDay(new BigDecimal("100.01"),month.atDay(i)));
            assertEquals(new BigDecimal("100.01"),sum);
        }
        assertEquals(new BigDecimal("50.00"),java.util.stream.IntStream.rangeClosed(1,15).mapToObj(d->MonthlyAllocation.forDay(new BigDecimal("100.00"),LocalDate.of(2026,11,d))).reduce(new BigDecimal("0.00"),BigDecimal::add));
    }
    @Test void invalidSchedulesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->EconomicDepreciation.schedule(new BigDecimal("-1"),12));
        assertThrows(IllegalArgumentException.class,()->EconomicDepreciation.schedule(new BigDecimal("1.001"),12));
        assertThrows(IllegalArgumentException.class,()->EconomicDepreciation.schedule(BigDecimal.ONE,0));
        assertThrows(IllegalArgumentException.class,()->EconomicDepreciation.schedule(BigDecimal.ONE,1201));
    }
}
