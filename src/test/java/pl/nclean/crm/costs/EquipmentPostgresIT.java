package pl.nclean.crm.costs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.nclean.crm.shared.BusinessDates;
import static org.junit.jupiter.api.Assertions.*;

/** Disposable PostgreSQL only. Exercises actual transactions, constraints and Flyway. */
@Testcontainers
@SpringBootTest(properties={"spring.config.location=classpath:/application.yml","spring.profiles.active=integration","crm.admin.username=equipment-test-owner","crm.admin.password-hash=$2a$12$....................................................."})
class EquipmentPostgresIT {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>(
            "postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f")
            .withDatabaseName("equipment_test").withUsername("equipment_test").withPassword(UUID.randomUUID().toString());
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired EquipmentService service;
    @Autowired CostService costs;
    @Autowired JdbcTemplate db;
    final LocalDate today=BusinessDates.today();
    static BigDecimal n(String value){return new BigDecimal(value);}
    long asset(String kind) {
        return service.saveAsset(new EquipmentService.AssetCommand(null,0,UUID.randomUUID(),"Sprzęt QA","Test","Model QA","","","",kind,true,today.minusMonths(4),today.minusMonths(3),null,today.minusMonths(1),false),"test-owner");
    }
    long counter(long asset,String name,String unit,String source) {
        return service.addCounter(new EquipmentService.CounterCommand(asset,UUID.randomUUID(),name,unit,source),"test-owner");
    }
    void read(long counter,LocalDate day,String value,String kind) {
        service.addReading(new EquipmentService.ReadingCommand(counter,service.counter(counter,today).version(),UUID.randomUUID(),day,n(value),kind,"QA"),"test-owner");
    }
    @Test void retriesHistoryIsolationAndArchive() {
        long a=asset("EQUIPMENT");var command=new EquipmentService.CounterCommand(a,UUID.randomUUID(),"Pompa","h","");
        long c=service.addCounter(command,"owner");assertEquals(c,service.addCounter(command,"owner"));
        assertThrows(IllegalArgumentException.class,()->service.addCounter(new EquipmentService.CounterCommand(a,command.token(),"Inny","h",""),"owner"));
        long motor=counter(a,"Silnik","h",null);read(c,today.minusDays(2),"100","ACTUAL");read(motor,today,"500","ESTIMATE");
        assertEquals(n("100.00"),service.counter(c,today).current());assertNull(service.counter(motor,today).actual());
        read(c,today.minusDays(4),"90","ACTUAL");
        assertThrows(IllegalArgumentException.class,()->read(c,today.minusDays(3),"101","ACTUAL"));
        assertThrows(IllegalArgumentException.class,()->read(c,today.plusDays(1),"110","ACTUAL"));
        read(c,today,"120","ESTIMATE");assertTrue(service.counter(c,today).estimated());
        assertEquals(n("120.00"),service.counter(c,today).current());assertEquals(n("100.00"),service.counter(c,today).actual());
        var archive=new EquipmentService.ArchiveCommand(a,service.asset(a).version(),UUID.randomUUID(),false);
        service.archive(archive,"owner");service.archive(archive,"owner");
        assertFalse(service.asset(a).active());assertEquals(4,service.readings(a,"all").size());
        assertThrows(IllegalArgumentException.class,()->read(c,today,"125","ACTUAL"));
        assertEquals(0,service.readings(a,"historical").size());assertEquals(4,service.readings(a,"current").size());
        service.archive(new EquipmentService.ArchiveCommand(a,service.asset(a).version(),UUID.randomUUID(),true),"owner");
        assertTrue(service.asset(a).active());
    }
    @Test void executionIsAtomicIdempotentAndSnoozeKeepsOverdueState() {
        long a=asset("EQUIPMENT"), c=counter(a,"Silnik","h",null);read(c,today,"101","ACTUAL");
        long plan=service.addPlan(new EquipmentService.PlanCommand(a,UUID.randomUUID(),"Wymiana oleju",c,today.minusDays(3),n("100"),30,n("100"),7,n("5")),"owner");
        assertEquals(0,service.plan(plan,today).rank());
        service.snooze(new EquipmentService.PlanAction(plan,0,UUID.randomUUID(),today.plusDays(2)),"owner");
        assertEquals(0,service.plan(plan,today).rank());assertTrue(service.attention(today).stream().noneMatch(p->p.id()==plan));
        BigDecimal before=costs.operating(today,today);
        var command=new EquipmentService.ExecutionCommand(plan,1,UUID.randomUUID(),today,n("101"),n("80.50"),"Wykonano QA");
        long result=service.execute(command,"owner");assertEquals(result,service.execute(command,"owner"));
        assertEquals(before.add(n("80.50")),costs.operating(today,today));
        assertEquals(1,service.executions(a,"all").size());assertEquals(1,service.expenses(a,"all").size());
        assertEquals(n("201.00"),service.plan(plan,today).dueValue());assertEquals(today.plusDays(30),service.plan(plan,today).dueOn());
        assertNull(service.plan(plan,today).snoozedUntil());
        assertThrows(IllegalArgumentException.class,()->service.execute(new EquipmentService.ExecutionCommand(plan,1,UUID.randomUUID(),today,n("101"),n("80.50"),"stale"),"owner"));
        assertThrows(IllegalArgumentException.class,()->service.execute(new EquipmentService.ExecutionCommand(plan,2,UUID.randomUUID(),today,null,null,"missing reading"),"owner"));
        assertEquals(1,service.executions(a,"all").size());
    }
    @Test void executionFailureRollsBackReadingCostAndPlan() {
        long a=asset("EQUIPMENT"),c=counter(a,"Pompa","h",null);read(c,today,"50","ACTUAL");
        long plan=service.addPlan(new EquipmentService.PlanCommand(a,UUID.randomUUID(),"Kontrola",c,today,n("60"),20,n("60"),3,n("5")),"owner");
        db.execute("CREATE OR REPLACE FUNCTION equipment_test_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.note='forced-test-failure' THEN RAISE EXCEPTION 'synthetic failure'; END IF; RETURN NEW; END $$");
        db.execute("CREATE TRIGGER equipment_test_failure BEFORE INSERT ON equipment_maintenance_execution FOR EACH ROW EXECUTE FUNCTION equipment_test_failure()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.execute(new EquipmentService.ExecutionCommand(plan,0,UUID.randomUUID(),today,n("60"),n("45"),"forced-test-failure"),"owner"));
            assertEquals(n("50.00"),service.counter(c,today).actual());assertEquals(1,service.counter(c,today).version());
            assertEquals(0,service.plan(plan,today).version());assertTrue(service.expenses(a,"all").isEmpty());assertTrue(service.executions(a,"all").isEmpty());
        } finally {db.execute("DROP TRIGGER equipment_test_failure ON equipment_maintenance_execution");db.execute("DROP FUNCTION equipment_test_failure()");}
    }
    @Test void canonicalFuelCountersAreNotCopiedAndCannotBeAssignedTwice() {
        long car=asset("CAR"), generator=asset("GENERATOR");
        long c=counter(car,"Drogomierz","km","CAR"),g=counter(generator,"Agregat","h","GENERATOR");
        assertNull(service.counter(c,today).actual());
        db.update("INSERT INTO fuel_baseline(target,started_at,reading,request_token,updated_by) VALUES ('CAR',?,?,?,'test')",today.atStartOfDay(),n("184200"),UUID.randomUUID());
        db.update("INSERT INTO fuel_baseline(target,started_at,reading,request_token,updated_by) VALUES ('GENERATOR',?,?,?,'test')",today.atStartOfDay(),n("96"),UUID.randomUUID());
        assertEquals(n("184200.00"),service.counter(c,today).actual());assertEquals(n("96.00"),service.counter(g,today).actual());
        assertThrows(IllegalArgumentException.class,()->read(c,today,"184210","ACTUAL"));
        assertThrows(IllegalArgumentException.class,()->counter(asset("CAR"),"Inny","km","CAR"));
        long plan=service.addPlan(new EquipmentService.PlanCommand(generator,UUID.randomUUID(),"Olej",g,null,n("100"),null,n("100"),7,n("5")),"owner");
        assertEquals(2,service.plan(plan,today).rank());
        service.execute(new EquipmentService.ExecutionCommand(plan,0,UUID.randomUUID(),today,null,null,"Z odczytu paliwa"),"owner");
        assertEquals(n("196.00"),service.plan(plan,today).dueValue());assertEquals(0,db.queryForObject("SELECT count(*) FROM equipment_reading WHERE counter_id IN (?,?)",Integer.class,c,g));
        long cp=service.addPlan(new EquipmentService.PlanCommand(car,UUID.randomUUID(),"Przegląd",c,today,null,null,null,7,n("5")),"owner");
        service.execute(new EquipmentService.ExecutionCommand(cp,0,UUID.randomUUID(),today,null,n("200"),"Pojazd"),"owner");
        assertEquals("CAR_SERVICE",service.expenses(car,"all").get(0).get("category"));
    }
    @Test void upgradingV20PreservesOldAssetsAndBills() {
        String schema="equipment_upgrade_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("20").load().migrate();
        var old=new JdbcTemplate(source);
        long id=old.queryForObject("INSERT INTO equipment_item(request_token,name,model,active) VALUES (?,'Historyczny','Stary model',false) RETURNING id",Long.class,UUID.randomUUID());
        old.update("INSERT INTO cost_entry(request_token,spent_on,category,title,amount,effect,asset_id) VALUES (?,'2026-01-12','EQUIPMENT_PURCHASE','Historyczna cena',3600,'INVESTMENT',?)",UUID.randomUUID(),id);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals("Historyczny",old.queryForObject("SELECT name FROM equipment_item WHERE id=?",String.class,id));
        assertFalse(old.queryForObject("SELECT active FROM equipment_item WHERE id=?",Boolean.class,id));
        assertFalse(old.queryForObject("SELECT history_complete FROM equipment_item WHERE id=?",Boolean.class,id));
        assertEquals(n("3600.00"),old.queryForObject("SELECT amount FROM cost_entry WHERE asset_id=?",BigDecimal.class,id));
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().validate();
    }
}
