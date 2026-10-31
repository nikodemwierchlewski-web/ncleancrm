package pl.nclean.crm.calendar;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.nclean.crm.security.SecurityConfiguration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalendarController.class)
@Import(SecurityConfiguration.class)
class CalendarSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean CalendarService service;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("crm.admin.username", () -> "test-owner");
        properties.add("crm.admin.password-hash", () -> new BCryptPasswordEncoder().encode("test-only-password"));
    }
    @Test void anonymousCannotReadCalendar() throws Exception {
        mvc.perform(get("/calendar")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(service);
    }
    @Test void writeRequiresCsrf() throws Exception {
        mvc.perform(post("/calendar/district").with(user("owner").roles("OWNER"))
            .param("date", "2026-10-28").param("districtId", "1")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void wrongRoleCannotReadCalendar() throws Exception {
        mvc.perform(get("/calendar").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void ownerCanReadRenderedCalendar() throws Exception {
        when(service.days(any())).thenReturn(List.of(new CalendarService.Day(java.time.LocalDate.of(2026,10,28), "Krzyki", "#D1FAE5")));
        when(service.districts()).thenReturn(List.of(new CalendarService.District(1, "Krzyki", "#D1FAE5")));
        mvc.perform(get("/calendar").param("month", "2026-10").with(user("owner").roles("OWNER")))
            .andExpect(status().isOk()).andExpect(view().name("calendar/month"));
    }
    @Test void ownerCanAssignDay() throws Exception {
        mvc.perform(post("/calendar/district").with(user("owner").roles("OWNER")).with(csrf())
            .param("date", "2026-10-28").param("districtId", "1"))
            .andExpect(redirectedUrl("/calendar?month=2026-10"));
        verify(service).assign(java.time.LocalDate.of(2026,10,28), 1L);
    }
}
