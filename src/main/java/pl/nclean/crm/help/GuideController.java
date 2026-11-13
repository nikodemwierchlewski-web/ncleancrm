package pl.nclean.crm.help;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
public class GuideController {
    private final GuideService guide;
    public GuideController(GuideService guide){this.guide=guide;}
    @GetMapping("/help/context") public ResponseEntity<Map<String,Object>> context(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) Long order){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(guide.context(date,order));
    }
}
