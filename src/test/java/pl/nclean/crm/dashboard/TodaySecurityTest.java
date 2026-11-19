package pl.nclean.crm.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.work.*;
import pl.nclean.crm.notes.*;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TodayController.class,ReminderController.class})
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class TodaySecurityTest {
 @MockitoBean pl.nclean.crm.costs.EquipmentService equipment;
 @Autowired MockMvc mvc;
 @MockitoBean WorkService work;@MockitoBean DayReviewService reviews;@MockitoBean DashboardService dashboard;@MockitoBean NoteService notes;@MockitoBean ReminderService reminders;
 @Test void todayAndRemindersRequireOwner() throws Exception {
  for(String p:new String[]{"/today","/notes/reminders"}){mvc.perform(get(p)).andExpect(status().is3xxRedirection());mvc.perform(get(p).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());}
 }
 @Test void reminderMutationRequiresCsrfAndOwner() throws Exception {
  mvc.perform(post("/notes/reminders/1").with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
  mvc.perform(post("/notes/reminders/1").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
 }
}
