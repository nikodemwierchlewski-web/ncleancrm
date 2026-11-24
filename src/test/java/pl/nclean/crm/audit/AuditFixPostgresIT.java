package pl.nclean.crm.audit;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.images.builder.Transferable;
import pl.nclean.crm.records.*;
import pl.nclean.crm.work.*;
import pl.nclean.crm.notes.NoteService;
import pl.nclean.crm.fuel.FuelJournalService;
import pl.nclean.crm.costs.CostService;
import pl.nclean.crm.shared.BusinessDates;
import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for the production paths reproduced by the TEST.6 audit. */
@Testcontainers
@Transactional
@SpringBootTest(properties={"spring.config.location=classpath:/application.yml","spring.profiles.active=integration","crm.admin.username=audit-owner","crm.admin.password-hash=$2a$12$....................................................."})
class AuditFixPostgresIT {
 static final String LEGACY_FINGERPRINT_SQL="""
        SELECT md5(jsonb_build_array(
          (SELECT COALESCE(jsonb_agg(to_jsonb(b)-'removed_at'-'historical'-'customer_type' ORDER BY id),'[]') FROM booking b WHERE b.removed_at IS NULL AND day=?),
          (SELECT COALESCE(jsonb_agg(to_jsonb(i) ORDER BY booking_id,position),'[]') FROM booking_item i WHERE booking_id IN (SELECT id FROM booking WHERE removed_at IS NULL AND day=?)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(a) ORDER BY booking_id,item_position,chemical_id),'[]') FROM booking_actual_chemical a WHERE booking_id IN (SELECT id FROM booking WHERE removed_at IS NULL AND day=?)),
          (SELECT COALESCE(jsonb_agg(jsonb_build_object('id',p.id,'booking_id',p.booking_id,'stage',p.stage,'name',p.original_name,'time',p.captured_at) ORDER BY p.id),'[]') FROM booking_photo p WHERE booking_id IN (SELECT id FROM booking WHERE removed_at IS NULL AND day=?)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(m) ORDER BY id),'[]') FROM booking_move m WHERE from_day=?)
        )::text)
        """;
 @Container static final PostgreSQLContainer<?> PG=new PostgreSQLContainer<>("postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f")
  .withDatabaseName("nclean_crm").withUsername("crm_bootstrap").withPassword(UUID.randomUUID().toString());
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);}
 @Autowired JdbcTemplate db; @Autowired CrmRecords records; @Autowired WorkService work;
 @Autowired ChemistryService chemistry; @Autowired ActualUsageService actual; @Autowired FuelJournalService fuel;
 @Autowired NoteService notes; @Autowired CostService costs;
 final LocalDate today=BusinessDates.today();
 static BigDecimal n(String s){return new BigDecimal(s);}
 long service(){return db.queryForObject("SELECT min(id) FROM service_definition",Long.class);}
 long book(UUID token,String amount,String note){long s=service();return records.book(token,today,LocalTime.NOON,null,"Audit QA","500009901","Adres QA",Map.of(s,BigDecimal.ONE),Map.of(s,n(amount)),Map.of(),"UNKNOWN","",note);}
 void observed(String code,String evidence){System.out.println("AUDIT_OBSERVED "+code+" "+evidence);}
 @Test void changedBookingPayloadIsRejectedAndExactRetryIsSafe(){
  UUID token=UUID.randomUUID();long b=book(token,"100","Pierwsza notatka QA");assertThrows(pl.nclean.crm.persistence.RequestReplay.Conflict.class,()->book(token,"900","Zmieniona notatka QA"));
  assertEquals(b,book(token,"100","Pierwsza notatka QA"));
  assertEquals(n("100.00"),records.booking(b).get("total"));assertEquals("Pierwsza notatka QA",records.booking(b).get("work_note"));
 }
 @Test void oldTokensRequireReviewAndExactRetryDoesNotUndoLaterCorrections(){
  UUID token=UUID.randomUUID();long b=book(token,"100","Pierwsza notatka QA");
  db.update("UPDATE booking SET work_note='Późniejsza korekta',version=version+1 WHERE id=?",b);
  assertEquals(b,book(token,"100","Pierwsza notatka QA"));assertEquals("Późniejsza korekta",records.booking(b).get("work_note"));
  db.update("DELETE FROM request_replay WHERE scope='BOOKING' AND token=?",token);
  assertThrows(pl.nclean.crm.persistence.RequestReplay.Conflict.class,()->book(token,"100","Pierwsza notatka QA"));
  assertEquals(1,db.queryForObject("SELECT count(*) FROM booking WHERE request_token=?",Integer.class,token));
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void failedChemicalAuditRollsBackQuantitiesAndVersion(){
  long chemical=chemistry.saveMaterial(null,0,"Rollback QA","ml","100","10");
  long b=book(UUID.randomUUID(),"100","Rollback QA");
  work.complete(b,0,"100","30","QA",Map.of(),Map.of(),List.of(),"audit");
  long version=db.queryForObject("SELECT actual_version FROM booking_item WHERE booking_id=? AND position=0",Long.class,b);
  var before=records.actuals(b);
  db.execute("ALTER TABLE chemical_usage_audit ADD CONSTRAINT qa_reject_actor CHECK(actor<>'reject-audit')");
  try {
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->actual.record(b,0,version,Map.of(chemical,n("9")),"reject-audit"));
   assertEquals(before,records.actuals(b));assertEquals(version,db.queryForObject("SELECT actual_version FROM booking_item WHERE booking_id=? AND position=0",Long.class,b));
   assertEquals(0,db.queryForObject("SELECT count(*) FROM chemical_usage_audit WHERE booking_id=? AND actor='reject-audit'",Integer.class,b));
  } finally {db.execute("ALTER TABLE chemical_usage_audit DROP CONSTRAINT qa_reject_actor");}
 }
 @Test void changedMemoAndCostPayloadsAreRejected(){
  UUID token=UUID.randomUUID();long note=notes.save(null,0,token,"NOTE","Pierwszy tytuł QA","Treść A",null,false,List.of());
  assertThrows(pl.nclean.crm.persistence.RequestReplay.Conflict.class,()->notes.save(null,0,token,"NOTE","Zmieniony tytuł QA","Treść B",null,false,List.of()));assertEquals("Pierwszy tytuł QA",notes.get(note).title());
  var fields=new HashMap<String,String>();fields.put("token",UUID.randomUUID().toString());fields.put("spent_on",today.toString());fields.put("category","OTHER");fields.put("title","Koszt QA");fields.put("amount","10");
  long cost=costs.save(fields,"audit");fields.put("amount","90");assertThrows(pl.nclean.crm.persistence.RequestReplay.Conflict.class,()->costs.save(fields,"audit"));assertEquals(n("10.00"),costs.entry(cost).get("amount"));
 }
 @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints={22,24})
 void migrationKeepsClosedDaysAndDoesNotApproveRealEdits(int startVersion) throws Exception {
  String schema="audit_upgrade_"+UUID.randomUUID().toString().replace("-","");
  var ds=new DriverManagerDataSource(PG.getJdbcUrl()+(PG.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,PG.getUsername(),PG.getPassword());
  Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target(Integer.toString(startVersion)).load().migrate();var old=new JdbcTemplate(ds);
  long c=old.queryForObject("INSERT INTO crm_customer(name,phone) VALUES ('Audit QA','500009902') RETURNING id",Long.class);
  long service=old.queryForObject("SELECT min(id) FROM service_definition",Long.class);
  var ids=new ArrayList<Long>();
  for(int i=0;i<3;i++){
   LocalDate day=today.minusDays(i);
   long b=old.queryForObject("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,status,total,final_total,work_minutes,completion_no_notes,photo_before_exception,photo_after_exception) VALUES (?,?,?,'09:00','Audit QA','500009902','Adres QA','DONE',100,100,30,TRUE,'QA','QA') RETURNING id",Long.class,UUID.randomUUID(),c,day);ids.add(b);
   old.update("INSERT INTO booking_item(booking_id,position,service_id,name,unit,quantity,price,amount,suggested_price,forecast_defined,chemical_cost,actual_defined,actual_no_chem,final_amount,work_minutes) VALUES (?,0,?,'QA','szt.',1,100,100,100,TRUE,0,TRUE,TRUE,100,30)",b,service);
   String hash=old.queryForObject(LEGACY_FINGERPRINT_SQL,String.class,day,day,day,day,day);
   old.update("INSERT INTO day_review(day,fingerprint,closed_by) VALUES (?,?,'audit')",day,hash);
  }
  String changedHash=old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,today.minusDays(1));
  old.update("UPDATE booking SET work_note='Real edit after closure' WHERE id=?",ids.get(1));
  // Historical additions alone must preserve the ordinary-day reminder suppression.
  old.update("INSERT INTO booking(request_token,customer_id,day,starts,customer_name,phone,address,status,total,historical) VALUES (?,?,?,'13:00','History QA','500009902','QA','DONE',10,TRUE)",UUID.randomUUID(),c,today.minusDays(2));
  var closure=old.queryForMap("SELECT closed_at,closed_by FROM day_review WHERE day=?",today);
  Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
  var field=DayReviewService.class.getDeclaredField("FINGERPRINT_SQL");field.setAccessible(true);String sql=(String)field.get(null);
  assertEquals(old.queryForObject(sql,String.class,today,today,today,today,today),old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,today));
  assertEquals(closure,old.queryForMap("SELECT closed_at,closed_by FROM day_review WHERE day=?",today));
  var changed=today.minusDays(1);assertEquals(changedHash,old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,changed));assertNotEquals(changedHash,old.queryForObject(sql,String.class,changed,changed,changed,changed,changed));
  field=DayReviewService.class.getDeclaredField("REGULAR_FINGERPRINT_SQL");field.setAccessible(true);String ordinary=(String)field.get(null);var historical=today.minusDays(2);
  assertEquals(old.queryForObject(ordinary,String.class,historical,historical,historical,historical,historical),old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,historical));
  assertNotEquals(old.queryForObject(sql,String.class,historical,historical,historical,historical,historical),old.queryForObject("SELECT fingerprint FROM day_review WHERE day=?",String.class,historical));
 }
 @Test void allNewFuelEntriesEnforceTheVehicleFuelPair(){
  var when=today.minusDays(1).atTime(12,0);
  assertThrows(IllegalArgumentException.class,()->fuel.saveQuickPurchase(new FuelJournalService.Purchase(null,0,UUID.randomUUID(),when,"CAR","PETROL","50","10","5",false,"1000",true,"Audit QA",null),false,"audit"));
  assertThrows(IllegalArgumentException.class,()->fuel.saveQuickPurchase(new FuelJournalService.Purchase(null,0,UUID.randomUUID(),when,"GENERATOR","DIESEL","50","10","5",false,null,true,"Audit QA",null),false,"audit"));
  UUID token=UUID.randomUUID();var good=new FuelJournalService.Purchase(null,0,token,when,"CAR","DIESEL","50","10","5",false,"1000",true,"Audit QA",null);
  long id=fuel.saveQuickPurchase(good,false,"audit");assertEquals(id,fuel.saveQuickPurchase(good,false,"audit"));
  assertThrows(pl.nclean.crm.persistence.RequestReplay.Conflict.class,()->fuel.saveQuickPurchase(new FuelJournalService.Purchase(null,0,token,when,"CAR","DIESEL","60","12","5",false,"1000",true,"Audit QA",null),false,"audit"));
 }
 @Test void chemistryCorrectionsKeepPriorActualValues(){
  long s=service();long chemical=chemistry.saveMaterial(null,0,"Środek Audit QA","ml","100","10");
  long sv=db.queryForObject("SELECT version FROM service_definition WHERE id=?",Long.class,s);chemistry.saveRecipe(s,sv,true,Map.of(chemical,n("10")));
  long b=book(UUID.randomUUID(),"100","QA");work.complete(b,0,"100","30","QA",Map.of(),Map.of(),List.of(),"audit");
  long count=db.queryForObject("SELECT count(*) FROM booking_audit WHERE booking_id=?",Long.class,b);
  actual.record(b,0,1,Map.of(chemical,n("8")));actual.record(b,0,2,Map.of(chemical,n("9")));
  assertEquals(n("9.000"),records.actuals(b).get(0).get("quantity"));assertEquals(count,db.queryForObject("SELECT count(*) FROM booking_audit WHERE booking_id=?",Long.class,b));
  assertEquals(3,db.queryForObject("SELECT count(*) FROM chemical_usage_audit WHERE booking_id=?",Integer.class,b));
  assertEquals("8.000",db.queryForObject("SELECT before_data->'chemicals'->0->>'quantity' FROM chemical_usage_audit WHERE booking_id=? ORDER BY id DESC LIMIT 1",String.class,b));
  assertFalse(actual.history(b).isEmpty());
 }
 @Test void timerRoundsOnlyTheAccumulatedTotal(){
  long b=book(UUID.randomUUID(),"100","QA");
  for(int i=0;i<3;i++){
   long v=((Number)work.order(b).get("version")).longValue();work.timer(b,v,true,"audit");
   db.update("UPDATE booking SET timer_started=CURRENT_TIMESTAMP-INTERVAL '10 seconds' WHERE id=?",b);
   work.timer(b,v+1,false,"audit");
  }
  assertEquals(1,work.order(b).get("work_minutes"));
  assertTrue(((Number)work.order(b).get("timer_seconds")).longValue()<60);
  long v=((Number)work.order(b).get("version")).longValue();work.timer(b,v,true,"audit");db.update("UPDATE booking SET timer_started=CURRENT_TIMESTAMP-INTERVAL '10 seconds' WHERE id=?",b);
  records.reschedule(b,v+1,today,LocalTime.NOON,"CANCELLED");assertEquals(1,work.order(b).get("work_minutes"));
  assertTrue(((Number)work.order(b).get("timer_seconds")).longValue()<60);
  records.reschedule(b,v+2,today,LocalTime.NOON,"PLANNED");work.timer(b,v+3,true,"audit");db.update("UPDATE booking SET timer_started=CURRENT_TIMESTAMP-INTERVAL '10 seconds' WHERE id=?",b);
  work.complete(b,v+4,"100",null,"QA",Map.of(),Map.of(),List.of(),"audit");assertEquals(1,work.order(b).get("work_minutes"));
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void actualPcBackupScriptRestoresAllPublicTablesAndRejectsCorruption() throws Exception {
  long b=book(UUID.randomUUID(),"123.45","Backup QA");
  byte[] photo={1,2,3,4,5};db.update("INSERT INTO booking_photo(id,request_token,booking_id,stage,original_name,image) VALUES (?,?,?,'BEFORE','qa.jpg',?)",UUID.randomUUID(),UUID.randomUUID(),b,photo);
  var before=fingerprints(db);
  assertEquals(0,PG.execInContainer("sh","-c","mkdir -p /run/secrets /backups; echo '127.0.0.1 postgres' >> /etc/hosts").getExitCode());
  PG.copyFileToContainer(Transferable.of(PG.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8),0600),"/run/secrets/postgres_admin_password");
  PG.copyFileToContainer(MountableFile.forHostPath(java.nio.file.Path.of("docker/pc/database.sh").toAbsolutePath()),"/tmp/audit_database.sh");
  assertEquals(0,PG.execInContainer("psql","-U","crm_bootstrap","-d","nclean_crm","-c","CREATE ROLE crm_dev LOGIN").getExitCode());
  var backup=PG.execInContainer("sh","/tmp/audit_database.sh","backup");assertEquals(0,backup.getExitCode(),backup.getStderr());
  String filename=backup.getStdout().lines().filter(l->l.startsWith("Backup verified: ")).findFirst().orElseThrow().substring(17).split(" ")[0];
  String target="nclean_restore_audit";var restored=PG.execInContainer("env","BACKUP_FILE="+filename,"RESTORE_DATABASE="+target,"sh","/tmp/audit_database.sh","restore-test");assertEquals(0,restored.getExitCode(),restored.getStderr());
  var copied=new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://"+PG.getHost()+":"+PG.getMappedPort(5432)+"/"+target,PG.getUsername(),PG.getPassword()));assertEquals(before,fingerprints(copied));
  assertEquals(0,PG.execInContainer("sh","-c","cp /backups/"+filename+" /backups/tampered.dump; cp /backups/"+filename+".sha256 /backups/tampered.dump.sha256; printf tamper >> /backups/tampered.dump").getExitCode());
  var rejected=PG.execInContainer("env","BACKUP_FILE=tampered.dump","RESTORE_DATABASE=nclean_restore_rejected","sh","/tmp/audit_database.sh","restore-test");assertNotEquals(0,rejected.getExitCode());assertEquals(0,db.queryForObject("SELECT count(*) FROM pg_database WHERE datname='nclean_restore_rejected'",Integer.class));assertEquals(before,fingerprints(db));
 }
 static Map<String,String> fingerprints(JdbcTemplate jdbc){
  var result=new TreeMap<String,String>();for(String table:jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name",String.class))result.put(table,jdbc.queryForObject("SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,'' ORDER BY to_jsonb(t)::text),'')) FROM public."+table+" t",String.class));return result;
 }
}
