package pl.nclean.crm.removal;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class RemovalController {
 private final RemovalService removal;
 public RemovalController(RemovalService removal){this.removal=removal;}
 @PostMapping(value="/manage/remove",produces="application/json") @ResponseBody
 ResponseEntity<?> change(@RequestParam String kind,@RequestParam long id,@RequestParam long version,@RequestParam(defaultValue="false") boolean restore,@RequestParam(defaultValue="false") boolean confirmed,Principal actor){
  if(!confirmed)return ResponseEntity.badRequest().body(Map.of("message","Potwierdź wybraną operację."));
  try{return ResponseEntity.ok(removal.change(kind,id,version,restore,actor.getName()));}
  catch(IllegalStateException ex){return ResponseEntity.status(409).body(Map.of("message",ex.getMessage()));}
  catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}
 }
}
