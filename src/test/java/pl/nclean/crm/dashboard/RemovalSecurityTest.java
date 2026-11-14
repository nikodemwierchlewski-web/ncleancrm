package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.removal.*;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(RemovalController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class RemovalSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean RemovalService removal;
 @Test void removalAndRestoreRequireOwnerAndCsrf() throws Exception {
  mvc.perform(post("/manage/remove").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/manage/remove").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/manage/remove").with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
 }
 @Test void confirmationIsRequiredBeforeCallingRemoval() throws Exception {
  mvc.perform(post("/manage/remove").with(user("owner").roles("OWNER")).with(csrf()).param("kind","booking").param("id","1").param("version","0")).andExpect(status().isBadRequest());org.mockito.Mockito.verifyNoInteractions(removal);
 }
}
