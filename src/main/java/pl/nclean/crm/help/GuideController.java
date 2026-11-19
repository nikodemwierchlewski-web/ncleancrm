package pl.nclean.crm.help;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
public class GuideController {
    private final GuideService guide;
    private final pl.nclean.crm.costs.EquipmentService equipment;
    public GuideController(GuideService guide,pl.nclean.crm.costs.EquipmentService equipment){this.guide=guide;this.equipment=equipment;}
    @GetMapping("/help/context") public ResponseEntity<Map<String,Object>> context(@RequestParam(required=false) LocalDate date,@RequestParam(required=false) Long order,jakarta.servlet.http.HttpServletRequest request){
        var result=new java.util.LinkedHashMap<String,Object>(guide.context(date,order));
        var attention=equipment.attention(pl.nclean.crm.shared.BusinessDates.today());
        if(!attention.isEmpty()){var first=attention.get(0);result.put("equipmentReminder",java.util.Map.of("id","equipment-plan:"+first.id(),"title","Przeglądy sprzętu","message",first.assetName()+": "+first.title()+". "+first.label()+".","url","/today#equipment-maintenance","missing",attention.size(),"date",pl.nclean.crm.shared.BusinessDates.today().toString()));}
        for(String key:java.util.List.of("reminder","focused","equipmentReminder"))if(result.get(key) instanceof Map<?,?> row){
            var copy=new java.util.LinkedHashMap<String,Object>();row.forEach((k,v)->copy.put(k.toString(),v));
            copy.put("url",request.getContextPath()+copy.get("url"));result.put(key,copy);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
}
