package pl.nclean.crm.calendar;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pl.nclean.crm.CrmApplication;

import static org.junit.jupiter.api.Assertions.*;

/** Actual HTTP, Security, Flyway, Hibernate and PostgreSQL; never an external datasource. */
@Testcontainers
class CalendarPostgresIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse(
        "postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f"))
        .withDatabaseName("calendar_integration")
        .withUsername("calendar_test")
        .withPassword(UUID.randomUUID().toString());

    private static final String OWNER = "integration-owner";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder(12).encode(PASSWORD);
    private static final String DAY = "2026-10-28";
    private RunningApp app;
    private JdbcTemplate jdbc;

    private static final List<String> SEED_NAMES = List.of("Fabryczna", "Krzyki", "Psie Pole", "Stare Miasto", "Śródmieście");
    private static final List<String> SEED_COLORS = List.of("#DBEAFE", "#D1FAE5", "#FEF3C7", "#FCE7F3", "#EDE9FE");

    @BeforeEach
    void startIsolatedApplication() {
        app = RunningApp.start();
        jdbc = app.context().getBean(JdbcTemplate.class);
        jdbc.update("DELETE FROM day_district");
        jdbc.update("DELETE FROM district WHERE id > 5");
        for (int i = 0; i < SEED_NAMES.size(); i++) {
            jdbc.update("UPDATE district SET name = ?, color = ?, active = TRUE WHERE id = ?", SEED_NAMES.get(i), SEED_COLORS.get(i), i + 1);
        }
    }

    @AfterEach
    void stopApplication() {
        if (app != null) app.close();
    }

    @Test
    void flywayCreatesLatestSchemaAndFiveOperationalAreas() {
        assertEquals(List.of("1", "2"), jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL' ORDER BY installed_rank", String.class));
        assertEquals(SEED_NAMES, jdbc.queryForList("SELECT name FROM district ORDER BY id", String.class));
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM district WHERE active = TRUE", Integer.class));
        assertEquals(List.of("day", "district_id"), jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'day_district' ORDER BY ordinal_position", String.class));
        assertTrue(jdbc.queryForObject("SELECT bool_and(color ~ '^#[0-9A-Fa-f]{6}$') FROM district", Boolean.class));
    }

    @Test
    void upgradingV1PreservesExistingAssignmentAndAllowsSecondAreaOnSameDay() {
        String schema = "legacy_upgrade";
        String schemaUrl = POSTGRES.getJdbcUrl() + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        var source = new DriverManagerDataSource(schemaUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
        var legacy = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("1").load().migrate();
        long first = legacy.queryForObject("SELECT id FROM district WHERE name = 'Fabryczna'", Long.class);
        long second = legacy.queryForObject("SELECT id FROM district WHERE name = 'Krzyki'", Long.class);
        legacy.update("INSERT INTO day_district(day, district_id) VALUES (?, ?)", java.sql.Date.valueOf(DAY), first);

        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals(List.of(first), legacy.queryForList("SELECT district_id FROM day_district WHERE day = DATE '2026-10-28'", Long.class));
        legacy.update("INSERT INTO day_district(day, district_id) VALUES (?, ?)", java.sql.Date.valueOf(DAY), second);
        assertEquals(List.of(first, second), legacy.queryForList("SELECT district_id FROM day_district WHERE day = DATE '2026-10-28' ORDER BY district_id", Long.class));
        assertEquals(5, legacy.queryForObject("SELECT count(*) FROM district", Integer.class));
        assertEquals(List.of("1", "2"), legacy.queryForList("SELECT version FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL' ORDER BY installed_rank", String.class));
    }

    @Test
    void ownerLogsInAndReadsMonthAndSelectedDayFromRealDatabase() throws Exception {
        var browser = app.browser();
        var protectedPage = browser.get("/calendar?month=2026-10");
        assertEquals(302, protectedPage.statusCode());
        assertTrue(protectedPage.headers().firstValue("Location").orElseThrow().endsWith("/login"));
        browser.login();
        var month = browser.get("/calendar?month=2026-10");
        assertEquals(200, month.statusCode());
        assertTrue(month.body().contains("2026-10-28"));
        var selected = browser.get(calendarPath(DAY));
        assertEquals(200, selected.statusCode());
        assertTrue(selected.body().contains("Krzyki"));
        assertTrue(selected.body().contains("districtIds"));
        assertTrue(selected.body().contains("name=\"_csrf\""));
    }

    @Test
    void ownerAssignsSeveralAreasDeduplicatesReplacesAndClears() throws Exception {
        var browser = app.browser(); browser.login();
        long first = areaId("Fabryczna"), second = areaId("Krzyki"), third = areaId("Psie Pole");
        assertCalendarRedirect(browser.assign(DAY, List.of(first, second, first), true), DAY);
        assertEquals(List.of(first, second), assignedAreas(DAY));
        var rendered = browser.get(calendarPath(DAY)).body();
        assertTrue(rendered.contains("Fabryczna") && rendered.contains("Krzyki"));
        assertCalendarRedirect(browser.assign(DAY, List.of(second, third), true), DAY);
        assertEquals(List.of(second, third), assignedAreas(DAY));
        assertCalendarRedirect(browser.assign(DAY, List.of(), true), DAY);
        assertEquals(List.of(), assignedAreas(DAY));
    }

    @Test
    void invalidUnauthorizedAndCsrfFreeWritesPreserveExistingAreas() throws Exception {
        long first = areaId("Fabryczna"), inactive = areaId("Krzyki");
        var browser = app.browser();
        assertEquals(403, browser.post("/calendar/areas", Map.of("date", DAY, "districtIds", first)).statusCode());
        browser.login();
        assertCalendarRedirect(browser.assign(DAY, List.of(first), true), DAY);
        assertEquals(403, browser.assign(DAY, List.of(inactive), false).statusCode());
        assertHtmlBadRequest(browser.assign(DAY, List.of(first, Long.MAX_VALUE), true));
        jdbc.update("UPDATE district SET active = FALSE WHERE id = ?", inactive);
        assertHtmlBadRequest(browser.assign(DAY, List.of(first, inactive), true));
        for (String invalidDate : List.of("1999-12-31", "2101-01-01", "not-a-date")) {
            assertHtmlBadRequest(browser.assign(invalidDate, List.of(first), true));
        }
        for (String invalidMonth : List.of("1999-12", "2101-01", "not-a-month")) {
            assertEquals(400, browser.get("/calendar?month=" + invalidMonth).statusCode());
        }
        assertEquals(400, browser.get("/calendar?month=2026-10&day=2101-01-01").statusCode());
        assertEquals(List.of(first), assignedAreas(DAY));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM day_district", Integer.class));
    }

    @Test
    void settingsCreateRenameRecolorAndDeactivateWithoutLosingAssignments() throws Exception {
        var browser = app.browser(); browser.login();
        assertSettingsRedirect(browser.settings("/settings/areas", Map.of("name", "Strefa testowa", "color", "#1A2B3C"), true));
        long created = areaId("Strefa testowa"), first = areaId("Fabryczna");
        assertCalendarRedirect(browser.assign(DAY, List.of(first, created), true), DAY);
        assertSettingsRedirect(browser.settings("/settings/areas/" + created, Map.of("name", "Osiedle testowe", "color", "#5E6F70"), true));
        assertEquals("#5E6F70", jdbc.queryForObject("SELECT color FROM district WHERE id = ?", String.class, created));
        assertSettingsRedirect(browser.settings("/settings/areas/" + created + "/active", Map.of("active", "false"), true));
        assertFalse(jdbc.queryForObject("SELECT active FROM district WHERE id = ?", Boolean.class, created));
        assertEquals(List.of(first, created), assignedAreas(DAY));
        var rendered = browser.get(calendarPath(DAY)).body();
        assertTrue(rendered.contains("Osiedle testowe"));
        assertTrue(rendered.contains("#5E6F70"));
        assertTrue(rendered.toLowerCase(java.util.Locale.ROOT).contains("nieaktywn"));
        // Retaining a historical inactive area is allowed; assigning it to another day is not.
        assertCalendarRedirect(browser.assign(DAY, List.of(first, created), true), DAY);
        assertHtmlBadRequest(browser.assign("2026-10-29", List.of(created), true));
        assertEquals(List.of(), assignedAreas("2026-10-29"));
        assertCalendarRedirect(browser.assign(DAY, List.of(), true), DAY);
        assertHtmlBadRequest(browser.assign(DAY, List.of(created), true));
        assertSettingsRedirect(browser.settings("/settings/areas/" + created + "/active", Map.of("active", "true"), true));
        assertCalendarRedirect(browser.assign(DAY, List.of(created), true), DAY);
    }

    @Test
    void settingsRejectInvalidNamesColorsDuplicatesUnknownIdsAndMissingCsrf() throws Exception {
        var browser = app.browser(); browser.login();
        assertEquals(403, browser.settings("/settings/areas", Map.of("name", "Nowa strefa", "color", "#123456"), false).statusCode());
        for (var invalid : List.of(
                Map.of("name", " ", "color", "#123456"),
                Map.of("name", "A".repeat(101), "color", "#123456"),
                Map.of("name", "Nowa strefa", "color", "red"),
                Map.of("name", "Nowa strefa", "color", "#123"),
                Map.of("name", "Nowa strefa", "color", "#1234567"),
                Map.of("name", "Nowa strefa", "color", "#GGGGGG"),
                Map.of("name", "fabryczna", "color", "#123456"))) {
            assertHtmlBadRequest(browser.settings("/settings/areas", invalid, true));
        }
        assertHtmlBadRequest(browser.settings("/settings/areas/" + areaId("Krzyki"), Map.of("name", "FABRYCZNA", "color", "#123456"), true));
        assertEquals(404, browser.get("/settings/areas?edit=" + Long.MAX_VALUE).statusCode());
        assertEquals(404, browser.settings("/settings/areas/" + Long.MAX_VALUE, Map.of("name", "Nowa strefa", "color", "#123456"), true).statusCode());
        assertEquals(404, browser.settings("/settings/areas/" + Long.MAX_VALUE + "/active", Map.of("active", "false"), true).statusCode());
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM district", Integer.class));
        assertEquals(SEED_NAMES, jdbc.queryForList("SELECT name FROM district ORDER BY id", String.class));
    }

    @Test
    void severalAreasWithEditedNameColorAndInactiveHistorySurviveApplicationRestart() throws Exception {
        var browser = app.browser(); browser.login();
        assertSettingsRedirect(browser.settings("/settings/areas", Map.of("name", "Przed zmianą", "color", "#123456"), true));
        long created = areaId("Przed zmianą"), first = areaId("Fabryczna");
        assertCalendarRedirect(browser.assign(DAY, List.of(first, created), true), DAY);
        assertSettingsRedirect(browser.settings("/settings/areas/" + created, Map.of("name", "Po zmianie", "color", "#654321"), true));
        assertSettingsRedirect(browser.settings("/settings/areas/" + created + "/active", Map.of("active", "false"), true));
        var previousContext = app.context(); app.close(); assertFalse(previousContext.isActive());
        app = RunningApp.start(); jdbc = app.context().getBean(JdbcTemplate.class);
        assertEquals(List.of(first, created), assignedAreas(DAY));
        assertEquals("Po zmianie", jdbc.queryForObject("SELECT name FROM district WHERE id = ?", String.class, created));
        assertEquals("#654321", jdbc.queryForObject("SELECT color FROM district WHERE id = ?", String.class, created));
        assertFalse(jdbc.queryForObject("SELECT active FROM district WHERE id = ?", Boolean.class, created));
        var newSession = app.browser(); assertEquals(302, newSession.get(calendarPath(DAY)).statusCode()); newSession.login();
        var page = newSession.get(calendarPath(DAY)); assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Po zmianie") && page.body().contains("#654321"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL'", Integer.class));
    }

    private long areaId(String name) { return jdbc.queryForObject("SELECT id FROM district WHERE name = ?", Long.class, name); }
    private List<Long> assignedAreas(String day) {
        return jdbc.queryForList("SELECT district_id FROM day_district WHERE day = ? ORDER BY district_id", Long.class, java.sql.Date.valueOf(day));
    }
    private static String calendarPath(String day) { return "/calendar?month=2026-10&day=" + day; }
    private static void assertCalendarRedirect(HttpResponse<String> response, String day) {
        assertEquals(302, response.statusCode());
        var target = URI.create(response.headers().firstValue("Location").orElseThrow());
        assertEquals("/calendar", target.getPath());
        assertEquals("month=" + day.substring(0, 7) + "&day=" + day + "&saved=1", target.getQuery());
    }
    private static void assertSettingsRedirect(HttpResponse<String> response) {
        assertEquals(302, response.statusCode());
        var target = URI.create(response.headers().firstValue("Location").orElseThrow());
        assertEquals("/settings/areas", target.getPath()); assertEquals("saved=1", target.getQuery());
    }
    private static void assertHtmlBadRequest(HttpResponse<String> response) {
        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    private record RunningApp(ConfigurableApplicationContext context, URI base) implements AutoCloseable {
        static RunningApp start() {
            // Do not inherit database URLs, active profiles, or config files from the caller.
            var environment = new StandardEnvironment();
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().addFirst(new MapPropertySource("isolated-integration-test", Map.ofEntries(
                Map.entry("spring.config.location", "classpath:/application.yml"),
                Map.entry("spring.profiles.active", "integration"),
                Map.entry("spring.datasource.url", POSTGRES.getJdbcUrl()),
                Map.entry("spring.datasource.username", POSTGRES.getUsername()),
                Map.entry("spring.datasource.password", POSTGRES.getPassword()),
                Map.entry("crm.admin.username", OWNER),
                Map.entry("crm.admin.password-hash", PASSWORD_HASH),
                Map.entry("server.address", "127.0.0.1"),
                Map.entry("server.port", "0"),
                Map.entry("spring.main.banner-mode", "off"))));
            var context = new SpringApplicationBuilder(CrmApplication.class)
                .environment(environment).web(WebApplicationType.SERVLET).registerShutdownHook(false).run();
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            return new RunningApp(context, URI.create("http://127.0.0.1:" + port));
        }

        Browser browser() { return new Browser(base); }
        @Override public void close() { context.close(); }
    }

    private static final class Browser {
        private final URI base;
        private final HttpClient http;

        Browser(URI base) {
            this.base = base;
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        }

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return http.send(HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        HttpResponse<String> post(String path, Map<String, ?> form) throws IOException, InterruptedException {
            var pairs = new ArrayList<String>();
            form.forEach((name, value) -> {
                if (value instanceof Iterable<?> values) values.forEach(item -> pairs.add(encode(name) + "=" + encode(item.toString())));
                else pairs.add(encode(name) + "=" + encode(value.toString()));
            });
            String body = String.join("&", pairs);
            return http.send(HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        void login() throws IOException, InterruptedException {
            var page = get("/login");
            assertEquals(200, page.statusCode());
            var response = post("/login", Map.of("username", OWNER, "password", PASSWORD, "_csrf", csrf(page.body())));
            assertEquals(302, response.statusCode());
            assertFalse(response.headers().firstValue("Location").orElseThrow().contains("error"));
            assertEquals(200, get("/calendar?month=2026-10").statusCode());
        }

        HttpResponse<String> assign(String day, List<Long> areaIds, boolean includeCsrf) throws IOException, InterruptedException {
            var fields = new LinkedHashMap<String, Object>(); fields.put("date", day);
            if (!areaIds.isEmpty()) fields.put("districtIds", areaIds);
            return settings("/calendar/areas", fields, includeCsrf);
        }

        HttpResponse<String> settings(String path, Map<String, ?> fields, boolean includeCsrf) throws IOException, InterruptedException {
            var form = new LinkedHashMap<String, Object>(fields);
            if (includeCsrf) {
                var page = get(calendarPath(DAY)); assertEquals(200, page.statusCode());
                form.put("_csrf", csrf(page.body()));
            }
            return post(path, form);
        }

        private static String csrf(String html) {
            var matcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
            assertTrue(matcher.find(), "The real login/calendar form must include its CSRF token");
            return matcher.group(1);
        }

        private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    }
}
