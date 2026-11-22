package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.records.CustomerType;
import pl.nclean.crm.shared.BusinessDates;

/** Completed-visit statistics; category prices are never guessed from a mixed invoice. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CustomerInsights {
    private final JdbcTemplate db;
    public CustomerInsights(JdbcTemplate db) { this.db = db; }

    public record Values(int count, BigDecimal total, BigDecimal average, BigDecimal median) {}
    public record Category(long id, String name, Values values, int missingSplit) {}
    public record Returns(int clients, int newClients, int returningClients, BigDecimal returningShare,
                          int repeatVisits, BigDecimal repeatRevenue, BigDecimal medianDays) {}
    public record Report(String type, Values overall, List<Category> categories, int missingSplit,
                         int unconfirmedPrices, int unlinkedVisits, Returns returns) {}
    private static final class CategoryValues {
        final long id;
        String name;
        final List<BigDecimal> values = new ArrayList<>();
        int missing;
        CategoryValues(long id, String name) { this.id=id; this.name=name; }
    }

    public Report report(LocalDate start, LocalDate end, String type) {
        BusinessDates.period(start,"day");
        BusinessDates.period(end,"day");
        if(end.isBefore(start) || ChronoUnit.DAYS.between(start,end)>=366)
            throw new IllegalArgumentException("Wybierz okres do 366 dni.");
        if(!"ALL".equals(type))CustomerType.parse(type);
        // Compute history BEFORE applying the period/type filter. Earlier visits of any type count.
        var bookings=db.queryForList("""
                WITH history AS (
                  SELECT b.*,MIN(day) OVER(PARTITION BY customer_id) AS first_day,
                         LAG(day) OVER(PARTITION BY customer_id ORDER BY day,starts,id) AS previous_day
                  FROM booking b WHERE removed_at IS NULL AND status='DONE' AND day<=?
                )
                SELECT *,COALESCE(final_total,total) AS value FROM history
                WHERE day>=? AND (?='ALL' OR customer_type=?) ORDER BY day,starts,id
                """,end,start,type,type);
        var items=db.queryForList("""
                SELECT i.*,s.category_id,c.name AS category_name
                FROM booking_item i JOIN booking b ON b.id=i.booking_id
                JOIN service_definition s ON s.id=i.service_id JOIN service_category c ON c.id=s.category_id
                WHERE b.removed_at IS NULL AND b.status='DONE' AND b.day BETWEEN ? AND ?
                  AND (?='ALL' OR b.customer_type=?) ORDER BY b.day,b.starts,b.id,i.position
                """,start,end,type,type);
        var byBooking=new HashMap<Long,List<Map<String,Object>>>();
        for(var i:items)byBooking.computeIfAbsent(number(i,"booking_id"),k->new ArrayList<>()).add(i);
        var categories=new LinkedHashMap<Long,CategoryValues>();
        var allValues=new ArrayList<BigDecimal>();
        var newClients=new HashSet<Long>();var returningClients=new HashSet<Long>();
        var days=new ArrayList<BigDecimal>();
        BigDecimal repeatRevenue=BigDecimal.ZERO;
        int unconfirmed=0,unlinked=0,missingSplit=0,repeatVisits=0;
        for(var b:bookings){
            var value=(BigDecimal)b.get("value");allValues.add(value);
            if(b.get("final_total")==null)unconfirmed++;
            if(b.get("customer_id")==null)unlinked++;
            else {
                long customer=number(b,"customer_id");
                if(day(b.get("first_day")).isBefore(start))returningClients.add(customer);else newClients.add(customer);
                if(b.get("previous_day")!=null){
                    repeatVisits++;repeatRevenue=repeatRevenue.add(value);
                    days.add(BigDecimal.valueOf(ChronoUnit.DAYS.between(day(b.get("previous_day")),day(b.get("day")))));
                }
            }
            var lines=byBooking.getOrDefault(number(b,"id"),List.of());
            var groups=new LinkedHashMap<Long,List<Map<String,Object>>>();
            for(var line:lines){
                long category=number(line,"category_id");
                var bucket=categories.computeIfAbsent(category,k->new CategoryValues(k,(String)line.get("category_name")));
                bucket.name=(String)line.get("category_name");
                groups.computeIfAbsent(category,k->new ArrayList<>()).add(line);
            }
            if(groups.size()==1){categories.get(groups.keySet().iterator().next()).values.add(value);continue;}
            boolean complete=groups.size()>1 && b.get("final_total")!=null
                    && lines.stream().allMatch(i->i.get("final_amount")!=null)
                    && lines.stream().map(i->(BigDecimal)i.get("final_amount")).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(value)==0;
            if(!complete){
                missingSplit++;
                groups.keySet().forEach(id->categories.get(id).missing++);
                continue;
            }
            groups.forEach((id,rows)->categories.get(id).values.add(rows.stream().map(i->(BigDecimal)i.get("final_amount")).reduce(BigDecimal.ZERO,BigDecimal::add)));
        }
        int clients=newClients.size()+returningClients.size();
        var returns=new Returns(clients,newClients.size(),returningClients.size(),clients==0?BigDecimal.ZERO:
                BigDecimal.valueOf(returningClients.size()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(clients),1,RoundingMode.HALF_UP),
                repeatVisits,repeatRevenue,median(days,1));
        var categoryRows=categories.values().stream().sorted(Comparator.comparing(c->c.name))
                .map(c->new Category(c.id,c.name,values(c.values),c.missing)).toList();
        return new Report(type,values(allValues),categoryRows,missingSplit,unconfirmed,unlinked,returns);
    }

    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static LocalDate day(Object value){return ((java.sql.Date)value).toLocalDate();}
    static Values values(List<BigDecimal> amounts){
        var total=amounts.stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Values(amounts.size(),total,amounts.isEmpty()?null:total.divide(BigDecimal.valueOf(amounts.size()),2,RoundingMode.HALF_UP),median(amounts,2));
    }
    private static BigDecimal median(List<BigDecimal> amounts,int scale){
        if(amounts.isEmpty())return null;
        var sorted=amounts.stream().sorted().toList();int middle=sorted.size()/2;
        return (sorted.size()%2==1?sorted.get(middle):sorted.get(middle-1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2)))
                .setScale(scale,RoundingMode.HALF_UP);
    }
}
