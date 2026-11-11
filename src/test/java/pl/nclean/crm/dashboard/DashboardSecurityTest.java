package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(DashboardController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class DashboardSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean DashboardService service;
 @Test void dashboardCostsRequireOwnerAndCsrf() throws Exception {
  for(String path:new String[]{"/dashboard","/settings/costs"}){mvc.perform(get(path)).andExpect(status().is3xxRedirection());mvc.perform(get(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
  for(String path:new String[]{"/settings/costs/month","/settings/costs/fuel"}){mvc.perform(post(path).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}
 }
}
