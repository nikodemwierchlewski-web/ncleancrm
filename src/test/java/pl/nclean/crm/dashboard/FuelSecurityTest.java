package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.fuel.*;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(FuelController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class FuelSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean FuelJournalService fuel;
 @MockitoBean ReceiptService receipts;
 @Test void journalAndImagesArePrivateAndWritesRequireCsrf() throws Exception {
  for(String path:new String[]{"/fuel","/fuel/receipts/00000000-0000-0000-0000-000000000001"}){mvc.perform(get(path)).andExpect(status().is3xxRedirection());mvc.perform(get(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
  for(String path:new String[]{"/fuel/day","/fuel/parameters","/fuel/purchases","/fuel/receipts","/fuel/receipts/00000000-0000-0000-0000-000000000001/discard"}){mvc.perform(post(path).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}
 }
}
