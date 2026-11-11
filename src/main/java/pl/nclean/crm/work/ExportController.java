package pl.nclean.crm.work;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller
public class ExportController {
    private final JdbcTemplate db;
    public ExportController(JdbcTemplate db){this.db=db;}
    public static String cell(Object value){String s=value==null?"":value.toString();String trimmed=s.stripLeading();if(s.startsWith("\t")||s.startsWith("\r")||s.startsWith("\n")||!trimmed.isEmpty()&&"=+-@".indexOf(trimmed.charAt(0))>=0)s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private ResponseEntity<byte[]> csv(String filename,List<Map<String,Object>> rows,List<String> columns){StringBuilder text=new StringBuilder("\ufeff");text.append(columns.stream().map(ExportController::cell).collect(java.util.stream.Collectors.joining(";"))).append("\r\n");for(var row:rows)text.append(columns.stream().map(c->cell(row.get(c))).collect(java.util.stream.Collectors.joining(";"))).append("\r\n");return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).cacheControl(CacheControl.noStore()).header("Content-Disposition","attachment; filename="+filename).body(text.toString().getBytes(StandardCharsets.UTF_8));}
    @GetMapping("/export/customers.csv") ResponseEntity<byte[]> customers(){return csv("nclean-klienci.csv",db.queryForList("SELECT id,name,phone,address,notes,active FROM crm_customer ORDER BY id"),List.of("id","name","phone","address","notes","active"));}
    @GetMapping("/export/orders.csv") ResponseEntity<byte[]> orders(){return csv("nclean-zlecenia.csv",db.queryForList("SELECT b.id,b.day,b.starts,b.customer_name,b.phone,b.address,b.status,b.total quoted_total,b.final_total,b.work_minutes,b.work_note,COALESCE((SELECT SUM(amount) FROM booking_payment WHERE booking_id=b.id),0) paid FROM booking b ORDER BY day,id"),List.of("id","day","starts","customer_name","phone","address","status","quoted_total","final_total","paid","work_minutes","work_note"));}
}
