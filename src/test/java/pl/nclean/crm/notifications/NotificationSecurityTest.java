package pl.nclean.crm.notifications;

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

@WebMvcTest(NotificationController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties={"crm.admin.username=test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class NotificationSecurityTest {
 @Autowired MockMvc mvc;@MockitoBean EntityReminderService service;@MockitoBean pl.nclean.crm.notes.ReminderService legacy;
 @Test void ownerOnlyAndSessionRequired() throws Exception {for(String p:new String[]{"/api/v1/notifications","/api/v1/reminders?sourceType=MEMO&sourceId=1"}){mvc.perform(get(p)).andExpect(status().is3xxRedirection());mvc.perform(get(p).with(user("staff").roles("EMPLOYEE"))).andExpect(status().isForbidden());}}
 @Test void csrfAndRoleOnAllWrites() throws Exception {for(String p:new String[]{"/api/v1/reminders","/api/v1/notifications/00000000-0000-0000-0000-000000000001"}){mvc.perform(post(p).with(user("owner").roles("OWNER"))).andExpect(status().isForbidden());mvc.perform(post(p).with(user("staff").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());}}
}
