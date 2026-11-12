package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.costs.*;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(CostController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class CostSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean CostService costs;@MockitoBean DashboardService dashboard;@MockitoBean pl.nclean.crm.fuel.FuelJournalService fuel;@MockitoBean pl.nclean.crm.fuel.ReceiptService receipts;@MockitoBean pl.nclean.crm.records.ChemistryService chemistry;
 @Test void newContextAndImagesRequireOwnerAndCsrf() throws Exception {
  for(String p:new String[]{"/costs","/costs/editor","/costs/equipment/1"}){mvc.perform(get(p)).andExpect(status().is3xxRedirection());mvc.perform(get(p).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
  for(String p:new String[]{"/costs","/costs/1/cancel","/costs/fuel/1/cancel","/costs/equipment/1","/costs/receipts"}){mvc.perform(post(p).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(p).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}
 }
}
