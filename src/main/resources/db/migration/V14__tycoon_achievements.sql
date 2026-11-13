CREATE TABLE tycoon_award (
 code VARCHAR(24) PRIMARY KEY CHECK(code IN ('FIRST_CLEAN','TEN_CLEAN','FIFTY_CLEAN','FIRST_DAY','FIRST_GOAL')),
 earned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 source_key VARCHAR(40) NOT NULL,
 source_value BIGINT NOT NULL CHECK(source_value>0),
 origin VARCHAR(8) NOT NULL DEFAULT 'EVENT' CHECK(origin IN ('EVENT','HISTORY')),
 notified_at TIMESTAMPTZ
);
CREATE TABLE tycoon_write_guard(id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO tycoon_write_guard VALUES(1);
-- Recognition date is the migration time, not an invented historical completion time.
-- Existing milestones do not generate a flood of notifications after upgrading.
INSERT INTO tycoon_award(code,source_key,source_value,origin,notified_at)
 SELECT m.code,b.id::text,m.target,'HISTORY',CURRENT_TIMESTAMP
 FROM (VALUES ('FIRST_CLEAN',1),('TEN_CLEAN',10),('FIFTY_CLEAN',50)) m(code,target)
 JOIN (SELECT id,row_number() OVER(ORDER BY day,starts,id) n FROM booking
 WHERE status='DONE' AND day <= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Warsaw')::date) b ON b.n=m.target;
INSERT INTO tycoon_award(code,source_key,source_value,origin,notified_at)
 SELECT 'FIRST_GOAL',id::text,1,'HISTORY',CURRENT_TIMESTAMP FROM memo_entry
 WHERE kind='GOAL' AND completed ORDER BY id LIMIT 1;
