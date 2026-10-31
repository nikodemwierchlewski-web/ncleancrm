package pl.nclean.crm.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CalendarController.class, AreaSettingsController.class})
@Import(SecurityConfiguration.class)
class CalendarSecurityTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 28);
    private static final List<CalendarService.District> AREAS = List.of(
        new CalendarService.District(1, "Krzyki", "#D1FAE5", true),
        new CalendarService.District(2, "Fabryczna", "#DBEAFE", true));
    @Autowired MockMvc mvc;
    @MockitoBean CalendarService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("crm.admin.username", () -> "test-owner");
        properties.add("crm.admin.password-hash", () -> new BCryptPasswordEncoder().encode("test-only-password"));
    }

    @BeforeEach
    void calendarData() {
        when(service.days(any())).thenAnswer(invocation -> {
            YearMonth month = invocation.getArgument(0);
            return month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
                .map(day -> new CalendarService.Day(day, day.equals(DAY) ? AREAS : List.of())).toList();
        });
        when(service.districts()).thenReturn(AREAS);
        when(service.allDistricts()).thenReturn(AREAS);
        when(service.assignedAreaIds(any())).thenAnswer(invocation -> DAY.equals(invocation.getArgument(0)) ? Set.of(1L, 2L) : Set.of());
    }

    @Test void anonymousCannotReadCalendarOrSettings() throws Exception {
        mvc.perform(get("/calendar")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/settings/areas")).andExpect(status().is3xxRedirection());
        verify(service, never()).days(any());
        verify(service, never()).allDistricts();
    }

    @Test void employeeCannotReadOrChangeAreas() throws Exception {
        for (String path : List.of("/calendar", "/settings/areas")) {
            mvc.perform(get(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/calendar/areas").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("date", DAY.toString()).param("districtIds", "1", "2")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("name", "Osiedle").param("color", "#123456")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas/1").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("name", "Osiedle").param("color", "#123456")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas/1/active").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("active", "false")).andExpect(status().isForbidden());
        verify(service, never()).assign(any(), anyList());
        verify(service, never()).createArea(any());
        verify(service, never()).updateArea(anyLong(), any());
        verify(service, never()).setAreaActive(anyLong(), anyBoolean());
    }

    @Test void allWritesRequireCsrf() throws Exception {
        mvc.perform(post("/calendar/areas").with(user("owner").roles("OWNER"))
            .param("date", DAY.toString()).param("districtIds", "1", "2")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas").with(user("owner").roles("OWNER"))
            .param("name", "Osiedle").param("color", "#123456")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas/1").with(user("owner").roles("OWNER"))
            .param("name", "Osiedle").param("color", "#123456")).andExpect(status().isForbidden());
        mvc.perform(post("/settings/areas/1/active").with(user("owner").roles("OWNER"))
            .param("active", "false")).andExpect(status().isForbidden());
        verify(service, never()).assign(any(), anyList());
        verify(service, never()).createArea(any());
        verify(service, never()).updateArea(anyLong(), any());
        verify(service, never()).setAreaActive(anyLong(), anyBoolean());
    }

    @Test void ownerCanReadRenderedCalendarWithSelectedAreas() throws Exception {
        mvc.perform(get("/calendar").param("month", "2026-10").param("day", DAY.toString())
            .with(user("owner").roles("OWNER")))
            .andExpect(status().isOk()).andExpect(view().name("calendar/month"))
            .andExpect(model().attribute("selectedDate", DAY))
            .andExpect(model().attribute("selectedAreaIds", Set.of(1L, 2L)));
    }

    @Test void ownerCanAssignSeveralAreasAndClearThem() throws Exception {
        mvc.perform(post("/calendar/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("date", DAY.toString()).param("districtIds", "1", "2"))
            .andExpect(redirectedUrl("/calendar?month=2026-10&day=2026-10-28&saved=1"));
        verify(service).assign(DAY, List.of(1L, 2L));
        mvc.perform(post("/calendar/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("date", DAY.toString()))
            .andExpect(redirectedUrl("/calendar?month=2026-10&day=2026-10-28&saved=1"));
        verify(service).assign(DAY, List.of());
    }

    @Test void invalidDayCannotReachAssignmentService() throws Exception {
        mvc.perform(post("/calendar/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("date", "1999-12-31").param("districtIds", "1"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/calendar/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("date", "not-a-date").param("districtIds", "1"))
            .andExpect(status().isBadRequest());
        verify(service, never()).assign(any(), anyList());
    }

    @Test void ownerCanReadAreaSettings() throws Exception {
        mvc.perform(get("/settings/areas").with(user("owner").roles("OWNER")))
            .andExpect(status().isOk());
        verify(service).allDistricts();
    }

    @Test void ownerCanCreateAndEditArea() throws Exception {
        when(service.createArea(any())).thenReturn(6L);
        mvc.perform(post("/settings/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("name", "Osiedle testowe").param("color", "#123456"))
            .andExpect(redirectedUrl("/settings/areas?saved=1"));
        verify(service).createArea(argThat(form -> form.getName().equals("Osiedle testowe") && form.getColor().equals("#123456")));
        mvc.perform(post("/settings/areas/1").with(user("owner").roles("OWNER")).with(csrf())
            .param("name", "Nowa nazwa").param("color", "#ABCDEF"))
            .andExpect(redirectedUrl("/settings/areas?saved=1"));
        verify(service).updateArea(eq(1L), argThat(form -> form.getName().equals("Nowa nazwa") && form.getColor().equals("#ABCDEF")));
    }

    @Test void invalidAreaSettingsAreRenderedAsErrorsWithoutWrite() throws Exception {
        mvc.perform(post("/settings/areas").with(user("owner").roles("OWNER")).with(csrf())
            .param("name", " ").param("color", "red"))
            .andExpect(status().isBadRequest());
        verify(service, never()).createArea(any());
    }

    @Test void ownerCanDeactivateArea() throws Exception {
        mvc.perform(post("/settings/areas/1/active").with(user("owner").roles("OWNER")).with(csrf())
            .param("active", "false"))
            .andExpect(redirectedUrl("/settings/areas?saved=1"));
        verify(service).setAreaActive(1L, false);
    }
}
