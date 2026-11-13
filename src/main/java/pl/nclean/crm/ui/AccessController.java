package pl.nclean.crm.ui;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.*;
@Controller
public class AccessController {
 @GetMapping("/login") public String login(){return "access/login";}
 @GetMapping("/logout") public String logout(){return "access/logout";}
 @GetMapping("/session") @ResponseBody public ResponseEntity<Map<String,String>> session(CsrfToken csrf){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("parameter",csrf.getParameterName(),"token",csrf.getToken()));}
}
