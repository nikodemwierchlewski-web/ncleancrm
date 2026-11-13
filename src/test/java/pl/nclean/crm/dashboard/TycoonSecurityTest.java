package pl.nclean.crm.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.tycoon.*;
import pl.nclean.crm.notes.NoteService;
import pl.nclean.crm.work.DayReviewService;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TycoonController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class TycoonSecurityTest {
 @Autowired MockMvc mvc;
 @MockitoBean AchievementService awards;
 @MockitoBean NoteService notes;
 @MockitoBean DayReviewService days;
 @Test void progressAndNotificationRequireOwnerAndCsrf() throws Exception {
  mvc.perform(get("/tycoon/progress")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/tycoon/progress").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
  String path="/tycoon/achievements/FIRST_CLEAN/ack";
  mvc.perform(post(path).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("owner").roles("OWNER")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.claimed").value(false));
 }
}
