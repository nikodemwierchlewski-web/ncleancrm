package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.nclean.crm.records.CustomerAcquisitionSource;
import pl.nclean.crm.shared.BusinessDates;

/** Read-only acquisition report, attributed to the source currently recorded on the customer. */
@Service
@Transactional(readOnly = true)
public class CustomerSourceStatistics {
    private final JdbcTemplate db;

    public CustomerSourceStatistics(JdbcTemplate db) {
        this.db = db;
    }

    public record Bar(String code, String label, BigDecimal value, BigDecimal percentage) {}
    public record Series(String key, String label, boolean money, BigDecimal total, List<Bar> bars) {}
    public record Report(long clients, long bookings, BigDecimal revenue, long unlinkedBookings,
                         long unconfirmedPrices, List<Series> series) {}
    private record Counts(long clients, long bookings, BigDecimal revenue) {}

    public Report report(LocalDate start, LocalDate end, String mode) {
        BusinessDates.period(start, "day");
        BusinessDates.period(end, "day");
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= 366
                || !List.of("actual", "plan").contains(mode)) {
            throw new IllegalArgumentException("Wybierz okres do 366 dni i tryb podsumowania.");
        }
        var counts = new LinkedHashMap<String, Counts>();
        CustomerAcquisitionSource.LABELS.keySet().forEach(code -> counts.put(code, new Counts(0, 0, BigDecimal.ZERO)));
        // One row per booking before aggregation: service items must never multiply revenue or customers.
        var rows = db.queryForList("""
                SELECT COALESCE(c.source,'UNKNOWN') AS source, COUNT(DISTINCT c.id) AS clients,
                       COUNT(*) AS bookings,
                       COALESCE(SUM(CASE WHEN ?='plan' THEN b.total ELSE COALESCE(b.final_total,b.total) END),0) AS revenue,
                       COUNT(*) FILTER(WHERE c.id IS NULL) AS unlinked,
                       COUNT(*) FILTER(WHERE b.status='DONE' AND b.final_total IS NULL) AS unconfirmed
                FROM booking b LEFT JOIN crm_customer c ON c.id=b.customer_id
                WHERE b.removed_at IS NULL AND b.day BETWEEN ? AND ? AND b.status<>'CANCELLED'
                  AND (?='plan' OR b.status='DONE')
                GROUP BY COALESCE(c.source,'UNKNOWN')
                """, mode, start, end, mode);
        long clients = 0, bookings = 0, unlinked = 0, unconfirmed = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (var row : rows) {
            String code = (String) row.get("source");
            long rowClients = ((Number) row.get("clients")).longValue();
            long rowBookings = ((Number) row.get("bookings")).longValue();
            BigDecimal rowRevenue = (BigDecimal) row.get("revenue");
            counts.put(code, new Counts(rowClients, rowBookings, rowRevenue));
            clients += rowClients;
            bookings += rowBookings;
            revenue = revenue.add(rowRevenue);
            unlinked += ((Number) row.get("unlinked")).longValue();
            unconfirmed += ((Number) row.get("unconfirmed")).longValue();
        }
        var series = new ArrayList<Series>();
        series.add(series("clients", "Klienci", false, BigDecimal.valueOf(clients), counts));
        series.add(series("bookings", "Zlecenia", false, BigDecimal.valueOf(bookings), counts));
        series.add(series("revenue", "Przychód", true, revenue, counts));
        return new Report(clients, bookings, revenue, unlinked, unconfirmed, List.copyOf(series));
    }

    private static Series series(String key, String label, boolean money, BigDecimal total,
                                 LinkedHashMap<String, Counts> counts) {
        var bars = new ArrayList<Bar>();
        CustomerAcquisitionSource.LABELS.forEach((code, name) -> {
            var count = counts.get(code);
            BigDecimal value = switch (key) {
                case "clients" -> BigDecimal.valueOf(count.clients());
                case "bookings" -> BigDecimal.valueOf(count.bookings());
                default -> count.revenue();
            };
            BigDecimal share = total.signum() == 0 ? BigDecimal.ZERO
                    : value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            bars.add(new Bar(code, name, value, share));
        });
        // Stable order for ties follows the shared source vocabulary.
        bars.sort(Comparator.comparing(Bar::value).reversed());
        return new Series(key, label, money, total, List.copyOf(bars));
    }
}
