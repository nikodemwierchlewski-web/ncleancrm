package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.nclean.crm.records.*;
import pl.nclean.crm.removal.RemovalService;
import pl.nclean.crm.shared.BusinessDates;
import pl.nclean.crm.work.WorkService;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@Transactional
@SpringBootTest(properties={"spring.config.location=classpath:/application.yml","spring.profiles.active=integration","crm.admin.username=insights-test","crm.admin.password-hash=$2a$12$....................................................."})
class CustomerInsightsPostgresIT {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>(
        "postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f")
        .withDatabaseName("insights_test").withUsername("insights_test").withPassword(UUID.randomUUID().toString());
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired CustomerInsights insights;
    @Autowired CrmRecords records;
    @Autowired WorkService work;
    @Autowired RemovalService removal;
    @Autowired JdbcTemplate db;
    static final AtomicInteger NEXT=new AtomicInteger();
    final LocalDate today=BusinessDates.today();
    static BigDecimal n(String s){return new BigDecimal(s);}
    static void amount(String expected,BigDecimal actual){assertNotNull(actual);assertEquals(0,n(expected).compareTo(actual));}
    long category(){String name="Kategoria QA "+UUID.randomUUID();records.category(null,name);return db.queryForObject("SELECT id FROM service_category WHERE name=?",Long.class,name);}
    long service(long category){String name="Usługa QA "+UUID.randomUUID();records.service(null,0,category,name,"szt.","100",30,true);return db.queryForObject("SELECT id FROM service_definition WHERE name=?",Long.class,name);}
    long customer(CustomerType type){return records.saveCustomerProfile(new CustomerProfileCommand(null,0,"Klient QA","500003"+String.format("%03d",NEXT.incrementAndGet()),"Adres QA","",true,true,"UNKNOWN","",type.name()));}
    long version(long b){return ((Number)records.booking(b).get("version")).longValue();}
    long customerVersion(long c){return ((Number)records.customer(c).get("version")).longValue();}
    long book(Long c,LocalDate day,Map<Long,BigDecimal> quantities,Map<Long,BigDecimal> prices,CustomerType type){
        return records.book(UUID.randomUUID(),day,LocalTime.of(9,0),c,"","",c==null?"":"Adres QA",quantities,prices,Map.of(),"UNKNOWN","","Syntetyczne ustalenia",c==null,type==null?null:new CustomerType.Choice(type,c==null?null:customerVersion(c)));
    }
    long done(long c,long s,LocalDate day,String price,CustomerType type){long b=book(c,day,Map.of(s,BigDecimal.ONE),Map.of(s,n(price)),type);finish(b,price,Map.of());return b;}
    void finish(long b,String total,Map<Integer,BigDecimal> split){work.complete(b,version(b),total,"30","Wykonanie QA",split,Map.of(),List.of(),"qa");}
    CustomerInsights.Report report(String type){return insights.report(today.minusDays(6),today,type);}
    CustomerInsights.Category cat(CustomerInsights.Report r,long id){return r.categories().stream().filter(c->c.id()==id).findFirst().orElseThrow();}

    @Test void averageMedianAndCategoriesUseVisitsRatherThanUnitsOrItems(){
        long a=category(),b=category(),s=service(a),second=service(a),other=service(b),c=customer(CustomerType.PRIVATE);
        long first=book(c,today,Map.of(s,n("2")),Map.of(s,n("50")),null);finish(first,"100",Map.of());
        long two=book(c,today,Map.of(s,BigDecimal.ONE,second,BigDecimal.ONE),Map.of(s,n("100"),second,n("100")),null);finish(two,"200",Map.of());
        done(c,s,today,"900",null);done(c,other,today,"50",null);done(c,other,today,"150",null);
        var r=report("ALL");assertEquals(5,r.overall().count());amount("280",r.overall().average());amount("150",r.overall().median());
        assertEquals(3,cat(r,a).values().count());amount("400",cat(r,a).values().average());amount("200",cat(r,a).values().median());
        assertEquals(2,cat(r,b).values().count());amount("100",cat(r,b).values().average());amount("100",cat(r,b).values().median());
    }
    @Test void mixedCategoriesUseFinalAllocationsOncePerCategory(){
        long a=category(),b=category(),s=service(a),second=service(a),other=service(b),c=customer(CustomerType.BUSINESS);
        var qty=new LinkedHashMap<Long,BigDecimal>();qty.put(s,BigDecimal.ONE);qty.put(second,BigDecimal.ONE);qty.put(other,BigDecimal.ONE);
        long job=book(c,today,qty,Map.of(),null);finish(job,"240",Map.of(0,n("40"),1,n("60"),2,n("140")));
        var r=report("BUSINESS");amount("240",r.overall().average());amount("100",cat(r,a).values().average());amount("140",cat(r,b).values().average());assertEquals(1,cat(r,a).values().count());assertEquals(0,r.missingSplit());
    }
    @Test void missingOrInconsistentSplitStaysOutOfCategoriesAndInOverall(){
        long a=category(),b=category(),s=service(a),other=service(b),c=customer(CustomerType.PRIVATE);
        var qty=new LinkedHashMap<Long,BigDecimal>();qty.put(s,BigDecimal.ONE);qty.put(other,BigDecimal.ONE);
        long job=book(c,today,qty,Map.of(),null);finish(job,"300",Map.of());
        var r=report("ALL");assertEquals(1,r.overall().count());amount("300",r.overall().median());assertEquals(1,r.missingSplit());assertEquals(1,cat(r,a).missingSplit());assertNull(cat(r,a).values().average());
        db.update("UPDATE booking_item SET final_amount=1 WHERE booking_id=?",job);assertEquals(1,report("ALL").missingSplit());
        finish(job,"300",Map.of(0,n("100"),1,n("200")));assertEquals(0,report("ALL").missingSplit());amount("100",cat(report("ALL"),a).values().median());
    }
    @Test void returnsUseAllEarlierTypesAndIncludeSecondVisitOfANewClient(){
        long s=service(category()),old=customer(CustomerType.PRIVATE),fresh=customer(CustomerType.BUSINESS);
        done(old,s,today.minusDays(20),"40",null);
        done(old,s,today.minusDays(5),"100",CustomerType.BUSINESS);done(old,s,today,"200",null);
        done(fresh,s,today.minusDays(4),"50",null);done(fresh,s,today.minusDays(1),"70",null);
        var r=report("BUSINESS");assertEquals(4,r.overall().count());amount("105",r.overall().average());amount("85",r.overall().median());
        assertEquals(2,r.returns().clients());assertEquals(1,r.returns().newClients());assertEquals(1,r.returns().returningClients());amount("50",r.returns().returningShare());
        assertEquals(3,r.returns().repeatVisits());amount("370",r.returns().repeatRevenue());amount("5",r.returns().medianDays());
    }
    @Test void sameDayReturnsAndUnlinkedHistoricalDataAreExplicit(){
        long s=service(category()),c=customer(CustomerType.PRIVATE);
        done(c,s,today,"0",null);done(c,s,today,"50",null);
        book(null,today.minusDays(1),Map.of(s,BigDecimal.ONE),Map.of(s,n("20")),CustomerType.UNKNOWN);
        var r=report("ALL");assertEquals(3,r.overall().count());assertEquals(1,r.unlinkedVisits());assertEquals(1,r.unconfirmedPrices());assertEquals(1,r.returns().clients());assertEquals(1,r.returns().newClients());assertEquals(0,r.returns().returningClients());assertEquals(1,r.returns().repeatVisits());amount("0",r.returns().medianDays());
    }
    @Test void removalRestoreAndCancelledHistoryRecomputeReturns(){
        long s=service(category()),c=customer(CustomerType.PRIVATE),old=done(c,s,today.minusDays(20),"50",null);
        long current=done(c,s,today,"100",null);assertEquals(1,report("ALL").returns().returningClients());
        removal.change("booking",old,version(old),false,"qa");assertEquals(1,report("ALL").returns().newClients());
        removal.change("booking",old,db.queryForObject("SELECT version FROM booking WHERE id=?",Long.class,old),true,"qa");assertEquals(1,report("ALL").returns().returningClients());
        records.reschedule(old,version(old),today.minusDays(20),LocalTime.NOON,"CANCELLED");assertEquals(1,report("ALL").returns().newClients());
        removal.change("booking",current,version(current),false,"qa");assertEquals(0,report("ALL").overall().count());assertNull(report("ALL").overall().median());
    }
    @Test void typeIsRememberedAndProfileCorrectionsDoNotRewriteVisits(){
        long s=service(category()),c=customer(CustomerType.PRIVATE);
        long b=done(c,s,today,"100",CustomerType.BUSINESS);assertEquals("BUSINESS",records.customer(c).get("customer_type"));assertEquals("BUSINESS",records.booking(b).get("customer_type"));
        var p=records.customer(c);records.saveCustomerProfile(new CustomerProfileCommand(c,customerVersion(c),(String)p.get("name"),(String)p.get("phone"),"Adres QA","",true,false,null,null,"PRIVATE"));
        assertEquals("BUSINESS",records.booking(b).get("customer_type"));long next=done(c,s,today,"200",null);assertEquals("PRIVATE",records.booking(next).get("customer_type"));assertEquals(1,report("BUSINESS").overall().count());assertEquals(1,report("PRIVATE").overall().count());
    }
    @Test void invalidTypeAndStaleVersionCannotPartiallyChangeProfileOrCreateAJob(){
        long s=service(category()),c=customer(CustomerType.PRIVATE);var before=records.customer(c);
        assertThrows(IllegalArgumentException.class,()->records.saveCustomerProfile(new CustomerProfileCommand(c,0,"Zmieniona nazwa",(String)before.get("phone"),"Adres QA","",true,false,null,null,"INVALID")));assertEquals(before,records.customer(c));
        records.saveCustomer(c,0,"Nowsza nazwa",(String)before.get("phone"),"Adres QA","",true);
        assertThrows(IllegalArgumentException.class,()->records.book(UUID.randomUUID(),today,LocalTime.NOON,c,"","","Adres QA",Map.of(s,BigDecimal.ONE),Map.of(),Map.of(),"UNKNOWN","","QA",false,new CustomerType.Choice(CustomerType.BUSINESS,0L)));
        assertEquals("PRIVATE",records.customer(c).get("customer_type"));assertEquals(0,report("ALL").overall().count());
    }
    @Test void lostResponseRetryDoesNotChangeTypeTwice(){
        long s=service(category()),c=customer(CustomerType.PRIVATE);UUID token=UUID.randomUUID();
        var choice=new CustomerType.Choice(CustomerType.BUSINESS,0L);
        long b=records.book(token,today,LocalTime.NOON,c,"","","Adres QA",Map.of(s,BigDecimal.ONE),Map.of(),Map.of(),"UNKNOWN","","QA",false,choice);
        assertEquals(b,records.book(token,today,LocalTime.NOON,c,"","","Adres QA",Map.of(s,BigDecimal.ONE),Map.of(),Map.of(),"UNKNOWN","","QA",false,choice));assertEquals(1,customerVersion(c));assertEquals("BUSINESS",records.booking(b).get("customer_type"));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void bookingFailureRollsBackTheProfileTypeChange(){
        long s=service(category()),c=customer(CustomerType.PRIVATE);var before=records.customer(c);
        db.execute("CREATE FUNCTION test_customer_type_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'synthetic failure'; END $$");
        db.execute("CREATE TRIGGER test_customer_type_failure BEFORE INSERT ON booking FOR EACH ROW EXECUTE FUNCTION test_customer_type_failure()");
        try{assertThrows(org.springframework.dao.DataAccessException.class,()->book(c,today,Map.of(s,BigDecimal.ONE),Map.of(),CustomerType.BUSINESS));assertEquals(before,records.customer(c));}
        finally{db.execute("DROP TRIGGER test_customer_type_failure ON booking");db.execute("DROP FUNCTION test_customer_type_failure()");}
    }
    @Test void categoryChangesUseCurrentGroupingWhileKeepingHistoricalAmounts(){
        long a=category(),b=category(),s=service(a),c=customer(CustomerType.PRIVATE),job=done(c,s,today,"123.45",null);var before=records.items(job);
        records.service(s,0,b,"Przeniesiona usługa QA","szt.","999",60,true);
        amount("123.45",cat(report("ALL"),b).values().median());assertTrue(report("ALL").categories().stream().noneMatch(k->k.id()==a));assertEquals(before,records.items(job));
    }
    @Test void emptyAndInvalidPeriodsHaveNoInventedValues(){
        var r=report("ALL");assertNull(r.overall().average());assertNull(r.overall().median());assertNull(r.returns().medianDays());assertEquals(0,r.returns().clients());
        assertThrows(IllegalArgumentException.class,()->insights.report(today,today.minusDays(1),"ALL"));assertThrows(IllegalArgumentException.class,()->insights.report(today.minusDays(366),today,"ALL"));assertThrows(IllegalArgumentException.class,()->report("INVALID"));
    }
    @Test void upgradeV23PreservesDataAndLeavesTypesUnknown() throws Exception {
        String schema="type_upgrade_"+UUID.randomUUID().toString().replace("-","");
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("23").load().migrate();var old=new JdbcTemplate(ds);
        long c=old.queryForObject("INSERT INTO crm_customer(name,phone,notes) VALUES ('QA','+48500003999','Dawne informacje QA') RETURNING id",Long.class);
        long b=old.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,total,status,final_total,work_note) VALUES (?,?,?,'09:00','QA','+48500003999','Adres QA',123.45,'DONE',111.11,'Dawna notatka QA') RETURNING id",Long.class,UUID.randomUUID(),c,today);
        var beforeCustomer=old.queryForMap("SELECT * FROM crm_customer WHERE id=?",c);var beforeBooking=old.queryForMap("SELECT * FROM booking WHERE id=?",b);var checksums=old.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE type='SQL' ORDER BY installed_rank");
        var field=pl.nclean.crm.work.DayReviewService.class.getDeclaredField("FINGERPRINT_SQL");field.setAccessible(true);String fingerprintSql=(String)field.get(null);
        String fingerprint=old.queryForObject(fingerprintSql,String.class,today,today,today,today,today);
        old.update("INSERT INTO day_review(day,fingerprint,closed_by) VALUES (?,?,'qa')",today,fingerprint);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals(fingerprint,old.queryForObject(fingerprintSql,String.class,today,today,today,today,today));assertEquals(fingerprint,old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,today));
        var afterCustomer=old.queryForMap("SELECT * FROM crm_customer WHERE id=?",c);var afterBooking=old.queryForMap("SELECT * FROM booking WHERE id=?",b);assertEquals("UNKNOWN",afterCustomer.remove("customer_type"));assertEquals("UNKNOWN",afterBooking.remove("customer_type"));assertEquals(0L,afterBooking.remove("timer_seconds"));assertEquals(beforeCustomer,afterCustomer);assertEquals(beforeBooking,afterBooking);assertEquals(checksums,old.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE type='SQL' AND version::integer<24 ORDER BY installed_rank"));
    }
}
