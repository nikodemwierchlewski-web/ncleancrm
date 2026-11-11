package pl.nclean.crm.dashboard;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DashboardTest {
 @Test void dailyAllocationSumsExactlyEvenInLeapYearAndAcrossMonths(){
  for(var month:new YearMonth[]{YearMonth.of(2028,2),YearMonth.of(2027,2),YearMonth.of(2026,11),YearMonth.of(2026,12)}){
   BigDecimal sum=BigDecimal.ZERO;for(int i=1;i<=month.lengthOfMonth();i++)sum=sum.add(DashboardService.allocated(new BigDecimal("100.01"),month.atDay(i)));assertEquals(new BigDecimal("100.01"),sum);
  }
  var week=DashboardService.period(LocalDate.of(2026,12,31),"week");assertEquals(LocalDate.of(2026,12,28),week.start());assertEquals(LocalDate.of(2027,1,3),week.end());
  var leap=DashboardService.period(LocalDate.of(2028,2,10),"month");assertEquals(29,leap.end().getDayOfMonth());assertEquals(LocalDate.of(2028,1,1),leap.previous());
 }
 @Test void invalidPeriodsAndQuantitiesAreRejectedAndBlankDiffersFromZero(){
  assertThrows(IllegalArgumentException.class,()->DashboardService.period(LocalDate.now(),"year"));assertThrows(IllegalArgumentException.class,()->DashboardService.period(LocalDate.of(1900,1,1),"day"));
  assertNull(DashboardService.number("",2,new BigDecimal("100"),true));assertEquals(new BigDecimal("0.00"),DashboardService.number("0",2,new BigDecimal("100"),true));
  for(String value:new String[]{"-1","1.001","NaN","101"})assertThrows(IllegalArgumentException.class,()->DashboardService.number(value,2,new BigDecimal("100"),false));
 }
}
