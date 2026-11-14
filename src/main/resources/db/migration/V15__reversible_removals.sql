-- Reversible removal: booking/customer/price snapshots and all foreign keys survive.
ALTER TABLE booking ADD COLUMN removed_at TIMESTAMPTZ;
ALTER TABLE service_category ADD COLUMN removed_at TIMESTAMPTZ;
ALTER TABLE service_category ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE service_definition ADD COLUMN removed_at TIMESTAMPTZ;
ALTER TABLE chemical ADD COLUMN removed_at TIMESTAMPTZ;
CREATE INDEX booking_visible_day_idx ON booking(day,starts) WHERE removed_at IS NULL;
CREATE TABLE removal_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 kind VARCHAR(20) NOT NULL,
 entity_id BIGINT NOT NULL,
 action VARCHAR(10) NOT NULL CHECK(action IN ('REMOVE','RESTORE')),
 before_data JSONB NOT NULL,
 after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX removal_audit_entity_idx ON removal_audit(kind,entity_id,id);
