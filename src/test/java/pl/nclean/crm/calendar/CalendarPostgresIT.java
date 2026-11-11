package pl.nclean.crm.calendar;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import pl.nclean.crm.dashboard.DashboardService;
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
        jdbc.update("DELETE FROM fuel_audit");jdbc.update("DELETE FROM fuel_purchase");jdbc.update("DELETE FROM fuel_receipt");jdbc.update("DELETE FROM fuel_day");jdbc.update("DELETE FROM fuel_parameters");
        jdbc.update("DELETE FROM dashboard_month_cost");jdbc.update("DELETE FROM dashboard_month");jdbc.update("DELETE FROM dashboard_fuel");
        jdbc.update("DELETE FROM booking_actual_chemical");jdbc.update("DELETE FROM booking_chemical");jdbc.update("DELETE FROM booking_item");jdbc.update("DELETE FROM booking");jdbc.update("DELETE FROM crm_customer");
        jdbc.update("DELETE FROM service_chemical");jdbc.update("UPDATE service_definition SET chemistry_defined=FALSE");jdbc.update("DELETE FROM chemical");
        jdbc.update("DELETE FROM weekly_area");jdbc.update("DELETE FROM weekly_area_plan");jdbc.update("DELETE FROM day_area_override");
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
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), jdbc.queryForList(
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
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), legacy.queryForList("SELECT version FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL' ORDER BY installed_rank", String.class));
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
        assertEquals(8, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL'", Integer.class));
    }

    @Test
    void customerCatalogueAndBookingKeepHistoricalPricesAfterRestart() throws Exception {
        var records=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);
        var browser=app.browser();browser.login();
        assertEquals(200,browser.get("/customers").statusCode());
        assertEquals(200,browser.get("/settings/services").statusCode());
        long person=records.saveCustomer(null,0,"Klient syntetyczny","500000010","Adres przykładowy 1","Notatka",true);
        assertEquals("+48500000010",jdbc.queryForObject("SELECT phone FROM crm_customer WHERE id=?",String.class,person));
        var service=records.services().get(0);long serviceId=((Number)service.get("id")).longValue();
        var price=(java.math.BigDecimal)service.get("price");String token=UUID.randomUUID().toString();
        var fields=new LinkedHashMap<String,Object>();fields.put("token",token);fields.put("day",DAY);fields.put("starts","09:00");fields.put("customer",person);fields.put("address","Adres realizacji 2");fields.put("qty_"+serviceId,"2");
        assertEquals(302,browser.settings("/orders",fields,true).statusCode());
        assertEquals(302,browser.settings("/orders",fields,true).statusCode());
        long booking=jdbc.queryForObject("SELECT id FROM booking WHERE request_token=?::uuid",Long.class,token);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM booking WHERE request_token=?::uuid",Integer.class,token));
        assertEquals(200,browser.get("/orders?day="+DAY).statusCode());
        assertEquals(302,browser.settings("/settings/services",Map.of("id",serviceId,"version",service.get("version"),"category",service.get("category_id"),"name","Usługa po zmianie","unit","pakiet","price",price.add(new java.math.BigDecimal("50.00")).toPlainString(),"minutes","60","active","on"),true).statusCode());
        records.saveCustomer(person,0,"Nowa nazwa klienta","500000011","Inny adres","",true);
        assertEquals(302,browser.settings("/orders/"+booking,Map.of("version","0","day","2026-10-29","starts","10:00","status","DONE"),true).statusCode());
        assertEquals(0,price.multiply(java.math.BigDecimal.valueOf(2)).compareTo(jdbc.queryForObject("SELECT total FROM booking WHERE id=?",java.math.BigDecimal.class,booking)));
        assertEquals("Klient syntetyczny",jdbc.queryForObject("SELECT customer_name FROM booking WHERE id=?",String.class,booking));
        assertEquals(service.get("name"),jdbc.queryForObject("SELECT name FROM booking_item WHERE booking_id=?",String.class,booking));
        long count=jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class);
        assertThrows(IllegalArgumentException.class,()->records.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Nie zapisuj","500000010","Adres",Map.of(Long.MAX_VALUE,java.math.BigDecimal.ONE)));
        assertEquals(count,jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class));
        assertThrows(IllegalArgumentException.class,()->records.saveCustomer(person,0,"Nie nadpisuj","500000010","","",true));
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        assertEquals("DONE",jdbc.queryForObject("SELECT status FROM booking WHERE id=?",String.class,booking));
        var after=app.browser();after.login();assertEquals(200,after.get("/orders").statusCode());
    }

    @Test
    void failedBookingRollsBackInlineCustomerAndFormsRejectMissingData() throws Exception {
        var records=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);
        long category=((Number)records.categories().get(0).get("id")).longValue();
        records.service(null,0,category,"Próba rollback","szt.","9999999999.99",1,true);
        long service=jdbc.queryForObject("SELECT id FROM service_definition WHERE name='Próba rollback'",Long.class);
        long before=jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->records.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Wycofaj","500000010","Adres",Map.of(service,new java.math.BigDecimal("10000"))));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class));
        var browser=app.browser();browser.login();
        assertHtmlBadRequest(browser.settings("/orders",Map.of("day",DAY,"starts","09:00"),true));
        assertHtmlBadRequest(browser.settings("/customers",Map.of("name","Brak numeru"),true));
        assertHtmlBadRequest(browser.settings("/settings/services",Map.of("name","Brak danych"),true));
        records.category(null,"Nowa kategoria");
        long created=jdbc.queryForObject("SELECT id FROM service_category WHERE name='Nowa kategoria'",Long.class);
        records.category(created,"Kategoria zmieniona");
        records.service(service,0,created,"Usługa wyłączona","m²","12.50",20,false);
        assertThrows(IllegalArgumentException.class,()->records.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Nie dodawaj","500000010","Adres",Map.of(service,java.math.BigDecimal.ONE)));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class));
    }

    private long areaId(String name) { return jdbc.queryForObject("SELECT id FROM district WHERE name = ?", Long.class, name); }
    private List<Long> assignedAreas(String day) {
        return jdbc.queryForList("SELECT district_id FROM day_district WHERE day = ? ORDER BY district_id", Long.class, java.sql.Date.valueOf(day));
    }
    private static String calendarPath(String day) { return "/calendar?month=2026-10&day=" + day; }
    @Test
    void calendarShowsEightBookingsInTimeOrderAndJsonWritesSurviveRestart() throws Exception {
        jdbc.update("DELETE FROM booking_item");jdbc.update("DELETE FROM booking");jdbc.update("DELETE FROM crm_customer");
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);
        var browser=app.browser();browser.login();
        long person=data.saveCustomer(null,0,"Klient kalendarza","500000020","Adres domyślny","",true);
        long service=((Number)data.services().get(0).get("id")).longValue();
        for(int hour=15;hour>=8;hour--)data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.of(hour,0),person,null,null,"Adres wizyty",Map.of(service,java.math.BigDecimal.ONE));
        var month=data.monthBookings(java.time.YearMonth.of(2026,10));assertEquals(8,month.size());
        assertEquals("08:00:00",month.get(0).get("starts").toString());
        var page=browser.get(calendarPath(DAY));assertEquals(200,page.statusCode());assertTrue(page.body().contains("+6 kolejnych"));
        String token=UUID.randomUUID().toString();
        var fields=Map.<String,Object>of("calendar","1","token",token,"day",DAY,"starts","16:00","customer",person,"name","Kontakt na tę wizytę","phone","500000021","address","Inny adres","qty_"+service,"1");
        assertEquals(403,browser.settings("/orders",fields,false).statusCode());
        var created=browser.settings("/orders",fields,true);assertEquals(200,created.statusCode());assertTrue(created.body().contains("saved"));
        assertEquals(200,browser.settings("/orders",fields,true).statusCode());
        assertEquals(9,jdbc.queryForObject("SELECT count(*) FROM booking",Integer.class));
        assertEquals("Klient kalendarza",jdbc.queryForObject("SELECT name FROM crm_customer WHERE id=?",String.class,person));
        long id=jdbc.queryForObject("SELECT id FROM booking WHERE request_token=?::uuid",Long.class,token);
        assertEquals("Kontakt na tę wizytę",jdbc.queryForObject("SELECT customer_name FROM booking WHERE id=?",String.class,id));
        var invalid=new LinkedHashMap<String,Object>(fields);invalid.put("token",UUID.randomUUID());invalid.put("qty_"+service,"-1");
        var rejected=browser.settings("/orders",invalid,true);assertEquals(400,rejected.statusCode());assertTrue(rejected.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertEquals(200,browser.settings("/calendar/areas",Map.of("calendar","1","date",DAY,"districtIds",List.of(areaId("Krzyki"),areaId("Fabryczna"))),true).statusCode());
        assertEquals(200,browser.settings("/orders/"+id,Map.of("calendar","1","version","0","day",DAY,"starts","07:30","status","DONE"),true).statusCode());
        assertEquals(400,browser.settings("/orders/"+id,Map.of("calendar","1","version","0","day",DAY,"starts","07:30","status","DONE"),true).statusCode());
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        var after=app.browser();after.login();var html=after.get(calendarPath(DAY));assertEquals(200,html.statusCode());assertTrue(html.body().contains("Kontakt na tę wizytę"));
        assertEquals(2,assignedAreas(DAY).size());assertEquals("DONE",jdbc.queryForObject("SELECT status FROM booking WHERE id=?",String.class,id));
    }

    @Test
    void bookingAcrossCategoriesKeepsEveryItemAndVisitContactAfterRestart() throws Exception {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);
        var active=data.services().stream().filter(s -> Boolean.TRUE.equals(s.get("active"))).toList();
        var first=active.get(0);
        var second=active.stream().filter(s -> !s.get("category_id").equals(first.get("category_id"))).findFirst().orElseThrow();
        long firstId=((Number)first.get("id")).longValue(),secondId=((Number)second.get("id")).longValue();
        long customer=data.saveCustomer(null,0,"Stały klient lean","500000041","Adres domyślny lean","",true);
        var token=UUID.randomUUID();
        var fields=new LinkedHashMap<String,Object>();
        fields.put("calendar","1");fields.put("token",token);fields.put("day","2026-10-30");fields.put("starts","12:30");fields.put("customer",customer);
        fields.put("name","Kontakt wizyty lean");fields.put("phone","500000042");fields.put("address","Adres tej wizyty lean");
        fields.put("qty_"+firstId,"2");fields.put("qty_"+secondId,"1.5");
        var browser=app.browser();browser.login();
        assertEquals(403,browser.settings("/orders",fields,false).statusCode());
        assertEquals(200,browser.settings("/orders",fields,true).statusCode());
        assertEquals(200,browser.settings("/orders",fields,true).statusCode());
        long id=jdbc.queryForObject("SELECT id FROM booking WHERE request_token=?",Long.class,token);
        assertEquals(2,data.items(id).size());
        var expected=((java.math.BigDecimal)first.get("price")).multiply(new java.math.BigDecimal("2")).add(((java.math.BigDecimal)second.get("price")).multiply(new java.math.BigDecimal("1.5"))).setScale(2,java.math.RoundingMode.HALF_UP);
        assertEquals(0,expected.compareTo(jdbc.queryForObject("SELECT total FROM booking WHERE id=?",java.math.BigDecimal.class,id)));
        assertEquals("Adres domyślny lean",jdbc.queryForObject("SELECT address FROM crm_customer WHERE id=?",String.class,customer));
        assertEquals("Stały klient lean",jdbc.queryForObject("SELECT name FROM crm_customer WHERE id=?",String.class,customer));
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM booking_item WHERE booking_id=?",Integer.class,id));
        assertEquals("Kontakt wizyty lean",jdbc.queryForObject("SELECT customer_name FROM booking WHERE id=?",String.class,id));
        assertEquals("Adres tej wizyty lean",jdbc.queryForObject("SELECT address FROM booking WHERE id=?",String.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM booking WHERE request_token=?",Integer.class,token));
    }

    @Test
    void upgradingV3KeepsOldPricesAndDoesNotInventChemicalForecasts() {
        String schema="legacy_estimates",url=POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema;
        var source=new DriverManagerDataSource(url,POSTGRES.getUsername(),POSTGRES.getPassword());var old=new JdbcTemplate(source);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("3").load().migrate();
        long person=old.queryForObject("INSERT INTO crm_customer(name,phone) VALUES ('Stary klient syntetyczny','+48500000088') RETURNING id",Long.class);
        long service=old.queryForObject("SELECT id FROM service_definition WHERE name='Kanapa / sofa'",Long.class);
        long booking=old.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,total) VALUES (?,?,DATE '2026-10-28',TIME '10:00','Stary klient syntetyczny','+48500000088','Adres',180) RETURNING id",Long.class,UUID.randomUUID(),person);
        old.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount) VALUES (?,0,?,'Kanapa / sofa','szt.',1,180,180)",booking,service);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        var item=old.queryForMap("SELECT * FROM booking_item WHERE booking_id=?",booking);
        assertEquals(0,new java.math.BigDecimal("180").compareTo((java.math.BigDecimal)item.get("price")));
        assertEquals(0,new java.math.BigDecimal("180").compareTo((java.math.BigDecimal)item.get("suggested_price")));
        assertEquals(false,item.get("forecast_defined"));assertNull(item.get("chemical_cost"));
        assertEquals(0,old.queryForObject("SELECT count(*) FROM booking_chemical",Integer.class));
    }

    @Test
    void weeklyVersionsExceptionsAndRestorePreserveHistoryAndRestart() throws Exception {
        var calendar=app.context().getBean(CalendarService.class);
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Warsaw"));
        var monday=today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        long krzyki=areaId("Krzyki"),fabryczna=areaId("Fabryczna");
        calendar.assign(today.minusDays(1),List.of(fabryczna));
        calendar.saveWeekly(today,Map.of(1,List.of(krzyki),2,List.of(fabryczna)));
        assertEquals(java.util.Set.of(krzyki),calendar.assignedAreaIds(monday));
        assertEquals(java.util.Set.of(fabryczna),calendar.assignedAreaIds(monday.plusDays(1)));
        calendar.assign(monday,List.of());assertEquals(java.util.Set.of(),calendar.assignedAreaIds(monday));
        assertTrue(calendar.days(java.time.YearMonth.from(monday)).stream().filter(d->d.date().equals(monday)).findFirst().orElseThrow().overridden());
        calendar.restoreWeekly(monday);assertEquals(java.util.Set.of(krzyki),calendar.assignedAreaIds(monday));
        calendar.assign(monday,List.of(fabryczna));
        calendar.saveWeekly(monday.plusWeeks(1),Map.of(1,List.of(fabryczna)));
        assertEquals(java.util.Set.of(fabryczna),calendar.assignedAreaIds(monday.plusWeeks(1)));
        assertEquals(java.util.Set.of(fabryczna),calendar.assignedAreaIds(monday));
        calendar.restoreWeekly(monday);assertEquals(java.util.Set.of(krzyki),calendar.assignedAreaIds(monday));
        assertEquals(java.util.Set.of(fabryczna),calendar.assignedAreaIds(today.minusDays(1)));
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        var after=app.context().getBean(CalendarService.class);assertEquals(java.util.Set.of(krzyki),after.assignedAreaIds(monday));assertEquals(java.util.Set.of(fabryczna),after.assignedAreaIds(monday.plusWeeks(1)));
        var browser=app.browser();browser.login();assertTrue(browser.get("/calendar?day="+monday).body().contains("Z planu tygodniowego"));
    }

    @Test
    void emptyWeeklyPlanReplacesEarlierVersionWithoutChangingEarlierDays() {
        var calendar=app.context().getBean(CalendarService.class);var today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Warsaw"));
        long krzyki=areaId("Krzyki");var monday=today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        calendar.saveWeekly(today,Map.of(1,List.of(krzyki)));calendar.saveWeekly(monday.plusWeeks(1),Map.of());
        assertEquals(java.util.Set.of(krzyki),calendar.assignedAreaIds(monday));assertEquals(java.util.Set.of(),calendar.assignedAreaIds(monday.plusWeeks(1)));
        assertThrows(IllegalArgumentException.class,()->calendar.saveWeekly(today.minusDays(1),Map.of(1,List.of(krzyki))));
        calendar.assign(monday,List.of(krzyki));calendar.setAreaActive(krzyki,false);calendar.assign(monday,List.of(krzyki));
        assertEquals(java.util.Set.of(krzyki),calendar.assignedAreaIds(monday));
        assertThrows(IllegalArgumentException.class,()->calendar.saveWeekly(monday.plusWeeks(2),Map.of(3,List.of(krzyki))));
    }

    private long estimateService(String name) {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);long category=((Number)data.categories().get(0).get("id")).longValue();
        data.service(null,0,category,name,"szt.","180.00",60,true);
        return jdbc.queryForObject("SELECT id FROM service_definition WHERE name=?",Long.class,name);
    }

    @Test
    void negotiatedPriceAndChemicalSnapshotsRemainAfterCatalogueEditsAndRestart() throws Exception {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);
        long service=estimateService("Usługa prognozy");long material=chemistry.saveMaterial(null,0,"Płukanka próba","l","1","50.00");
        chemistry.saveRecipe(service,0,true,Map.of(material,new java.math.BigDecimal("50")));
        var quote=chemistry.quote(service,new java.math.BigDecimal("2"),null);assertEquals(0,new java.math.BigDecimal("5.00").compareTo(quote.cost()));
        var browser=app.browser();browser.login();var token=UUID.randomUUID();
        var fields=Map.<String,Object>of("calendar","1","token",token,"day",DAY,"starts","11:00","name","Klient prognozy","phone","500000070","address","Adres prognozy","qty_"+service,"2","price_"+service,"220.00");
        assertEquals(200,browser.settings("/orders",fields,true).statusCode());assertEquals(200,browser.settings("/orders",fields,true).statusCode());
        long booking=jdbc.queryForObject("SELECT id FROM booking WHERE request_token=?",Long.class,token);
        var item=data.items(booking).get(0);assertEquals(0,new java.math.BigDecimal("180").compareTo((java.math.BigDecimal)item.get("suggested_price")));assertEquals(0,new java.math.BigDecimal("220").compareTo((java.math.BigDecimal)item.get("price")));
        assertEquals(0,new java.math.BigDecimal("440").compareTo(jdbc.queryForObject("SELECT total FROM booking WHERE id=?",java.math.BigDecimal.class,booking)));
        assertEquals(0,new java.math.BigDecimal("5").compareTo(jdbc.queryForObject("SELECT chemical_cost FROM booking WHERE id=?",java.math.BigDecimal.class,booking)));
        chemistry.saveMaterial(material,0,"Płukanka nowa cena","ml","1000","100.00");chemistry.saveRecipe(service,1,true,Map.of(material,new java.math.BigDecimal("150")));
        var current=data.services().stream().filter(v->((Number)v.get("id")).longValue()==service).findFirst().orElseThrow();data.service(service,((Number)current.get("version")).longValue(),((Number)current.get("category_id")).longValue(),"Usługa nowa cena","szt.","300",60,true);
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        var snapshot=jdbc.queryForMap("SELECT * FROM booking_chemical WHERE booking_id=?",booking);assertEquals("Płukanka próba",snapshot.get("name"));assertEquals("ml",snapshot.get("unit"));assertEquals(0,new java.math.BigDecimal("100").compareTo((java.math.BigDecimal)snapshot.get("quantity")));assertEquals(0,new java.math.BigDecimal("5").compareTo((java.math.BigDecimal)snapshot.get("amount")));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM booking WHERE request_token=?",Integer.class,token));
    }

    @Test
    void manualChemicalForecastZeroUnknownAndDimensionValidationAreDistinct() {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);
        long service=estimateService("Usługa ręcznej prognozy");long liquid=chemistry.saveMaterial(null,0,"Płyn","l","1","50");long powder=chemistry.saveMaterial(null,0,"Proszek","kg","2","80");
        chemistry.saveRecipe(service,0,true,Map.of(liquid,new java.math.BigDecimal("50")));
        assertEquals(0,new java.math.BigDecimal("0.04").compareTo(chemistry.materials().stream().filter(m->m.id()==powder).findFirst().orElseThrow().unitCost()));
        var forecast=new pl.nclean.crm.records.ChemistryService.Forecast(true,Map.of(liquid,new java.math.BigDecimal("20"),powder,new java.math.BigDecimal("10")));
        long booking=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Ręczna prognoza","500000071","Adres",Map.of(service,new java.math.BigDecimal("2")),Map.of(),Map.of(service,forecast));
        assertEquals(2,data.chemicals(booking).size());assertEquals(0,new java.math.BigDecimal("1.40").compareTo(jdbc.queryForObject("SELECT chemical_cost FROM booking WHERE id=?",java.math.BigDecimal.class,booking)));
        var zero=chemistry.quote(service,java.math.BigDecimal.ONE,new pl.nclean.crm.records.ChemistryService.Forecast(true,Map.of()));assertTrue(zero.defined());assertEquals(0,zero.cost().signum());
        long zeroBooking=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Prognoza zero","500000079","Adres",Map.of(service,java.math.BigDecimal.ONE),Map.of(),Map.of(service,new pl.nclean.crm.records.ChemistryService.Forecast(true,Map.of())));
        assertTrue((Boolean)data.items(zeroBooking).get(0).get("forecast_defined"));assertEquals(0,jdbc.queryForObject("SELECT chemical_cost FROM booking WHERE id=?",java.math.BigDecimal.class,zeroBooking).signum());
        var unknown=chemistry.quote(service,java.math.BigDecimal.ONE,new pl.nclean.crm.records.ChemistryService.Forecast(false,Map.of()));assertFalse(unknown.defined());assertNull(unknown.cost());
        assertThrows(IllegalArgumentException.class,()->chemistry.saveMaterial(liquid,0,"Płyn","kg","1","50"));
        assertThrows(IllegalArgumentException.class,()->chemistry.quote(service,java.math.BigDecimal.ONE,new pl.nclean.crm.records.ChemistryService.Forecast(true,Map.of(liquid,new java.math.BigDecimal("-1")))));
        assertThrows(IllegalArgumentException.class,()->chemistry.saveMaterial(null,0,"Brak opakowania","l","0","50"));
    }

    @Test
    void missingStandardDoesNotBlockBookingAndInvalidForecastRollsBack() {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);long service=estimateService("Usługa bez standardu");
        long id=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Bez prognozy","500000072","Adres",Map.of(service,java.math.BigDecimal.ONE));
        assertNull(jdbc.queryForObject("SELECT chemical_cost FROM booking WHERE id=?",java.math.BigDecimal.class,id));assertFalse((Boolean)data.items(id).get(0).get("forecast_defined"));
        long before=jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class);
        assertThrows(IllegalArgumentException.class,()->data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Nie zapisz","500000073","Adres",Map.of(service,java.math.BigDecimal.ONE),Map.of(),Map.of(service,new pl.nclean.crm.records.ChemistryService.Forecast(true,Map.of(Long.MAX_VALUE,java.math.BigDecimal.ONE)))));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class));
        assertThrows(IllegalArgumentException.class,()->data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Nie zapisz ceny","500000074","Adres",Map.of(service,java.math.BigDecimal.ONE),Map.of(service,new java.math.BigDecimal("-1")),Map.of()));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM crm_customer",Long.class));
    }

    @Test
    void actualUsageLearnsPerUnitMedianOnlyAfterFiveCompletedReportedBookings() {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);var actual=app.context().getBean(pl.nclean.crm.records.ActualUsageService.class);
        long service=estimateService("Usługa nauki zużycia");long liquid=chemistry.saveMaterial(null,0,"Płukanka obserwowana","l","1","50");long other=chemistry.saveMaterial(null,0,"Drugi środek","ml","500","40");
        chemistry.saveRecipe(service,0,true,Map.of(liquid,new java.math.BigDecimal("50"),other,new java.math.BigDecimal("20")));
        var ids=new java.util.ArrayList<Long>();
        for(int index=0;index<7;index++){
            long id=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Klient próby obserwacji","500000091","Adres próby",Map.of(service,new java.math.BigDecimal("2")));
            data.reschedule(id,0,java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,index==6?"CANCELLED":"DONE");ids.add(id);
        }
        assertTrue(actual.suggestions().isEmpty(),"Forecasts and completion alone are not observations");
        String[] perUnit={"0","50","60","70","1000"};
        for(int index=0;index<5;index++){
            actual.record(ids.get(index),0,0,Map.of(liquid,new java.math.BigDecimal(perUnit[index]).multiply(new java.math.BigDecimal("2"))));
            if(index<4)assertTrue(actual.suggestions().isEmpty());
        }
        var suggestion=actual.suggestions().get(service).get(0);assertEquals(5,suggestion.samples());assertEquals(0,new java.math.BigDecimal("60").compareTo(suggestion.quantity()));assertEquals(0,suggestion.minimum().signum());assertEquals(0,new java.math.BigDecimal("1000").compareTo(suggestion.maximum()));
        actual.apply(service,liquid,1);assertEquals(0,new java.math.BigDecimal("60").compareTo(chemistry.recipes().get(service).quantities().get(liquid)));assertEquals(0,new java.math.BigDecimal("20").compareTo(chemistry.recipes().get(service).quantities().get(other)));
        assertEquals(0,new java.math.BigDecimal("100").compareTo((java.math.BigDecimal)data.chemicals(ids.get(1)).stream().filter(row->((Number)row.get("chemical_id")).longValue()==liquid).findFirst().orElseThrow().get("quantity")),"Applying a standard does not rewrite old forecasts");
        actual.record(ids.get(0),0,1,Map.of(liquid,new java.math.BigDecimal("200")));
        var corrected=actual.suggestions().get(service).get(0);assertEquals(5,corrected.samples());assertEquals(0,new java.math.BigDecimal("70").compareTo(corrected.quantity()));
        data.reschedule(ids.get(1),1,java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,"CANCELLED");assertTrue(actual.suggestions().isEmpty());
        data.reschedule(ids.get(1),2,java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,"DONE");
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);
        assertEquals(5,app.context().getBean(pl.nclean.crm.records.ActualUsageService.class).suggestions().get(service).get(0).samples());
    }

    @Test
    void actualUsageRejectsUnfinishedInvalidAndStaleWritesAndBlankMeansUnknown() {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);var actual=app.context().getBean(pl.nclean.crm.records.ActualUsageService.class);
        long service=estimateService("Usługa walidacji pomiarów");long material=chemistry.saveMaterial(null,0,"Środek rzeczywisty","l","1","40");
        long booking=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Klient pomiarów","500000092","Adres",Map.of(service,java.math.BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,()->actual.record(booking,0,0,Map.of(material,java.math.BigDecimal.ONE)));
        data.reschedule(booking,0,java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,"DONE");
        assertThrows(IllegalArgumentException.class,()->actual.record(booking,0,0,Map.of(material,new java.math.BigDecimal("-1"))));
        assertThrows(IllegalArgumentException.class,()->actual.record(booking,0,0,Map.of(Long.MAX_VALUE,java.math.BigDecimal.ONE)));
        actual.record(booking,0,0,Map.of(material,java.math.BigDecimal.ZERO));assertTrue((Boolean)data.items(booking).get(0).get("actual_defined"));
        assertThrows(IllegalArgumentException.class,()->actual.record(booking,0,0,Map.of(material,new java.math.BigDecimal("100"))));
        assertEquals(0,((java.math.BigDecimal)data.actuals(booking).get(0).get("quantity")).signum());
        assertThrows(IllegalArgumentException.class,()->chemistry.saveMaterial(material,0,"Środek rzeczywisty","kg","1","40"));
        actual.record(booking,0,1,Map.of());assertFalse((Boolean)data.items(booking).get(0).get("actual_defined"));assertTrue(data.actuals(booking).isEmpty());
        assertNull(jdbc.queryForObject("SELECT chemical_cost FROM booking WHERE id=?",java.math.BigDecimal.class,booking));
    }

    @Test
    void actualUsageHttpAndSuggestionUseValidatedServerDataAndKeepUnitsSeparate() throws Exception {
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);var actual=app.context().getBean(pl.nclean.crm.records.ActualUsageService.class);
        long service=estimateService("Usługa jednostek pomiarów");long material=chemistry.saveMaterial(null,0,"Płyn jednostek","ml","1000","50");
        var browser=app.browser();browser.login();
        long first=0;
        for(int index=0;index<6;index++){
            long id=data.book(UUID.randomUUID(),java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,null,"Klient jednostek","500000093","Adres",Map.of(service,new java.math.BigDecimal("2")));
            data.reschedule(id,0,java.time.LocalDate.parse(DAY),java.time.LocalTime.NOON,"DONE");if(index==0)first=id;
            if(index<5){
                String path="/orders/"+id+"/items/0/actual";
                assertEquals(403,browser.settings(path,Map.of("calendar","1","version","0","actual_"+material,"120"),false).statusCode());
                assertEquals(200,browser.settings(path,Map.of("calendar","1","version","0","actual_"+material,"120"),true).statusCode());
            }
        }
        assertEquals(5,actual.suggestions().get(service).get(0).samples());
        assertEquals(302,browser.settings("/settings/services/"+service+"/suggestion",Map.of("chemical",material,"version","0","quantity","999999"),true).statusCode());
        assertEquals(0,new java.math.BigDecimal("60").compareTo(chemistry.recipes().get(service).quantities().get(material)));
        assertTrue(browser.get("/orders?day="+DAY).body().contains("Rzeczywiste zużycie chemii"));
        var fields=data.actualFields(Map.of(first,data.items(first)),data.chemicals(first),data.actuals(first));assertEquals(0,new java.math.BigDecimal("120").compareTo(fields.get(first+":0").get(material)));
        var current=data.services().stream().filter(v->((Number)v.get("id")).longValue()==service).findFirst().orElseThrow();
        data.service(service,((Number)current.get("version")).longValue(),((Number)current.get("category_id")).longValue(),"Usługa innej jednostki","pakiet","180",60,true);
        assertTrue(actual.suggestions().isEmpty(),"Different service units must not be mixed");
    }

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


    @Test
    void dashboardUsesSnapshotsAndSeparatesPlanActualCancellationAndMissingData() {
        var dashboard=app.context().getBean(DashboardService.class);var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);var actual=app.context().getBean(pl.nclean.crm.records.ActualUsageService.class);
        var date=DashboardService.today();long service=estimateService("Dashboard fotel");long liquid=chemistry.saveMaterial(null,0,"Płukanka dashboard","l","1","50");
        chemistry.saveRecipe(service,0,true,Map.of(liquid,new BigDecimal("100")));
        long first=data.book(UUID.randomUUID(),date,java.time.LocalTime.NOON,null,"Klient dashboard","500000096","Adres",Map.of(service,BigDecimal.ONE),Map.of(service,new BigDecimal("200")),Map.of());
        long second=data.book(UUID.randomUUID(),date,java.time.LocalTime.NOON,null,"Klient dashboard","500000096","Adres",Map.of(service,BigDecimal.ONE),Map.of(service,new BigDecimal("300")),Map.of());
        long cancelled=data.book(UUID.randomUUID(),date,java.time.LocalTime.NOON,null,"Klient dashboard","500000096","Adres",Map.of(service,BigDecimal.ONE));
        data.reschedule(first,0,date,java.time.LocalTime.NOON,"DONE");data.reschedule(cancelled,0,date,java.time.LocalTime.NOON,"CANCELLED");
        dashboard.saveMonth(YearMonth.from(date),-1,List.of(new DashboardService.Cost("Reklama","MARKETING",new BigDecimal("300")),new DashboardService.Cost("Telefon","PHONE",new BigDecimal("150"))));
        dashboard.saveFuel(date,-1,new BigDecimal("8"),new BigDecimal("6"),new BigDecimal("100"),new BigDecimal("50"),null);
        var plan=dashboard.summary(date,"day","plan");assertEquals(new BigDecimal("500.00"),plan.revenue());assertEquals(new BigDecimal("10.00"),plan.chemistry());assertEquals(new BigDecimal("48.00"),plan.fuel());assertEquals(1,plan.planned());assertEquals(1,plan.done());assertEquals(1,plan.cancelled());assertTrue(plan.complete());
        var unknown=dashboard.summary(date,"day","actual");assertEquals(1,unknown.missingChemistry());assertFalse(unknown.complete());assertEquals(new BigDecimal("200.00"),unknown.revenue());
        actual.record(first,0,0,Map.of(liquid,new BigDecimal("120")));chemistry.saveMaterial(liquid,0,"Płukanka dashboard","l","1","100");
        var done=dashboard.summary(date,"day","actual");assertTrue(done.complete());assertEquals(new BigDecimal("6.00"),done.chemistry());assertEquals(new BigDecimal("24.00"),done.fuel());
        actual.record(first,0,1,Map.of(liquid,new BigDecimal("140")));assertEquals(new BigDecimal("7.00"),dashboard.summary(date,"day","actual").chemistry(),"Editing quantity preserves price snapshot");
        actual.record(first,0,2,Map.of(liquid,BigDecimal.ZERO));assertTrue(dashboard.summary(date,"day","actual").complete());assertEquals(new BigDecimal("0.00"),dashboard.summary(date,"day","actual").chemistry());
        dashboard.saveFuel(date,0,new BigDecimal("8"),new BigDecimal("6"),new BigDecimal("100"),new BigDecimal("50"),new BigDecimal("12"));assertEquals(new BigDecimal("12.00"),dashboard.summary(date,"day","actual").fuel());
        assertEquals(new BigDecimal("500.00"),dashboard.summary(date,"day","plan").revenue());assertEquals(new BigDecimal("10.00"),dashboard.summary(date,"day","plan").chemistry());
    }

    @Test
    void dashboardMonthAllocationPersistsAndRejectsStaleAndClosedMonthChanges() {
        var dashboard=app.context().getBean(DashboardService.class);var date=DashboardService.today();var month=YearMonth.from(date);
        dashboard.saveMonth(month,-1,List.of(new DashboardService.Cost("Marketing","MARKETING",new BigDecimal("100.01")),new DashboardService.Cost("Stałe","OTHER",new BigDecimal("200.02"))));
        var summary=dashboard.summary(date,"month","actual");assertEquals(new BigDecimal("100.01"),summary.marketing());assertEquals(new BigDecimal("200.02"),summary.fixed());assertEquals(new BigDecimal("-300.03"),summary.remaining());assertEquals(month.lengthOfMonth(),summary.missingFuel());
        var initialDashboard=dashboard;assertThrows(IllegalArgumentException.class,()->initialDashboard.saveMonth(month,-1,List.of()));assertThrows(IllegalArgumentException.class,()->initialDashboard.saveMonth(month.minusMonths(1),-1,List.of()));
        dashboard.saveFuel(date,-1,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,null);assertThrows(IllegalArgumentException.class,()->initialDashboard.saveFuel(date,-1,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,null));
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);dashboard=app.context().getBean(DashboardService.class);
        assertEquals(new BigDecimal("100.01"),dashboard.summary(date,"month","actual").marketing());assertEquals(new BigDecimal("0.00"),dashboard.summary(date,"day","actual").fuel());
    }

    @Test
    void dashboardHandlesCrossMonthWeekMissingPricesAndEmptyPeriods() {
        var dashboard=app.context().getBean(DashboardService.class);
        var empty=dashboard.summary(LocalDate.of(2028,2,10),"day","plan");assertTrue(empty.orders().isEmpty());assertEquals(new BigDecimal("0.00"),empty.costs());assertFalse(empty.complete());
        jdbc.update("INSERT INTO dashboard_month(month,configured) VALUES ('2026-12-01',TRUE),('2027-01-01',TRUE)");
        jdbc.update("INSERT INTO dashboard_month_cost(month,name,category,amount) VALUES ('2026-12-01','Grudzień','MARKETING',310),('2027-01-01','Styczeń','OTHER',620)");
        var week=dashboard.summary(LocalDate.of(2026,12,31),"week","plan");assertEquals(new BigDecimal("40.00"),week.marketing());assertEquals(new BigDecimal("60.00"),week.fixed());assertEquals(0,week.missingMonths());
        var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);long service=estimateService("Bez historycznej wyceny");var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);long chemical=chemistry.saveMaterial(null,0,"Stary pomiar","ml","1000","50");
        long id=data.book(UUID.randomUUID(),LocalDate.of(2028,2,10),java.time.LocalTime.NOON,null,"Klient próbny","500000095","Adres",Map.of(service,BigDecimal.ONE));data.reschedule(id,0,LocalDate.of(2028,2,10),java.time.LocalTime.NOON,"DONE");
        jdbc.update("UPDATE booking_item SET actual_defined=TRUE WHERE booking_id=?",id);jdbc.update("INSERT INTO booking_actual_chemical(booking_id,item_position,chemical_id,name,unit,quantity) VALUES (?,0,?,'Stary pomiar','ml',100)",id,chemical);
        assertEquals(1,dashboard.summary(LocalDate.of(2028,2,10),"day","actual").missingChemistry());
    }

    @Test
    void dashboardHttpRendersAndInvalidSavePreservesFormAndCsrf() throws Exception {
        var browser=app.browser();browser.login();var date=DashboardService.today();
        assertEquals(200,browser.get("/dashboard?date="+date).statusCode());assertEquals(200,browser.get("/dashboard?date="+date+"&range=month&mode=actual").statusCode());
        var costs=browser.get("/settings/costs?day="+date+"&month="+YearMonth.from(date));assertEquals(200,costs.statusCode());assertTrue(costs.body().contains("name=\"_csrf\""));
        var form=new LinkedHashMap<String,Object>();form.put("month",YearMonth.from(date));form.put("day",date);form.put("version",-1);form.put("name_0","Zapamiętaj reklamę");form.put("category_0","MARKETING");form.put("amount_0","-12");
        assertEquals(403,browser.settings("/settings/costs/month",form,false).statusCode());var invalid=browser.settings("/settings/costs/month",form,true);assertEquals(400,invalid.statusCode());assertTrue(invalid.body().contains("Zapamiętaj reklamę"));assertTrue(invalid.body().contains("value=\"-12\""));
        form.put("amount_0","300");assertEquals(302,browser.settings("/settings/costs/month",form,true).statusCode());assertEquals(400,browser.settings("/settings/costs/month",form,true).statusCode());assertEquals(400,browser.get("/dashboard?range=year").statusCode());
    }


    @Test
    void dashboardV7UpgradesOldMeasurementsOnlyWithMatchingHistoricalPrices() {
        String schema="dashboard_v6_upgrade";String url=POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema;
        var source=new DriverManagerDataSource(url,POSTGRES.getUsername(),POSTGRES.getPassword());var old=new JdbcTemplate(source);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("6").load().migrate();
            long person=old.queryForObject("INSERT INTO crm_customer(name,phone) VALUES ('Test migracji','+48500000099') RETURNING id",Long.class);
            long service=old.queryForObject("SELECT min(id) FROM service_definition",Long.class);
            long material=old.queryForObject("INSERT INTO chemical(name,package_unit,package_quantity,package_price) VALUES ('Stara chemia','l',1,900) RETURNING id",Long.class);
            long booking=old.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,total,status) VALUES (?,?,CURRENT_DATE,'10:00','Test migracji','+48500000099','Adres',100,'DONE') RETURNING id",Long.class,UUID.randomUUID(),person);
            for(int i=0;i<3;i++){
                old.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount,suggested_price,actual_defined) VALUES (?,?,?,'Fotel','szt.',1,100,100,100,TRUE)",booking,i,service);
                old.update("INSERT INTO booking_actual_chemical(booking_id,item_position,chemical_id,name,unit,quantity) VALUES (?,?,?,'Stara chemia','ml',120)",booking,i,material);
            }
            old.update("INSERT INTO booking_chemical(booking_id,item_position,chemical_id,name,unit,quantity,unit_cost,amount) VALUES (?,0,?,'Stara chemia','ml',100,0.05,5),(?,1,?,'Stara chemia','g',100,0.05,5)",booking,material,booking,material);
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            assertEquals(new BigDecimal("6.00"),old.queryForObject("SELECT amount FROM booking_actual_chemical WHERE item_position=0",BigDecimal.class));
            assertNull(old.queryForObject("SELECT amount FROM booking_actual_chemical WHERE item_position=1",BigDecimal.class));assertNull(old.queryForObject("SELECT amount FROM booking_actual_chemical WHERE item_position=2",BigDecimal.class));
            assertEquals(new BigDecimal("900.00"),old.queryForObject("SELECT package_price FROM chemical",BigDecimal.class));
        }finally{jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}
    }

    @Test
    void dashboardPartialMeasurementsAndInvalidCostsRemainExplicit() {
        var dashboard=app.context().getBean(DashboardService.class);var data=app.context().getBean(pl.nclean.crm.records.CrmRecords.class);var chemistry=app.context().getBean(pl.nclean.crm.records.ChemistryService.class);var actual=app.context().getBean(pl.nclean.crm.records.ActualUsageService.class);var date=DashboardService.today();
        long service=estimateService("Częściowy pomiar");long a=chemistry.saveMaterial(null,0,"Środek A","l","1","50"),b=chemistry.saveMaterial(null,0,"Środek B","l","1","50");chemistry.saveRecipe(service,0,true,Map.of(a,new BigDecimal("100"),b,new BigDecimal("100")));
        long booking=data.book(UUID.randomUUID(),date,java.time.LocalTime.NOON,null,"Klient próbny","500000099","Adres",Map.of(service,BigDecimal.ONE));data.reschedule(booking,0,date,java.time.LocalTime.NOON,"DONE");actual.record(booking,0,0,Map.of(a,new BigDecimal("100")));
        assertEquals(1,dashboard.summary(date,"day","actual").missingChemistry());actual.record(booking,0,1,Map.of(a,new BigDecimal("100"),b,BigDecimal.ZERO));assertEquals(0,dashboard.summary(date,"day","actual").missingChemistry());
        assertThrows(IllegalArgumentException.class,()->dashboard.saveMonth(YearMonth.from(date),-1,List.of(new DashboardService.Cost("Zły koszt",null,BigDecimal.ONE))));assertEquals(-1,dashboard.monthVersion(YearMonth.from(date)));
        assertThrows(IllegalArgumentException.class,()->dashboard.saveFuel(date,-1,new BigDecimal("-1"),BigDecimal.ONE,null,null,null));assertEquals(-1,((Number)dashboard.fuel(date).get("version")).longValue());
        dashboard.saveMonth(YearMonth.from(date),-1,List.of());dashboard.saveFuel(date,-1,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,null);assertTrue(dashboard.summary(date,"day","actual").complete());
    }


    private long seedFuelParameters(LocalDate date) {
        return jdbc.queryForObject("INSERT INTO fuel_parameters(request_token,effective_day,car_rate,generator_rate,car_fuel,generator_fuel,car_price,generator_price) VALUES (?,?,8,1.5,'DIESEL','PETROL',6,7) RETURNING id",Long.class,UUID.randomUUID(),date);
    }
    private pl.nclean.crm.fuel.FuelJournalService.Purchase purchaseInput(UUID token,LocalDate date,int minute,String litres,String odo,boolean full,boolean complete,String target,String type,UUID receipt) {
        return new pl.nclean.crm.fuel.FuelJournalService.Purchase(null,-1,token,date.atTime(0,minute),target,type,"60",litres,"",full,odo,complete,"Próba",receipt);
    }
    @Test void fuelReadingsHandleBaselineGapsCorrectionAndSnapshots() {
        var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var today=DashboardService.today();seedFuelParameters(today.minusDays(10));
        fuel.saveDay(today.minusDays(2),-1,"1000",0,0,false,OWNER);var baseline=fuel.usage(today.minusDays(2),today).get(today.minusDays(2));assertFalse(baseline.carKnown());assertNull(baseline.kilometres());
        fuel.saveDay(today,-1,"1100",1,30,false,OWNER);var gap=fuel.usage(today,today).get(today);assertFalse(gap.carKnown());assertEquals(new BigDecimal("100.0"),gap.kilometres());assertEquals(new BigDecimal("15.75"),gap.generatorCost());assertTrue(gap.note().contains("Brak podziału"));
        fuel.saveDay(today.minusDays(1),-1,"1050",0,0,false,OWNER);assertEquals(new BigDecimal("24.00"),fuel.usage(today,today).get(today).carCost());
        var originalFuel=fuel;assertThrows(IllegalArgumentException.class,()->originalFuel.saveDay(today.minusDays(1),0,"1060",0,0,false,OWNER));fuel.saveDay(today.minusDays(1),0,"1060",0,0,true,OWNER);assertEquals(new BigDecimal("19.20"),fuel.usage(today,today).get(today).carCost());
        assertThrows(IllegalArgumentException.class,()->originalFuel.saveDay(today.minusDays(1),0,"1060",0,0,true,OWNER));assertThrows(IllegalArgumentException.class,()->originalFuel.saveDay(today.minusDays(1),1,"1200",0,0,true,OWNER));assertThrows(IllegalArgumentException.class,()->originalFuel.saveDay(today,0,"1100",24,1,true,OWNER));
        fuel.saveParameters(UUID.randomUUID(),today,"20","3","DIESEL","PETROL","9","10");fuel.saveDay(today,0,"1100",2,0,true,OWNER);assertEquals(new BigDecimal("21.00"),fuel.usage(today,today).get(today).generatorCost());assertEquals(new BigDecimal("19.20"),fuel.usage(today,today).get(today).carCost());
        assertEquals(2,fuel.audit(today.minusDays(1)).size());assertEquals("1050.0",fuel.audit(today.minusDays(1)).get(0).get("old_odometer"));
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);var restarted=fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);assertEquals(new BigDecimal("19.20"),restarted.usage(today,today).get(today).carCost());
    }
    @Test void fuelUnusualAndInvalidReadingsRequireExplicitConfirmation() {
        var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var today=DashboardService.today();seedFuelParameters(today.minusDays(1));fuel.saveDay(today.minusDays(1),-1,"1000",0,0,false,OWNER);
        assertThrows(IllegalArgumentException.class,()->fuel.saveDay(today,-1,"2500",17,0,false,OWNER));fuel.saveDay(today,-1,"2500",17,0,true,OWNER);
        assertThrows(IllegalArgumentException.class,()->fuel.saveDay(today,0,"-1",1,0,true,OWNER));assertThrows(IllegalArgumentException.class,()->fuel.saveDay(today.plusDays(1),-1,"2500",0,0,false,OWNER));assertThrows(IllegalArgumentException.class,()->fuel.saveParameters(UUID.randomUUID(),today.minusDays(1),"8","1","DIESEL","PETROL","6","6"));
    }
    @Test void fuelEstimatedCostAndPurchaseSpendingAreNotCountedTwiceAndLegacyOverridesSurvive() {
        var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var dash=app.context().getBean(DashboardService.class);var today=DashboardService.today();seedFuelParameters(today.minusDays(1));fuel.saveDay(today.minusDays(1),-1,"1000",0,0,false,OWNER);fuel.saveDay(today,-1,"1100",1,30,false,OWNER);
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,0,"10","",false,false,"GENERATOR","PETROL",null),OWNER);
        assertEquals(new BigDecimal("63.75"),dash.summary(today,"day","actual").fuel());assertEquals(new BigDecimal("60.00"),fuel.totals(today,today).purchases());assertEquals(new BigDecimal("8.000"),fuel.totals(today,today).carLitres());
        dash.saveFuel(today,-1,new BigDecimal("8"),new BigDecimal("6"),new BigDecimal("50"),new BigDecimal("50"),null);assertEquals(new BigDecimal("39.75"),dash.summary(today,"day","actual").fuel());
        dash.saveFuel(today,0,new BigDecimal("8"),new BigDecimal("6"),BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("12"));assertEquals(new BigDecimal("12.00"),dash.summary(today,"day","actual").fuel());
        assertEquals(new BigDecimal("15.75"),dash.summary(today,"day","plan").fuel());
    }
    @Test void fuelFullToFullIncludesPartialPurchasesButExcludesOpeningFill() {
        var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var today=DashboardService.today();seedFuelParameters(today.minusDays(10));
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today.minusDays(1),0,"30","1000",true,false,"CAR","DIESEL",null),OWNER);
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,0,"3","1050",false,false,"CAR","DIESEL",null),OWNER);
        var token=UUID.randomUUID();var input=purchaseInput(token,today,1,"7","1100",true,true,"CAR","DIESEL",null);long ending=fuel.savePurchase(input,OWNER);assertEquals(ending,fuel.savePurchase(input,OWNER));assertEquals(3,fuel.purchases(today.minusDays(1),today).size());
        var comparison=fuel.comparisons(today).get(0);assertTrue(comparison.complete());assertEquals(new BigDecimal("10.000"),comparison.litres());assertEquals(new BigDecimal("10.000"),comparison.actualRate());assertEquals(new BigDecimal("8.000"),comparison.expectedLitres());assertEquals(new BigDecimal("25.00"),comparison.percent());
        assertThrows(IllegalArgumentException.class,()->fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,2,"5","900",true,true,"CAR","DIESEL",null),OWNER));
    }
    @Test void fuelMissingLitresTypesAndUnconfirmedRecordsDoNotClaimMeasuredConsumption() {
        var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var today=DashboardService.today();seedFuelParameters(today.minusDays(10));
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today.minusDays(1),0,"30","1000",true,false,"CAR","DIESEL",null),OWNER);
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,0,"","",false,false,"CAR","DIESEL",null),OWNER);fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,1,"10","1100",true,true,"CAR","DIESEL",null),OWNER);
        assertFalse(fuel.comparisons(today).get(0).complete());assertNull(fuel.comparisons(today).get(0).actualRate());assertEquals(new BigDecimal("120.00"),fuel.totals(today,today).purchases());
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,2,"10","1200",true,false,"CAR","DIESEL",null),OWNER);assertFalse(fuel.comparisons(today).get(0).complete());
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,3,"10","1300",true,true,"CAR","PETROL",null),OWNER);assertFalse(fuel.comparisons(today).get(0).complete());
        fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,4,"10","",false,false,"SHARED","PETROL",null),OWNER);assertEquals(3,fuel.comparisons(today).size());
    }
    @Test void fuelReceiptsValidateBytesStayPrivateAndSurviveRestartAndReplacement() throws Exception {
        var receipts=app.context().getBean(pl.nclean.crm.fuel.ReceiptService.class);var fuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);var today=DashboardService.today();
        assertThrows(IllegalArgumentException.class,()->receipts.upload(new org.springframework.mock.web.MockMultipartFile("image","fake.jpg","image/jpeg","not an image".getBytes())));
        assertThrows(IllegalArgumentException.class,()->receipts.upload(new org.springframework.mock.web.MockMultipartFile("image","huge.jpg","image/jpeg",new byte[8*1024*1024+1])));
        var image=new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        UUID id=receipts.upload(new org.springframework.mock.web.MockMultipartFile("image","../../receipt.exe","text/plain",bytes.toByteArray()));assertTrue(receipts.image(id).length>0);
        var anonymous=app.browser();assertEquals(302,anonymous.get("/fuel/receipts/"+id).statusCode());anonymous.login();var picture=anonymous.get("/fuel/receipts/"+id);assertEquals(200,picture.statusCode());assertEquals("image/jpeg",picture.headers().firstValue("content-type").orElseThrow());assertTrue(picture.headers().firstValue("cache-control").orElseThrow().contains("no-store"));
        long purchase=fuel.savePurchase(purchaseInput(UUID.randomUUID(),today,0,"","",false,false,"SHARED","PETROL",id),OWNER);receipts.discard(id);assertTrue(receipts.image(id).length>0,"Linked image cannot be discarded");
        app.close();app=RunningApp.start();jdbc=app.context().getBean(JdbcTemplate.class);var restartedReceipts=app.context().getBean(pl.nclean.crm.fuel.ReceiptService.class);var restartedFuel=app.context().getBean(pl.nclean.crm.fuel.FuelJournalService.class);assertTrue(restartedReceipts.image(id).length>0);
        restartedFuel.savePurchase(new pl.nclean.crm.fuel.FuelJournalService.Purchase(purchase,0,UUID.randomUUID(),today.atStartOfDay(),"SHARED","PETROL","65","","",false,"",false,"Korekta",null),OWNER);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->restartedReceipts.image(id));
        assertThrows(IllegalArgumentException.class,()->restartedFuel.savePurchase(new pl.nclean.crm.fuel.FuelJournalService.Purchase(purchase,0,UUID.randomUUID(),today.atStartOfDay(),"SHARED","PETROL","65","","",false,"",false,"Korekta",null),OWNER));
    }
    @Test void fuelFormsRenderAndFailedDayOrPurchasePreserveInputs() throws Exception {
        var browser=app.browser();browser.login();var today=DashboardService.today();seedFuelParameters(today.minusDays(1));
        assertEquals(200,browser.get("/fuel?date="+today).statusCode());assertEquals(200,browser.get("/dashboard?date="+today).statusCode());
        var invalid=browser.settings("/fuel/day",Map.of("day",today,"version",-1,"odometer","1234","hours",24,"minutes",1),true);assertEquals(400,invalid.statusCode());assertTrue(invalid.body().contains("value=\"1234\""));assertTrue(invalid.body().contains("value=\"24\""));
        var purchase=new LinkedHashMap<String,Object>();purchase.put("purchased_at",today.atStartOfDay());purchase.put("version",-1);purchase.put("request_token",UUID.randomUUID());purchase.put("amount","123");purchase.put("target","GENERATOR");purchase.put("fuel_type","PETROL");purchase.put("full_tank","on");purchase.put("note","Zachowaj dane rachunku");
        var error=browser.settings("/fuel/purchases",purchase,true);assertEquals(400,error.statusCode());assertTrue(error.body().contains("Zachowaj dane rachunku"));assertTrue(error.body().contains("value=\"123\""));
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
