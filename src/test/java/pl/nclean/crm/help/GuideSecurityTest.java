package pl.nclean.crm.help;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.security.SecurityConfiguration;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(GuideController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class GuideSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean GuideService guide;
 @Test void helpIsOwnerOnlyAndNeverPubliclyCached() throws Exception {
  mvc.perform(get("/help/context")).andExpect(status().is3xxRedirection());mvc.perform(get("/help/context").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());when(guide.context(null,null)).thenReturn(Map.of("total",0));mvc.perform(get("/help/context").with(user("owner").roles("OWNER"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
 }
}
