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
    @Autowired DepreciationService depreciation;
    @Autowired pl.nclean.crm.dashboard.DashboardService dashboard;
    DepreciationService.Command depreciationCommand(long a,long version,UUID token,String initial,String residual,java.time.YearMonth start,int months,boolean enabled) {
        return new DepreciationService.Command(a,version,token,n(initial),n(residual),start,months,enabled);
    }
    BigDecimal assetAmount(long asset,LocalDate from,LocalDate to) {
        return depreciation.report(from,to).items().stream().filter(i->i.assetId()==asset).map(DepreciationService.Item::amount).findFirst().orElse(n("0.00"));
    }
    @Test void economicPlanReportsWithoutWritingAnyExpenseAndRetainsResidual() {
        long a=asset("EQUIPMENT");var start=java.time.YearMonth.of(today.getYear(),1);
        int count=db.queryForObject("SELECT count(*) FROM cost_entry",Integer.class);
        var raw=dashboard.summary(today,"month","actual");
        var c=depreciationCommand(a,-1,UUID.randomUUID(),"3600.00","0.00",start,36,true);
        long revision=depreciation.save(c,"owner");assertEquals(revision,depreciation.save(c,"owner"));
        assertEquals(n("100.00"),assetAmount(a,start.atDay(1),start.atEndOfMonth()));
        for(int i=0;i<3;i++)assertEquals(n("1200.00"),assetAmount(a,start.plusYears(i).atDay(1),start.plusYears(i).plusMonths(11).atEndOfMonth()));
        assertEquals(n("0.00"),assetAmount(a,start.minusMonths(1).atDay(1),start.minusMonths(1).atEndOfMonth()));
        var end=start.plusMonths(35).atEndOfMonth();var detail=depreciation.detail(a,end);
        assertEquals(n("3600.00"),detail.accrued());assertEquals(n("0.00"),detail.remaining());
        assertEquals(n("0.00"),assetAmount(a,end.plusDays(1),end.plusMonths(1)));
        assertEquals(count,db.queryForObject("SELECT count(*) FROM cost_entry",Integer.class));
        assertEquals(raw.remaining(),dashboard.summary(today,"month","actual").remaining());
        assertEquals(raw.costs(),dashboard.summary(today,"month","actual").costs());
    }
    @Test void correctionsPreserveClosedMonthsAndDoNotDoubleCountRevisions() {
        long a=asset("EQUIPMENT");var start=java.time.YearMonth.from(today).minusMonths(3);
        depreciation.save(depreciationCommand(a,-1,UUID.randomUUID(),"3600.00","0.00",start,36,true),"owner");
        var closed=start.plusMonths(2);assertEquals(n("100.00"),assetAmount(a,closed.atDay(1),closed.atEndOfMonth()));
        var update=depreciationCommand(a,0,UUID.randomUUID(),"3600.00","0.00",start,48,true);
        depreciation.save(update,"owner");depreciation.save(update,"owner");
        assertEquals(2,depreciation.history(a).size());
        assertEquals(n("100.00"),assetAmount(a,closed.atDay(1),closed.atEndOfMonth()));
        var current=java.time.YearMonth.from(today);
        assertEquals(n("73.33"),assetAmount(a,current.atDay(1),current.atEndOfMonth()));
        BigDecimal total=n("0.00");
        for(int i=0;i<48;i++){var month=start.plusMonths(i);total=total.add(assetAmount(a,month.atDay(1),month.atEndOfMonth()));}
        assertEquals(n("3600.00"),total);
        assertThrows(IllegalArgumentException.class,()->depreciation.save(depreciationCommand(a,1,UUID.randomUUID(),"200.00","0.00",start,48,true),"owner"));
        assertThrows(IllegalArgumentException.class,()->depreciation.save(depreciationCommand(a,0,UUID.randomUUID(),"4000.00","0.00",start,48,true),"owner"));
        assertThrows(IllegalArgumentException.class,()->depreciation.save(depreciationCommand(a,1,UUID.randomUUID(),"3600.00","0.00",start.plusMonths(1),48,true),"owner"));
        assertEquals(2,depreciation.history(a).size());
    }
    @Test void stopAndArchiveAreSeparateAndDailyRoundingMatchesTheReport() {
        long a=asset("EQUIPMENT");var start=java.time.YearMonth.from(today).minusMonths(2);
        depreciation.save(depreciationCommand(a,-1,UUID.randomUUID(),"1000.01","100.00",start,7,true),"owner");
        assertEquals(n("900.01"),assetAmount(a,start.atDay(1),start.plusMonths(6).atEndOfMonth()));
        var m=java.time.YearMonth.from(today);var report=depreciation.report(m.atDay(1),m.atEndOfMonth());
        assertEquals(report.total(),report.daily().values().stream().reduce(n("0.00"),BigDecimal::add));
        service.archive(new EquipmentService.ArchiveCommand(a,service.asset(a).version(),UUID.randomUUID(),false),"owner");
        assertEquals(n("128.57"),assetAmount(a,m.atDay(1),m.atEndOfMonth()));
        depreciation.save(depreciationCommand(a,0,UUID.randomUUID(),"1000.01","100.00",start,7,false),"owner");
        assertEquals(n("0.00"),assetAmount(a,m.atDay(1),m.atEndOfMonth()));
        assertEquals(n("128.57"),assetAmount(a,start.atDay(1),start.atEndOfMonth()));
        assertEquals(n("257.15"),depreciation.detail(a,today).accrued());
        assertEquals(n("742.86"),depreciation.detail(a,today).remaining());
        assertEquals(2,depreciation.history(a).size());
    }
    @Test void concurrentEconomicRetriesHaveOneRevisionAndMissingPlanIsNotZeroInformation() throws Exception {
        long a=asset("EQUIPMENT");assertNull(depreciation.detail(a,today).monthly());
        int missing=depreciation.report(today,today).unconfigured();assertTrue(missing>0);
        var c=depreciationCommand(a,-1,UUID.randomUUID(),"0.00","0.00",java.time.YearMonth.from(today),1,false);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->depreciation.save(c,"owner"));var second=executor.submit(()->depreciation.save(c,"owner"));
            assertEquals(first.get(),second.get());
        } finally {executor.shutdownNow();}
        assertEquals(1,depreciation.history(a).size());
        assertEquals(missing-1,depreciation.report(today,today).unconfigured());
        assertEquals(n("0.00"),depreciation.detail(a,today).monthly());
        assertThrows(IllegalArgumentException.class,()->depreciation.save(depreciationCommand(a,-1,c.token(),"1.00","0.00",c.startMonth(),1,false),"owner"));
    }
    @Test void economicScheduleFailureRollsBackAllGeneratedRows() {
        long a=asset("EQUIPMENT");
        db.execute("CREATE OR REPLACE FUNCTION depreciation_test_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF (SELECT asset_id FROM equipment_depreciation_revision WHERE id=NEW.revision_id)="+a+" THEN RAISE EXCEPTION 'synthetic failure'; END IF; RETURN NEW; END $$");
        db.execute("CREATE TRIGGER depreciation_test_failure BEFORE INSERT ON equipment_depreciation_charge FOR EACH ROW EXECUTE FUNCTION depreciation_test_failure()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class,()->depreciation.save(depreciationCommand(a,-1,UUID.randomUUID(),"3600.00","0.00",java.time.YearMonth.from(today),36,true),"owner"));
            assertNull(depreciation.latest(a));
        } finally {db.execute("DROP TRIGGER depreciation_test_failure ON equipment_depreciation_charge");db.execute("DROP FUNCTION depreciation_test_failure()");}
    }
    @Test void upgradingV21LeavesExistingEquipmentAndPricesUnchangedWithoutInventingPlans() {
        String schema="depreciation_upgrade_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("21").load().migrate();
        var old=new JdbcTemplate(source);
        long id=old.queryForObject("INSERT INTO equipment_item(request_token,name,active) VALUES (?,'Sprzęt historyczny',false) RETURNING id",Long.class,UUID.randomUUID());
        old.update("INSERT INTO cost_entry(request_token,spent_on,category,title,amount,effect,asset_id) VALUES (?,'2026-01-12','EQUIPMENT_PURCHASE','Historyczny zakup',3600,'INVESTMENT',?)",UUID.randomUUID(),id);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        assertFalse(old.queryForObject("SELECT active FROM equipment_item WHERE id=?",Boolean.class,id));
        assertEquals(n("3600.00"),old.queryForObject("SELECT amount FROM cost_entry WHERE asset_id=?",BigDecimal.class,id));
        assertEquals(0,old.queryForObject("SELECT count(*) FROM equipment_depreciation_revision",Integer.class));
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().validate();
    }
}
