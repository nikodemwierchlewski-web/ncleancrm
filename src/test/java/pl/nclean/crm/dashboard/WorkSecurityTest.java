package pl.nclean.crm.dashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.work.*;
import pl.nclean.crm.records.CrmRecords;
import pl.nclean.crm.calendar.CalendarService;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest({WorkController.class,ExportController.class})
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class WorkSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean WorkService work;
 @MockitoBean DayReviewService review;
 @MockitoBean pl.nclean.crm.context.ContextService context;
 @MockitoBean CrmRecords records;
 @MockitoBean DashboardService dashboard;
 @MockitoBean CalendarService calendar;
 @MockitoBean org.springframework.jdbc.core.JdbcTemplate db;
 @Test void ownerAndCsrfProtectTheWholeWorkday() throws Exception {
  for(String path:new String[]{"/today","/work/1","/customers/1","/export/orders.csv","/export/customers.csv"}){mvc.perform(get(path)).andExpect(status().is3xxRedirection());mvc.perform(get(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
  for(String path:new String[]{"/work/1/timer","/work/1/complete","/work/1/payments","/work/1/payments/1/reverse"}){mvc.perform(post(path).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}
 }
 @Test void csvNeutralizesFormulasAndEscapesQuotesAndMultilineCells(){for(String value:new String[]{"=SUM(A1)","+48500000000","  @cmd","-1","\tformula"})assertTrue(ExportController.cell(value).startsWith("\"'"));assertEquals("\"a;\"\"b\"\"\\nc\"".replace("\\n","\n"),ExportController.cell("a;\"b\"\nc"));}
}
