-- Keep every historical bill and reading. New counters are cumulative, not
-- the old per-day generator_minutes column.
ALTER TABLE fuel_purchase ADD COLUMN generator_meter NUMERIC(12,2)
    CHECK (generator_meter >= 0);
ALTER TABLE fuel_purchase ADD COLUMN metered_full BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE fuel_purchase DROP CONSTRAINT fuel_purchase_check;
ALTER TABLE fuel_purchase ADD CONSTRAINT fuel_purchase_full_reading_check CHECK (
    NOT full_tank OR (target='CAR' AND odometer IS NOT NULL)
    OR (target='GENERATOR' AND generator_meter IS NOT NULL)
);
ALTER TABLE fuel_purchase ADD CONSTRAINT fuel_purchase_metered_check CHECK (
    NOT metered_full OR (full_tank AND records_complete AND litres IS NOT NULL
      AND ((target='CAR' AND fuel_type='DIESEL' AND odometer IS NOT NULL)
        OR (target='GENERATOR' AND fuel_type='PETROL' AND generator_meter IS NOT NULL)))
);
CREATE TABLE fuel_baseline (
    target VARCHAR(12) PRIMARY KEY CHECK (target IN ('CAR','GENERATOR')),
    started_at TIMESTAMP NOT NULL CHECK (started_at >= TIMESTAMP '2000-01-01' AND started_at < TIMESTAMP '2101-01-01'),
    reading NUMERIC(12,2) NOT NULL CHECK (reading >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    request_token UUID NOT NULL UNIQUE,
    updated_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE fuel_audit DROP CONSTRAINT fuel_audit_kind_check;
ALTER TABLE fuel_audit ADD CONSTRAINT fuel_audit_kind_check CHECK (kind IN ('DAY','PURCHASE','BASELINE'));
