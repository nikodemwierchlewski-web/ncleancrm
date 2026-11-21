-- No backfill: existing bookings need explicit preparation with today's standards.
CREATE TABLE equipment_usage_standard (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 service_id BIGINT NOT NULL REFERENCES service_definition(id),
 counter_id BIGINT NOT NULL REFERENCES equipment_counter(id),
 unit VARCHAR(20) NOT NULL,
 minutes_per_unit NUMERIC(10,2) NOT NULL CHECK(minutes_per_unit BETWEEN 0 AND 10080),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(service_id,counter_id)
);
CREATE TABLE booking_equipment_state (
 booking_id BIGINT PRIMARY KEY REFERENCES booking(id) ON DELETE CASCADE,
 version BIGINT NOT NULL DEFAULT 0,
 no_equipment BOOLEAN NOT NULL DEFAULT FALSE,
 prepared_source VARCHAR(12) NOT NULL CHECK(prepared_source IN ('BOOKING','EXPLICIT')),
 prepared_by VARCHAR(120) NOT NULL,
 prepared_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE booking_equipment_snapshot (
 booking_id BIGINT NOT NULL REFERENCES booking_equipment_state(booking_id) ON DELETE CASCADE,
 item_position INTEGER NOT NULL,
 counter_id BIGINT NOT NULL REFERENCES equipment_counter(id),
 standard_id BIGINT NOT NULL REFERENCES equipment_usage_standard(id),
 standard_version BIGINT NOT NULL,
 unit VARCHAR(20) NOT NULL,
 quantity NUMERIC(10,2) NOT NULL CHECK(quantity>0),
 minutes_per_unit NUMERIC(10,2) NOT NULL CHECK(minutes_per_unit>=0),
 PRIMARY KEY(booking_id,item_position,counter_id),
 FOREIGN KEY(booking_id,item_position) REFERENCES booking_item(booking_id,position) ON DELETE CASCADE
);
CREATE INDEX booking_equipment_snapshot_counter ON booking_equipment_snapshot(counter_id,booking_id);
CREATE TABLE booking_equipment_override (
 booking_id BIGINT NOT NULL REFERENCES booking_equipment_state(booking_id) ON DELETE CASCADE,
 counter_id BIGINT NOT NULL REFERENCES equipment_counter(id),
 minutes NUMERIC(12,2) NOT NULL CHECK(minutes BETWEEN 0 AND 100800000),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 PRIMARY KEY(booking_id,counter_id)
);
CREATE TABLE equipment_usage_action (
 request_token UUID PRIMARY KEY,
 operation VARCHAR(12) NOT NULL CHECK(operation IN ('STANDARD','PREPARE','OVERRIDE')),
 subject_id BIGINT NOT NULL,
 payload JSONB NOT NULL,
 before_data JSONB,
 after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE VIEW equipment_job_usage AS
 WITH planned AS (
   SELECT booking_id,counter_id,SUM(quantity*minutes_per_unit) minutes
   FROM booking_equipment_snapshot GROUP BY booking_id,counter_id
 ), keys AS (
   SELECT booking_id,counter_id FROM planned
   UNION SELECT booking_id,counter_id FROM booking_equipment_override WHERE active
 )
 SELECT k.booking_id,k.counter_id,c.asset_id,b.day,b.status,b.historical,b.removed_at,
        s.no_equipment,s.prepared_source,
        COALESCE(p.minutes,0::numeric) standard_minutes,o.minutes override_minutes,
        CASE WHEN s.no_equipment THEN 0::numeric ELSE COALESCE(o.minutes,p.minutes,0::numeric) END minutes
 FROM keys k JOIN booking_equipment_state s ON s.booking_id=k.booking_id
 JOIN booking b ON b.id=k.booking_id JOIN equipment_counter c ON c.id=k.counter_id
 LEFT JOIN planned p ON p.booking_id=k.booking_id AND p.counter_id=k.counter_id
 LEFT JOIN booking_equipment_override o ON o.booking_id=k.booking_id AND o.counter_id=k.counter_id AND o.active;

-- Defaults are explicitly marked as estimates; historical measurements remain measurements.
ALTER TABLE booking_item ADD COLUMN actual_source VARCHAR(8) NOT NULL DEFAULT 'UNKNOWN'
 CHECK(actual_source IN ('UNKNOWN','MEASURED','STANDARD','NONE'));
UPDATE booking_item SET actual_source=CASE WHEN actual_no_chem THEN 'NONE' WHEN actual_defined THEN 'MEASURED' ELSE 'UNKNOWN' END;
ALTER TABLE booking_item ADD COLUMN standard_work_minutes NUMERIC(14,4) CHECK(standard_work_minutes>=0);
ALTER TABLE booking_item ADD COLUMN standard_time_origin VARCHAR(12) CHECK(standard_time_origin IN ('BOOKING','COMPLETION'));
ALTER TABLE booking_item ADD COLUMN standard_time_captured_at TIMESTAMPTZ;
ALTER TABLE booking DROP CONSTRAINT booking_work_time_source_check;
ALTER TABLE booking ADD CONSTRAINT booking_work_time_source_check CHECK(work_time_source IN ('UNKNOWN','MANUAL','TIMER','PHOTO','STANDARD'));
