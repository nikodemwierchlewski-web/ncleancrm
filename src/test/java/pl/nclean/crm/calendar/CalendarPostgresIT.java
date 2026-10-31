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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    @BeforeEach
    void startIsolatedApplication() {
        app = RunningApp.start();
        jdbc = app.context().getBean(JdbcTemplate.class);
        jdbc.update("DELETE FROM day_district");
        jdbc.update("UPDATE district SET active = TRUE");
    }

    @AfterEach
    void stopApplication() {
        if (app != null) app.close();
    }

    @Test
    void flywayCreatesSchemaAndExactlyFiveOperationalAreas() {
        assertEquals(List.of("1"), jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL'", String.class));
        assertEquals(List.of("Fabryczna", "Krzyki", "Psie Pole", "Stare Miasto", "Śródmieście"),
            jdbc.queryForList("SELECT name FROM district ORDER BY id", String.class));
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM district WHERE active = TRUE", Integer.class));
        assertEquals(List.of("day", "district_id"), jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'day_district' ORDER BY ordinal_position", String.class));
        assertTrue(jdbc.queryForObject("SELECT bool_and(color ~ '^#[0-9A-Fa-f]{6}$') FROM district", Boolean.class));
    }

    @Test
    void ownerLogsInAndReadsCalendarRenderedFromRealDatabase() throws Exception {
        var browser = app.browser();
        var protectedPage = browser.get("/calendar?month=2026-10");
        assertEquals(302, protectedPage.statusCode());
        assertTrue(protectedPage.headers().firstValue("Location").orElseThrow().endsWith("/login"));
        browser.login();
        var response = browser.get("/calendar?month=2026-10");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ncLEAN CRM"));
        assertTrue(response.body().contains("Krzyki"));
        assertEquals(31, Pattern.compile("<article\\b").matcher(response.body()).results().count());
        assertTrue(response.body().contains("name=\"_csrf\""));
    }

    @Test
    void ownerAssignsChangesAndRemovesAreaUsingCsrfProtectedForms() throws Exception {
        var browser = app.browser();
        browser.login();
        long first = areaId("Fabryczna"), second = areaId("Krzyki");
        assertRedirect(browser.assign(DAY, Long.toString(first), true));
        assertEquals(first, assignedArea());
        assertTrue(dayArticle(browser.get("/calendar?month=2026-10").body()).contains("<p>Fabryczna</p>"));
        assertRedirect(browser.assign(DAY, Long.toString(second), true));
        assertEquals(second, assignedArea());
        assertTrue(dayArticle(browser.get("/calendar?month=2026-10").body()).contains("<p>Krzyki</p>"));
        assertRedirect(browser.assign(DAY, "", true));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM day_district", Integer.class));
        assertTrue(dayArticle(browser.get("/calendar?month=2026-10").body()).contains("<p>Bez oznaczenia</p>"));
    }

    @Test
    void invalidUnauthorizedAndCsrfFreeWritesDoNotAlterPersistedAssignment() throws Exception {
        long validArea = areaId("Fabryczna"), inactiveArea = areaId("Krzyki");
        var browser = app.browser();
        // The CSRF filter rejects an anonymous write before controller access.
        assertEquals(403, browser.post("/calendar/district", Map.of("date", DAY, "districtId", Long.toString(validArea))).statusCode());
        browser.login();
        assertRedirect(browser.assign(DAY, Long.toString(validArea), true));
        assertEquals(403, browser.assign(DAY, Long.toString(inactiveArea), false).statusCode());
        assertEquals(400, browser.assign(DAY, Long.toString(Long.MAX_VALUE), true).statusCode());
        jdbc.update("UPDATE district SET active = FALSE WHERE id = ?", inactiveArea);
        assertEquals(400, browser.assign(DAY, Long.toString(inactiveArea), true).statusCode());
        for (String invalidDate : List.of("1999-12-31", "2101-01-01", "not-a-date")) {
            assertEquals(400, browser.assign(invalidDate, Long.toString(validArea), true).statusCode());
        }
        for (String invalidMonth : List.of("1999-12", "2101-01", "not-a-month")) {
            assertEquals(400, browser.get("/calendar?month=" + invalidMonth).statusCode());
        }
        assertEquals(validArea, assignedArea());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM day_district", Integer.class));
    }

    @Test
    void savedAssignmentSurvivesApplicationShutdownAndRestartAgainstSamePostgres() throws Exception {
        var browser = app.browser();
        browser.login();
        long id = areaId("Śródmieście");
        assertRedirect(browser.assign(DAY, Long.toString(id), true));
        var previousContext = app.context();
        app.close();
        assertFalse(previousContext.isActive());

        app = RunningApp.start();
        jdbc = app.context().getBean(JdbcTemplate.class);
        assertEquals(id, assignedArea());
        var newSession = app.browser();
        assertEquals(302, newSession.get("/calendar?month=2026-10").statusCode());
        newSession.login();
        assertTrue(dayArticle(newSession.get("/calendar?month=2026-10").body()).contains("<p>Śródmieście</p>"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class));
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM district", Integer.class));
    }

    private long areaId(String name) {
        return jdbc.queryForObject("SELECT id FROM district WHERE name = ?", Long.class, name);
    }

    private long assignedArea() {
        return jdbc.queryForObject("SELECT district_id FROM day_district WHERE day = DATE '2026-10-28'", Long.class);
    }

    private static void assertRedirect(HttpResponse<String> response) {
        assertEquals(302, response.statusCode());
        var target = URI.create(response.headers().firstValue("Location").orElseThrow());
        assertEquals("/calendar", target.getPath());
        assertEquals("month=2026-10", target.getQuery());
    }

    private static String dayArticle(String html) {
        var matcher = Pattern.compile("(?s)<article\\b[^>]*>\\s*<time datetime=\"" + DAY + "\"[^>]*>.*?</article>").matcher(html);
        assertTrue(matcher.find(), "The requested calendar day must be rendered");
        return matcher.group();
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

        HttpResponse<String> post(String path, Map<String, String> form) throws IOException, InterruptedException {
            String body = form.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
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

        HttpResponse<String> assign(String day, String districtId, boolean includeCsrf) throws IOException, InterruptedException {
            if (!includeCsrf) return post("/calendar/district", Map.of("date", day, "districtId", districtId));
            var page = get("/calendar?month=2026-10");
            assertEquals(200, page.statusCode());
            return post("/calendar/district", Map.of("date", day, "districtId", districtId, "_csrf", csrf(page.body())));
        }

        private static String csrf(String html) {
            var matcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
            assertTrue(matcher.find(), "The real login/calendar form must include its CSRF token");
            return matcher.group(1);
        }

        private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    }
}
