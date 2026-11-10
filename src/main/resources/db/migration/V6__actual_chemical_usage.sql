ALTER TABLE booking_item ADD COLUMN actual_defined BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE booking_item ADD COLUMN actual_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE booking_actual_chemical (
 booking_id BIGINT NOT NULL,
 item_position INTEGER NOT NULL,
 chemical_id BIGINT NOT NULL REFERENCES chemical(id),
 name VARCHAR(120) NOT NULL,
 unit VARCHAR(4) NOT NULL CHECK(unit IN ('ml','g','szt')),
 quantity NUMERIC(14,3) NOT NULL CHECK(quantity>=0),
 recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(booking_id,item_position,chemical_id),
 FOREIGN KEY(booking_id,item_position) REFERENCES booking_item(booking_id,position)
);
CREATE INDEX actual_usage_chemical_idx ON booking_actual_chemical(chemical_id);
-- Forecasts are deliberately not copied into actual usage.
