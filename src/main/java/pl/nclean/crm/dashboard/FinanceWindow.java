package pl.nclean.crm.dashboard;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import pl.nclean.crm.shared.BusinessDates;

/** Today filters do not alter the full-period accounting dashboard. */
public final class FinanceWindow {
    private FinanceWindow(){}
    public static DashboardService.Period select(LocalDate focus,String filter,LocalDate from,LocalDate to){
        BusinessDates.period(focus,"day");
        LocalDate start,end;
        switch(filter){
            case "day"->{start=focus;end=focus;}
            case "week-to-date"->{start=BusinessDates.period(focus,"week").start();end=focus;}
            case "month-to-date"->{start=focus.withDayOfMonth(1);end=focus;}
            case "week","month"->{var p=BusinessDates.period(focus,filter);start=p.start();end=p.end();}
            case "custom"->{start=from;end=to;}
            default->throw new IllegalArgumentException("Wybierz zakres podsumowania.");
        }
        BusinessDates.period(start,"day");BusinessDates.period(end,"day");
        if(end.isBefore(start)||ChronoUnit.DAYS.between(start,end)>365)throw new IllegalArgumentException("Zakres musi obejmować od 1 do 366 dni.");
        return new DashboardService.Period(start,end,filter);
    }
}
