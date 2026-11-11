package pl.nclean.crm.context;
import java.util.*;
import java.time.*;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
@Controller
public class ContextController {
 private final ContextService context;
 public ContextController(ContextService context){this.context=context;}
 @GetMapping("/search") String search(@RequestParam(defaultValue="") String q,Model m){m.addAttribute("query",q);m.addAttribute("results",context.search(q));return "context/search";}
 @GetMapping("/callbacks") String callbacks(Model m){var customers=context.callbacks();m.addAttribute("customers",customers);var tokens=new HashMap<Long,UUID>();customers.forEach(c->tokens.put(((Number)c.get("id")).longValue(),UUID.randomUUID()));m.addAttribute("contactTokens",tokens);m.addAttribute("next",pl.nclean.crm.dashboard.DashboardService.today().plusMonths(1));m.addAttribute("token",UUID.randomUUID());return "context/callbacks";}
 @PostMapping("/callbacks/{id}") String contact(@PathVariable long id,@RequestParam long version,@RequestParam UUID token,@RequestParam LocalDate next,@RequestParam(defaultValue="") String note,Model m,HttpServletResponse response){try{context.contact(id,version,token,next,note);return "redirect:/callbacks?saved=1";}catch(IllegalArgumentException ex){response.setStatus(400);m.addAttribute("error",ex.getMessage());return callbacks(m);}}
 @PostMapping("/work/{id}/photos") @ResponseBody ResponseEntity<Map<String,Object>> upload(@PathVariable long id,@RequestParam String stage,@RequestParam UUID token,@RequestParam MultipartFile image,@RequestParam long version,Principal actor){try{var photo=context.upload(id,stage,token,image,actor.getName(),version);return ResponseEntity.ok(Map.of("id",photo,"saved",true,"version",version+1));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}}
 @PostMapping("/work/{id}/photos/{photo}/delete") String delete(@PathVariable long id,@PathVariable UUID photo,Principal actor){context.delete(id,photo,actor.getName());return "redirect:/work/"+id+"#photos";}
 @PostMapping("/work/{id}/photo-time") @ResponseBody ResponseEntity<Map<String,Object>> manual(@PathVariable long id,@RequestParam long version,@RequestParam String start,@RequestParam String end,Principal actor){try{return ResponseEntity.ok(Map.of("version",context.manualTime(id,version,LocalDateTime.parse(start),LocalDateTime.parse(end),actor.getName())));}catch(IllegalArgumentException|DateTimeException ex){return ResponseEntity.badRequest().body(Map.of("error","Sprawdź godziny, datę i wersję zlecenia. "+ex.getMessage()));}}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseBody ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()==null?"Sprawdź dane.":ex.getMessage()));}
 @GetMapping("/photos/{id}") ResponseEntity<byte[]> image(@PathVariable UUID id){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline; filename=service-"+id+".jpg").body(context.image(id));}
}
