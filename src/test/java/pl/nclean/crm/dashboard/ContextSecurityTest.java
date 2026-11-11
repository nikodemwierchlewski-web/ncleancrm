package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.context.*;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ContextController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class ContextSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean ContextService context;
 @Test void newContextAndImagesRequireOwnerAndCsrf() throws Exception {
  for(String p:new String[]{"/search","/callbacks","/photos/00000000-0000-0000-0000-000000000001"}){mvc.perform(get(p)).andExpect(status().is3xxRedirection());mvc.perform(get(p).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
  for(String p:new String[]{"/callbacks/1","/work/1/photos","/work/1/photo-time","/work/1/photos/00000000-0000-0000-0000-000000000001/delete"}){mvc.perform(post(p).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(p).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}
 }
}
