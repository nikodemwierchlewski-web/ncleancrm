package pl.nclean.crm.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.nclean.crm.records.CrmRecords;
import pl.nclean.crm.records.CustomerProfileCommand;
import pl.nclean.crm.removal.RemovalService;
import pl.nclean.crm.shared.BusinessDates;
import pl.nclean.crm.work.WorkService;
import static org.junit.jupiter.api.Assertions.*;

/** Domain fixtures and real SQL, on an isolated disposable PostgreSQL only. */
@Testcontainers
@Transactional
@SpringBootTest(properties={"spring.config.location=classpath:/application.yml","spring.profiles.active=integration","crm.admin.username=source-stats-test","crm.admin.password-hash=$2a$12$....................................................."})
class CustomerSourcePostgresIT {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>(
        "postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f")
        .withDatabaseName("source_stats_test").withUsername("source_stats_test").withPassword(UUID.randomUUID().toString());
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired CustomerSourceStatistics stats;
    @Autowired DashboardService dashboard;
    @Autowired CrmRecords records;
    @Autowired WorkService work;
    @Autowired RemovalService removal;
    @Autowired JdbcTemplate db;
    final LocalDate today=BusinessDates.today();
    static final AtomicInteger NEXT=new AtomicInteger();
    static BigDecimal n(String v){return new BigDecimal(v);}
    long customer(String source){
        return records.saveCustomerProfile(new CustomerProfileCommand(null,0,"Klient QA "+NEXT.incrementAndGet(),"500001"+String.format("%03d",NEXT.get()),"Adres QA","",true,true,source,""));
    }
    long version(long b){return ((Number)records.booking(b).get("version")).longValue();}
    long book(Long customer,LocalDate day,String price,String status){
        long service=db.queryForObject("SELECT min(id) FROM service_definition",Long.class);
        boolean historical=customer==null;
        long b=records.book(UUID.randomUUID(),day,LocalTime.NOON,customer,"","",historical?"":"Adres QA",Map.of(service,BigDecimal.ONE),Map.of(service,n(price)),Map.of(),"UNKNOWN","","Syntetyczne ustalenia",historical);
        if("DONE".equals(status)&&!historical)work.complete(b,version(b),price,"30","QA",Map.of(),Map.of(),List.of(),"qa");
        else if(!historical&&!"PLANNED".equals(status))records.reschedule(b,version(b),day,LocalTime.NOON,status);
        return b;
    }
    CustomerSourceStatistics.Report report(){return stats.report(today.minusDays(6),today,"actual");}
    CustomerSourceStatistics.Bar bar(CustomerSourceStatistics.Report r,String metric,String source){
        return r.series().stream().filter(s->s.key().equals(metric)).findFirst().orElseThrow().bars().stream().filter(b->b.code().equals(source)).findFirst().orElseThrow();
    }
    void amount(String expected,BigDecimal actual){assertEquals(0,n(expected).compareTo(actual));}

    @Test void emptyPeriodHasAllSourcesAndNoInvalidPercentages(){
        var report=report();assertEquals(0,report.clients());assertEquals(0,report.bookings());amount("0",report.revenue());
        for(var series:report.series()){
            assertEquals(6,series.bars().size());
            for(var bar:series.bars()){amount("0",bar.value());amount("0",bar.percentage());}
        }
    }
    @Test void uniqueCustomersRepeatedVisitsAndMultipleItemsDoNotMultiplyRevenue(){
        long google=customer("GOOGLE"),second=customer("GOOGLE"),facebook=customer("FACEBOOK");
        var ids=db.queryForList("SELECT id FROM service_definition ORDER BY id LIMIT 2",Long.class);
        long multi=records.book(UUID.randomUUID(),today,LocalTime.NOON,google,"","","Adres QA",Map.of(ids.get(0),BigDecimal.ONE,ids.get(1),BigDecimal.ONE),Map.of(ids.get(0),n("100"),ids.get(1),n("200")),Map.of(),"UNKNOWN","","Dwie usługi QA");
        work.complete(multi,version(multi),"300","60","QA",Map.of(),Map.of(),List.of(),"qa");
        book(google,today,"0","DONE");book(second,today,"100","DONE");book(facebook,today,"500","DONE");
        db.update("INSERT INTO booking_payment(booking_id,request_token,paid_on,amount,method,note) VALUES (?,?,?,999,'CASH','Dawny zapis QA')",multi,UUID.randomUUID(),today);
        var report=report();assertEquals(3,report.clients());assertEquals(4,report.bookings());amount("900",report.revenue());
        amount("2",bar(report,"clients","GOOGLE").value());amount("66.67",bar(report,"clients","GOOGLE").percentage());
        amount("3",bar(report,"bookings","GOOGLE").value());amount("75",bar(report,"bookings","GOOGLE").percentage());
        amount("400",bar(report,"revenue","GOOGLE").value());amount("44.44",bar(report,"revenue","GOOGLE").percentage());
        assertEquals("FACEBOOK",report.series().get(2).bars().get(0).code());
        amount(dashboard.summary(today,"day","actual").revenue().toPlainString(),report.revenue());
    }
    @Test void actualAndPlanRespectPeriodEdgesStatusAndRemoval(){
        long google=customer("GOOGLE"),facebook=customer("FACEBOOK");
        book(google,today.minusDays(6),"100","DONE");book(google,today,"200","DONE");
        book(google,today.minusDays(7),"900","DONE");book(google,today.plusDays(1),"800","PLANNED");
        book(google,today,"1000","CANCELLED");book(facebook,today,"300","PLANNED");
        long removed=book(google,today,"400","DONE");removal.change("booking",removed,version(removed),false,"qa");
        var actual=report();assertEquals(1,actual.clients());assertEquals(2,actual.bookings());amount("300",actual.revenue());
        var plan=stats.report(today.minusDays(6),today,"plan");assertEquals(2,plan.clients());assertEquals(3,plan.bookings());amount("600",plan.revenue());
        amount("300",bar(plan,"revenue","GOOGLE").value());amount("300",bar(plan,"revenue","FACEBOOK").value());
    }
    @Test void unknownAndUnlinkedHistoricalVisitsAreHonestAboutIdentityAndPrice(){
        book(customer("UNKNOWN"),today.minusDays(1),"200","DONE");book(null,today.minusDays(2),"100","DONE");
        var report=report();assertEquals(1,report.clients());assertEquals(2,report.bookings());assertEquals(1,report.unlinkedBookings());assertEquals(1,report.unconfirmedPrices());
        amount("1",bar(report,"clients","UNKNOWN").value());amount("2",bar(report,"bookings","UNKNOWN").value());amount("300",bar(report,"revenue","UNKNOWN").value());
    }
    @Test void currentProfileSourceCorrectsAttributionWithoutChangingOldBooking(){
        long c=customer("GOOGLE"),b=book(c,today.minusDays(1),"120","DONE");var before=records.booking(b);amount("1",bar(report(),"clients","GOOGLE").value());
        var p=records.customer(c);
        records.saveCustomerProfile(new CustomerProfileCommand(c,((Number)p.get("version")).longValue(),(String)p.get("name"),(String)p.get("phone"),(String)p.get("address"),"",true,true,"REFERRAL","Testowe polecenie"));
        amount("0",bar(report(),"clients","GOOGLE").value());amount("1",bar(report(),"clients","REFERRAL").value());amount("120",bar(report(),"revenue","REFERRAL").value());assertEquals(before,records.booking(b));
    }
    @Test void archivedCustomersAndZeroValueJobsStillCount(){
        long c=customer("OLX");book(c,today,"0","DONE");var p=records.customer(c);
        records.saveCustomerProfile(new CustomerProfileCommand(c,((Number)p.get("version")).longValue(),(String)p.get("name"),(String)p.get("phone"),(String)p.get("address"),"",false,false,null,null));
        var report=report();assertEquals(1,report.clients());assertEquals(1,report.bookings());amount("100",bar(report,"clients","OLX").percentage());amount("0",bar(report,"revenue","OLX").percentage());
    }
    @Test void restoreAndDateCorrectionMoveOneContributionWithoutChangingMoney(){
        long b=book(customer("OTHER"),today,"90.25","DONE");
        removal.change("booking",b,version(b),false,"qa");assertEquals(0,report().bookings());
        removal.change("booking",b,db.queryForObject("SELECT version FROM booking WHERE id=?",Long.class,b),true,"qa");assertEquals(1,report().bookings());amount("90.25",report().revenue());
        records.reschedule(b,version(b),today.minusDays(7),LocalTime.NOON,"DONE");assertEquals(0,report().bookings());
        records.reschedule(b,version(b),today,LocalTime.NOON,"DONE");assertEquals(1,report().bookings());amount("90.25",report().revenue());
    }
    @Test void invalidPeriodAndModeAreRejected(){
        assertThrows(IllegalArgumentException.class,()->stats.report(today,today.minusDays(1),"actual"));
        assertThrows(IllegalArgumentException.class,()->stats.report(today.minusDays(366),today,"actual"));
        assertThrows(IllegalArgumentException.class,()->stats.report(today,today,"other"));
    }
}
