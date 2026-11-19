-- Additive extension of the existing asset book; no historical bills are rewritten.
ALTER TABLE equipment_item ADD COLUMN kind VARCHAR(12) NOT NULL DEFAULT 'EQUIPMENT'
 CHECK(kind IN ('EQUIPMENT','CAR','GENERATOR'));
ALTER TABLE equipment_item ADD COLUMN purchased_on DATE CHECK(purchased_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31');
ALTER TABLE equipment_item ADD COLUMN started_on DATE CHECK(started_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31');
ALTER TABLE equipment_item ADD COLUMN history_boundary DATE CHECK(history_boundary BETWEEN DATE '2000-01-01' AND DATE '2100-12-31');
ALTER TABLE equipment_item ADD COLUMN history_complete BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE equipment_counter (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 asset_id BIGINT NOT NULL REFERENCES equipment_item(id),
 request_token UUID NOT NULL UNIQUE,
 version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(160) NOT NULL CHECK(length(trim(name))>0),
 unit VARCHAR(2) NOT NULL CHECK(unit IN ('h','km')),
 fuel_target VARCHAR(12) UNIQUE CHECK(fuel_target IN ('CAR','GENERATOR')),
 CHECK(fuel_target IS NULL OR (fuel_target='CAR' AND unit='km') OR (fuel_target='GENERATOR' AND unit='h'))
);
CREATE TABLE equipment_reading (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 counter_id BIGINT NOT NULL REFERENCES equipment_counter(id),
 request_token UUID NOT NULL UNIQUE,
 read_on DATE NOT NULL CHECK(read_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 value NUMERIC(12,2) NOT NULL CHECK(value>=0),
 kind VARCHAR(8) NOT NULL CHECK(kind IN ('ACTUAL','ESTIMATE')),
 note VARCHAR(1000) NOT NULL DEFAULT '',
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX equipment_reading_counter ON equipment_reading(counter_id,read_on,id);
CREATE TABLE equipment_maintenance_plan (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 asset_id BIGINT NOT NULL REFERENCES equipment_item(id),
 counter_id BIGINT REFERENCES equipment_counter(id),
 request_token UUID NOT NULL UNIQUE,
 version BIGINT NOT NULL DEFAULT 0,
 title VARCHAR(160) NOT NULL CHECK(length(trim(title))>0),
 due_on DATE CHECK(due_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 due_value NUMERIC(12,2) CHECK(due_value>=0),
 interval_days INTEGER CHECK(interval_days BETWEEN 1 AND 36500),
 interval_value NUMERIC(12,2) CHECK(interval_value>0),
 warning_days INTEGER NOT NULL CHECK(warning_days BETWEEN 0 AND 36500),
 warning_value NUMERIC(12,2) NOT NULL CHECK(warning_value>=0),
 snoozed_until DATE CHECK(snoozed_until BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 CHECK(due_on IS NOT NULL OR due_value IS NOT NULL),
 CHECK(due_value IS NULL OR counter_id IS NOT NULL),
 CHECK(interval_value IS NULL OR due_value IS NOT NULL),
 CHECK(interval_days IS NULL OR due_on IS NOT NULL)
);
CREATE INDEX equipment_maintenance_asset ON equipment_maintenance_plan(asset_id);
CREATE TABLE equipment_maintenance_execution (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 plan_id BIGINT NOT NULL REFERENCES equipment_maintenance_plan(id),
 request_token UUID NOT NULL UNIQUE,
 completed_on DATE NOT NULL CHECK(completed_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 reading NUMERIC(12,2) CHECK(reading>=0),
 cost_entry_id BIGINT REFERENCES cost_entry(id),
 note VARCHAR(1000) NOT NULL DEFAULT '',
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE equipment_change (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 operation VARCHAR(20) NOT NULL,
 result_id BIGINT NOT NULL,
 asset_id BIGINT NOT NULL REFERENCES equipment_item(id),
 detail JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
