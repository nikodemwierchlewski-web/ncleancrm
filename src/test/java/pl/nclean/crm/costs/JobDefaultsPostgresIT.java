package pl.nclean.crm.costs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
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
import pl.nclean.crm.records.*;
import pl.nclean.crm.work.*;
import pl.nclean.crm.removal.RemovalService;
import pl.nclean.crm.shared.BusinessDates;
import static org.junit.jupiter.api.Assertions.*;

/** Uses only a newly created disposable database and synthetic records. */
@Testcontainers
@SpringBootTest(properties={"spring.config.location=classpath:/application.yml","spring.profiles.active=integration","crm.admin.username=job-defaults-test","crm.admin.password-hash=$2a$12$....................................................."})
class JobDefaultsPostgresIT {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>(
        "postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f")
        .withDatabaseName("job_defaults_test").withUsername("job_defaults_test").withPassword(UUID.randomUUID().toString());
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired EquipmentService equipment;
    @Autowired EquipmentUsageService usage;
    @Autowired CrmRecords records;
    @Autowired ChemistryService chemistry;
    @Autowired ActualUsageService actual;
    @Autowired CompletionDefaults defaults;
    @Autowired pl.nclean.crm.context.ContextService photos;
    @Autowired WorkService work;
    @Autowired DayReviewService review;
    @Autowired RemovalService removal;
    @Autowired JdbcTemplate db;
    final LocalDate today=BusinessDates.today();
    static BigDecimal n(String v){return new BigDecimal(v);}
    long service(){
        String name="Usługa QA "+UUID.randomUUID();
        long category=db.queryForObject("SELECT min(id) FROM service_category",Long.class);
        records.service(null,0,category,name,"szt.","100",30,true);
        return db.queryForObject("SELECT id FROM service_definition WHERE name=?",Long.class,name);
    }
    long version(long id){return ((Number)records.booking(id).get("version")).longValue();}
    long serviceVersion(long id){return db.queryForObject("SELECT version FROM service_definition WHERE id=?",Long.class,id);}
    long counter(String kind,String unit){
        long asset=equipment.saveAsset(new EquipmentService.AssetCommand(null,0,UUID.randomUUID(),"Urządzenie QA","","","","","",kind,true,null,null,null,null,false),"qa");
        return equipment.addCounter(new EquipmentService.CounterCommand(asset,UUID.randomUUID(),"Silnik",unit,null),"qa");
    }
    EquipmentUsageService.StandardCommand standardCommand(Long id,long version,long service,long counter,String minutes){
        return new EquipmentUsageService.StandardCommand(id,version,service,serviceVersion(service),"szt.",counter,n(minutes),true,UUID.randomUUID());
    }
    long standard(long service,long counter,String minutes){return usage.saveStandard(standardCommand(null,0,service,counter,minutes),"qa");}
    long book(long service,String quantity,LocalDate day){
        return records.book(UUID.randomUUID(),day,LocalTime.of(9,0),null,"","500000901","Adres QA",Map.of(service,n(quantity)),Map.of(),Map.of(),"UNKNOWN","","Testowe ustalenia",false);
    }
    void complete(long booking,String duration){work.complete(booking,version(booking),"250",duration,"Testowe wykonanie",Map.of(),Map.of(),List.of(),"qa");}
    EquipmentUsageService.OverrideCommand overrideCommand(long booking,boolean zero,Map<Long,BigDecimal> minutes){
        return new EquipmentUsageService.OverrideCommand(booking,version(booking),usage.job(booking).version(),UUID.randomUUID(),zero,minutes);
    }
    void assertMinutes(long booking,long counter,String expected){
        assertEquals(0,n(expected).compareTo(usage.job(booking).usages().stream().filter(u->u.counterId()==counter).findFirst().orElseThrow().minutes()));
    }
    @Test void noteIsMandatoryForOrdinaryAndHistoricalCreationWithoutPartialWrites(){
        long s=service();int bookings=db.queryForObject("SELECT count(*) FROM booking",Integer.class),people=db.queryForObject("SELECT count(*) FROM crm_customer",Integer.class);
        for(boolean historical:List.of(false,true))for(String note:Arrays.asList(null,""," \n\t ")){
            var failure=assertThrows(IllegalArgumentException.class,()->records.book(UUID.randomUUID(),historical?today.minusDays(1):today,LocalTime.NOON,null,"","500000942","Adres QA",Map.of(s,n("1")),Map.of(),Map.of(),"UNKNOWN","",note,historical));
            assertTrue(failure.getMessage().contains("notatkę"));
        }
        assertEquals(bookings,db.queryForObject("SELECT count(*) FROM booking",Integer.class));assertEquals(people,db.queryForObject("SELECT count(*) FROM crm_customer",Integer.class));
        long b=book(s,"1",today);assertEquals("Testowe ustalenia",records.booking(b).get("work_note"));
        long named=records.book(UUID.randomUUID(),today,LocalTime.NOON,null,"Klient nieustalony","500000942","Adres QA",Map.of(s,n("1")),Map.of(),Map.of(),"UNKNOWN","","Wyraźnie podana nazwa");
        assertEquals("Klient nieustalony",records.booking(named).get("customer_name"));
    }
    @Test void phoneAloneCreatesReadableCustomerAndFrozenVisit(){
        long b=book(service(),"1",today);
        assertEquals("+48500000901",records.booking(b).get("customer_name"));
        long customer=((Number)records.booking(b).get("customer_id")).longValue();
        assertEquals("+48500000901",records.customer(customer).get("name"));
        records.saveCustomer(customer,0,"Imię później","500000901","Adres QA","",true);
        assertEquals("+48500000901",records.booking(b).get("customer_name"));
        assertThrows(IllegalArgumentException.class,()->records.saveCustomer(null,0,"","","","",true));
    }
    @Test void frozenStandardsScalePerUnitAndOnlyDoneJobsCount(){
        long s=service(),c=counter("EQUIPMENT","h"),pump=counter("EQUIPMENT","h");
        long std=standard(s,c,"12.50");standard(s,pump,"5");long b=book(s,"2.50",today);
        assertMinutes(b,c,"31.25");assertMinutes(b,pump,"12.50");assertNull(equipment.counter(c,today).current());
        usage.saveStandard(standardCommand(std,0,s,c,"90"),"qa");
        assertMinutes(b,c,"31.25");long next=book(s,"1",today);assertMinutes(next,c,"90");
        complete(b,"");assertEquals(n("0.52"),equipment.counter(c,today).current());
        complete(b,"");assertEquals(n("0.52"),equipment.counter(c,today).current());
        assertNull(equipment.counter(c,today).actual());
        assertEquals(0,db.queryForObject("SELECT count(*) FROM equipment_reading WHERE counter_id=?",Integer.class,c));
        assertEquals(n("250.00"),work.paid(b));
    }
    @Test void statusMoveRemovalAndRestoreProjectUsageOnce(){
        long s=service(),c=counter("EQUIPMENT","h");standard(s,c,"60");long b=book(s,"2",today.minusDays(2));complete(b,"");
        assertEquals(n("2.00"),equipment.counter(c,today).current());
        records.reschedule(b,version(b),today.minusDays(2),LocalTime.NOON,"PLANNED");assertNull(equipment.counter(c,today).current());
        records.reschedule(b,version(b),today.minusDays(2),LocalTime.NOON,"CANCELLED");assertNull(equipment.counter(c,today).current());
        records.reschedule(b,version(b),today.minusDays(1),LocalTime.NOON,"PLANNED");complete(b,"");
        assertNull(equipment.counter(c,today.minusDays(2)).current());assertEquals(n("2.00"),equipment.counter(c,today).current());
        removal.change("booking",b,version(b),false,"qa");assertNull(equipment.counter(c,today).current());
        long v=db.queryForObject("SELECT version FROM booking WHERE id=?",Long.class,b);
        removal.change("booking",b,v,true,"qa");assertEquals(n("2.00"),equipment.counter(c,today).current());
    }
    @Test void overridesReplaceTotalsZeroIsExplicitAndGeneratorIsIndependent(){
        long s=service(),c=counter("EQUIPMENT","h"),g=counter("GENERATOR","h"),car=counter("CAR","km");
        standard(s,c,"60");long b=book(s,"2",today);complete(b,"");
        var cmd=overrideCommand(b,false,Map.of(c,n("30"),g,n("15")));
        usage.override(cmd,"qa");usage.override(cmd,"qa");assertMinutes(b,c,"30");assertMinutes(b,g,"15");
        assertEquals(n("0.50"),equipment.counter(c,today).current());assertEquals(n("0.25"),equipment.counter(g,today).current());
        assertThrows(IllegalArgumentException.class,()->usage.override(new EquipmentUsageService.OverrideCommand(b,version(b),cmd.version(),UUID.randomUUID(),false,Map.of(c,n("1"))),"qa"));
        assertThrows(IllegalArgumentException.class,()->usage.override(overrideCommand(b,false,Map.of(car,n("1"))),"qa"));
        usage.override(overrideCommand(b,true,Map.of()),"qa");assertMinutes(b,c,"0");assertNull(equipment.counter(c,today).current());
        usage.override(overrideCommand(b,false,Map.of(c,BigDecimal.ZERO)),"qa");assertMinutes(b,c,"0");
        usage.override(overrideCommand(b,false,Map.of()),"qa");assertMinutes(b,c,"120");
        assertThrows(IllegalArgumentException.class,()->standard(s,g,"10"));
    }
    @Test void absoluteReadingsAnchorOnlyLaterDaysAndRoundAfterSumming(){
        long s=service(),c=counter("EQUIPMENT","h");standard(s,c,"0.20");
        long before=book(s,"1",today.minusDays(3));complete(before,"");
        equipment.addReading(new EquipmentService.ReadingCommand(c,0,UUID.randomUUID(),today.minusDays(2),n("100"),"ACTUAL","QA"),"qa");
        long same=book(s,"1",today.minusDays(2));complete(same,"");
        long one=book(s,"1",today.minusDays(1)),two=book(s,"1",today);complete(one,"");complete(two,"");
        assertEquals(n("100.01"),equipment.counter(c,today).current());assertEquals(n("100.00"),equipment.counter(c,today).actual());
        equipment.addReading(new EquipmentService.ReadingCommand(c,1,UUID.randomUUID(),today,n("110"),"ACTUAL","QA"),"qa");
        assertEquals(n("110.00"),equipment.counter(c,today).current());assertFalse(equipment.counter(c,today).estimated());
    }
    @Test void concurrentRetriesKeepOneOverrideAndAudit() throws Exception {
        long s=service(),c=counter("EQUIPMENT","h");standard(s,c,"60");long b=book(s,"1",today);complete(b,"");
        var command=overrideCommand(b,false,Map.of(c,n("30")));var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->usage.override(command,"qa"));var other=pool.submit(()->usage.override(command,"qa"));assertEquals(b,a.get(15,TimeUnit.SECONDS));assertEquals(b,other.get(15,TimeUnit.SECONDS));}
        finally {pool.shutdownNow();}
        assertEquals(n("0.50"),equipment.counter(c,today).current());assertEquals(1,db.queryForObject("SELECT count(*) FROM equipment_usage_action WHERE request_token=?",Integer.class,command.token()));
        assertEquals(1,usage.job(b).version());
    }
    @Test void legacyEquipmentNeedsExplicitPreparationAndChangedUnitsDoNotMatch(){
        long s=service(),c=counter("EQUIPMENT","h");standard(s,c,"60");long b=book(s,"1",today);
        // Reproduce a pre-V23 booking, without an automatic backfill.
        db.update("DELETE FROM booking_equipment_state WHERE booking_id=?",b);
        assertFalse(usage.job(b).prepared());assertTrue(usage.job(b).usages().isEmpty());
        var cmd=new EquipmentUsageService.PrepareCommand(b,version(b),UUID.randomUUID());usage.prepare(cmd,"qa");usage.prepare(cmd,"qa");
        assertEquals("EXPLICIT",usage.job(b).source());assertMinutes(b,c,"60");
        db.update("UPDATE service_definition SET unit='m2',version=version+1 WHERE id=?",s);
        long other=book(s,"1",today);assertTrue(usage.job(other).usages().isEmpty());assertEquals(1,usage.job(other).missingItems());assertMinutes(b,c,"60");
    }
    @Test void bookingFailureRollsBackCustomerItemsAndAllSnapshots(){
        long s=service(),c=counter("EQUIPMENT","h");standard(s,c,"60");int before=db.queryForObject("SELECT count(*) FROM booking",Integer.class),people=db.queryForObject("SELECT count(*) FROM crm_customer",Integer.class);
        db.execute("CREATE FUNCTION job_test_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'synthetic snapshot failure'; END $$");
        db.execute("CREATE TRIGGER job_test_failure BEFORE INSERT ON booking_equipment_snapshot FOR EACH ROW EXECUTE FUNCTION job_test_failure()");
        try {assertThrows(org.springframework.dao.DataAccessException.class,()->book(s,"1",today));assertEquals(before,db.queryForObject("SELECT count(*) FROM booking",Integer.class));assertEquals(people,db.queryForObject("SELECT count(*) FROM crm_customer",Integer.class));}
        finally {db.execute("DROP TRIGGER job_test_failure ON booking_equipment_snapshot");db.execute("DROP FUNCTION job_test_failure()");}
    }
    @Test void blankChemistryAndTimeUseFrozenStandardsNotCurrentPricesAndExcludeLearning(){
        long s=service(),chemical=chemistry.saveMaterial(null,0,"Środek QA "+UUID.randomUUID(),"l","1","20");
        chemistry.saveRecipe(s,serviceVersion(s),true,Map.of(chemical,n("10")));
        for(int sample=0;sample<5;sample++){
            long b=book(s,"2",today);db.update("UPDATE service_definition SET minutes=90 WHERE id=?",s);
            chemistry.saveMaterial(chemical,sample,"Środek QA","l","1","100");complete(b,"");
            assertEquals("STANDARD",records.items(b).get(0).get("actual_source"));assertEquals("STANDARD",work.order(b).get("work_time_source"));
            assertEquals(sample==0?60:180,work.order(b).get("work_minutes"));
            assertEquals(n(sample==0?"0.40":"2.00"),records.actuals(b).get(0).get("amount"));
            var rows=records.actuals(b);complete(b,"");assertEquals(rows,records.actuals(b));
        }
        assertFalse(actual.suggestions().containsKey(s));
    }
    @Test void measurementsAndZeroOverrideStandardsWhileMissingRecipeStaysUnknown(){
        long s=service(),chem=chemistry.saveMaterial(null,0,"Pomiar QA "+UUID.randomUUID(),"ml","100","10");
        chemistry.saveRecipe(s,serviceVersion(s),true,Map.of(chem,n("10")));long b=book(s,"1",today);
        complete(b,"17");assertEquals("MANUAL",work.order(b).get("work_time_source"));assertEquals(17,work.order(b).get("work_minutes"));
        long v=((Number)records.items(b).get(0).get("actual_version")).longValue();actual.record(b,0,v,Map.of(chem,n("3")));complete(b,"17");
        assertEquals("MEASURED",records.items(b).get(0).get("actual_source"));assertEquals(n("3.000"),records.actuals(b).get(0).get("quantity"));
        v=((Number)records.items(b).get(0).get("actual_version")).longValue();actual.zero(b,0,v);complete(b,"");assertEquals("NONE",records.items(b).get(0).get("actual_source"));
        long unknown=book(service(),"1",today);complete(unknown,"");assertFalse((Boolean)records.items(unknown).get(0).get("actual_defined"));
    }
    @Test void photoAndTimerTimeHavePriorityOverStandards(){
        for(String source:List.of("PHOTO","TIMER")){
            long b=book(service(),"2",today);db.update("UPDATE booking SET work_minutes=13,work_time_source=? WHERE id=?",source,b);
            complete(b,"");assertEquals(13,work.order(b).get("work_minutes"));assertEquals(source,work.order(b).get("work_time_source"));
        }
    }
    @Test void laterPhotosReplaceEstimatedTimeAndTimerDoesNotAccumulateIt() throws Exception {
        long b=book(service(),"2",today);complete(b,"");
        byte[] image;
        try(var stream=new java.io.ByteArrayOutputStream()){
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"jpg",stream);image=stream.toByteArray();
        }
        String date=today.toString().replace("-","");
        photos.upload(b,"BEFORE",UUID.randomUUID(),new org.springframework.mock.web.MockMultipartFile("image","IMG_"+date+"_100000.jpg","image/jpeg",image),"qa",version(b));
        assertEquals("STANDARD",work.order(b).get("work_time_source"));assertEquals(60,work.order(b).get("work_minutes"));
        photos.upload(b,"AFTER",UUID.randomUUID(),new org.springframework.mock.web.MockMultipartFile("image","IMG_"+date+"_101700.jpg","image/jpeg",image),"qa",version(b));
        assertEquals("PHOTO",work.order(b).get("work_time_source"));assertEquals(17,work.order(b).get("work_minutes"));
        long another=book(service(),"2",today);complete(another,"");records.reschedule(another,version(another),today,LocalTime.NOON,"PLANNED");
        work.timer(another,version(another),true,"qa");assertNull(work.order(another).get("work_minutes"));work.timer(another,version(another),false,"qa");
        assertTrue(((Number)work.order(another).get("work_minutes")).intValue()<10);
    }
    @Test void closeoutAcceptsDefaultCustomAndZeroAndRollsBackStaleChemicalVersion(){
        long s=service(),chem=chemistry.saveMaterial(null,0,"Zamknięcie QA "+UUID.randomUUID(),"ml","100","10");chemistry.saveRecipe(s,serviceVersion(s),true,Map.of(chem,n("10")));
        long b=book(s,"1",today);var p=new HashMap<String,String>();p.put("version","0");p.put("outcome","DONE");p.put("final_total","250");p.put("minutes","");p.put("note","Wykonano testowo");p.put("chem_0","KEEP");
        review.save(b,today,p,"qa");assertEquals("STANDARD",records.items(b).get(0).get("actual_source"));
        p.put("version",Long.toString(version(b)));p.put("chem_0","MEASURED");p.put("actualVersion_0","1");p.put("usage_0_"+chem,"7");review.save(b,today,p,"qa");
        assertEquals(n("7.000"),records.actuals(b).get(0).get("quantity"));
        p.put("version",Long.toString(version(b)));p.put("chem_0","ZERO");p.put("actualVersion_0","1");long before=version(b);
        assertThrows(IllegalArgumentException.class,()->review.save(b,today,p,"qa"));assertEquals(before,version(b));assertEquals("MEASURED",records.items(b).get(0).get("actual_source"));
        p.put("actualVersion_0","2");review.save(b,today,p,"qa");assertEquals("NONE",records.items(b).get(0).get("actual_source"));
        p.put("version",Long.toString(version(b)));p.put("chem_0","STANDARD");p.put("actualVersion_0","3");review.save(b,today,p,"qa");assertEquals("STANDARD",records.items(b).get(0).get("actual_source"));
    }
    @Test void upgradingV22PreservesMeasurementsPricesAndDoesNotBackfillUsage(){
        String schema="job_upgrade_"+UUID.randomUUID().toString().replace("-","");
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("22").load().migrate();var old=new JdbcTemplate(ds);
        long s=old.queryForObject("SELECT min(id) FROM service_definition",Long.class);
        long b=old.queryForObject("INSERT INTO booking(request_token,day,starts,customer_name,phone,address,total,status,work_minutes,work_time_source,historical) VALUES (?,?,'09:00','QA','+48500000901','Adres QA',123,'DONE',17,'MANUAL',TRUE) RETURNING id",Long.class,UUID.randomUUID(),today);
        old.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount,suggested_price,actual_defined) VALUES (?,0,?,'QA','szt.',1,123,123,123,TRUE)",b,s);
        var before=old.queryForMap("SELECT price,amount,actual_defined FROM booking_item WHERE booking_id=?",b);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals(before,old.queryForMap("SELECT price,amount,actual_defined FROM booking_item WHERE booking_id=?",b));
        assertEquals("MEASURED",old.queryForObject("SELECT actual_source FROM booking_item WHERE booking_id=?",String.class,b));
        assertEquals(17,old.queryForObject("SELECT work_minutes FROM booking WHERE id=?",Integer.class,b));
        assertNull(old.queryForObject("SELECT standard_work_minutes FROM booking_item WHERE booking_id=?",BigDecimal.class,b));
        assertEquals(0,old.queryForObject("SELECT count(*) FROM booking_equipment_state",Integer.class));
    }
}
