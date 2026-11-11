package pl.nclean.crm.dashboard;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pl.nclean.crm.context.ContextService;
class PhotoNameTest {
 @Test void parsesOnlySupportedFullTimestamps(){assertEquals(LocalDateTime.of(2026,11,11,10,30,25),ContextService.filenameTime("IMG_20261111_103025.jpg").orElseThrow());assertTrue(ContextService.filenameTime("PXL_20261111_103025.123.jpg").isPresent());assertTrue(ContextService.filenameTime("2026-11-11_10-30-25.png").isPresent());}
 @Test void unknownAndInvalidNamesStayUnknown(){for(String n:new String[]{"IMG_1234.jpg","image.jpg","IMG_20260230_103025.jpg","IMG_20261111_253025.jpg"})assertTrue(ContextService.filenameTime(n).isEmpty(),n);}
}
