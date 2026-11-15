package pl.nclean.crm.dashboard;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinanceWindowTest {
 private final LocalDate day=LocalDate.of(2026,11,15);
 @Test void toDateNeverIncludesRestOfMonth(){var p=FinanceWindow.select(day,"month-to-date",null,null);assertEquals(LocalDate.of(2026,11,1),p.start());assertEquals(day,p.end());assertEquals(day,FinanceWindow.select(day,"week-to-date",null,null).end());}
 @Test void wholeWeekAndDayRemainExplicit(){assertEquals(LocalDate.of(2026,11,9),FinanceWindow.select(day,"week",null,null).start());assertEquals(day,FinanceWindow.select(day,"day",null,null).start());assertEquals(LocalDate.of(2026,11,30),FinanceWindow.select(day,"month",null,null).end());}
 @Test void customBoundedAndValidated(){assertEquals(day,FinanceWindow.select(day,"custom",day,day).end());for(var to:new LocalDate[]{day.minusDays(1),day.plusDays(366),null})assertThrows(IllegalArgumentException.class,()->FinanceWindow.select(day,"custom",day,to));assertThrows(IllegalArgumentException.class,()->FinanceWindow.select(day,"unexpected",null,null));}
}
