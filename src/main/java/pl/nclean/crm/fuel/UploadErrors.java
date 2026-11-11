package pl.nclean.crm.fuel;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class UploadErrors {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.badRequest().body(Map.of("error","Zdjęcie jest za duże. Wybierz JPEG lub PNG do 8 MB."));}
}
