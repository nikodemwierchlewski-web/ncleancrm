package pl.nclean.crm.work;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only reporting projection. HTTP and CSV encoding stay in the adapter. */
@Service
@Transactional(readOnly = true)
public class ExportQueries {
    private final JdbcTemplate db;

    public ExportQueries(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> customers() {
        return db.queryForList("""
                SELECT id,name,phone,address,notes,active,source,referral_note,last_contact,contact_due
                FROM crm_customer ORDER BY id
                """);
    }

    public List<Map<String, Object>> orders() {
        return db.queryForList("""
                SELECT b.id,b.day,b.starts,b.customer_name,b.phone,b.address,b.status,
                  b.total quoted_total,b.final_total,b.work_minutes,b.work_note,b.completion_note,
                  b.completion_no_notes,b.photo_before_exception,b.photo_after_exception,b.review_reason,
                  COALESCE((SELECT SUM(amount) FROM booking_payment WHERE booking_id=b.id),0) paid
                FROM booking b WHERE b.removed_at IS NULL ORDER BY day,id
                """);
    }
}
