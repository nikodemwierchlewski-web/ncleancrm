package pl.nclean.crm.help;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
public class GuideController {
    private final GuideService guide;
    public GuideController(GuideService guide){this.guide=guide;}
    @GetMapping("/help/context") public ResponseEntity<Map<String,Object>> context(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) Long order,jakarta.servlet.http.HttpServletRequest request){
        var result=new java.util.LinkedHashMap<String,Object>(guide.context(date,order));
        for(String key:java.util.List.of("reminder","focused"))if(result.get(key) instanceof Map<?,?> row){
            var copy=new java.util.LinkedHashMap<String,Object>();row.forEach((k,v)->copy.put(k.toString(),v));
            copy.put("url",request.getContextPath()+copy.get("url"));result.put(key,copy);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
}
