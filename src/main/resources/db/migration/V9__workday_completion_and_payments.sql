ALTER TABLE booking ADD COLUMN final_total NUMERIC(14,2) CHECK(final_total>=0);
ALTER TABLE booking ADD COLUMN work_minutes INTEGER CHECK(work_minutes BETWEEN 0 AND 10080);
ALTER TABLE booking ADD COLUMN timer_started TIMESTAMPTZ;
ALTER TABLE booking ADD COLUMN work_note VARCHAR(2000) NOT NULL DEFAULT '';
CREATE UNIQUE INDEX one_active_booking_timer ON booking((TRUE)) WHERE timer_started IS NOT NULL;
ALTER TABLE booking_item ADD COLUMN final_amount NUMERIC(14,2) CHECK(final_amount>=0);
ALTER TABLE booking_item ADD COLUMN work_minutes INTEGER CHECK(work_minutes BETWEEN 0 AND 10080);
CREATE TABLE work_write_guard(id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO work_write_guard VALUES(1);
CREATE TABLE booking_audit(
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 booking_id BIGINT NOT NULL REFERENCES booking(id),
 before_data JSONB NOT NULL,after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE booking_payment(
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 booking_id BIGINT NOT NULL REFERENCES booking(id),
 request_token UUID NOT NULL UNIQUE,
 paid_on DATE NOT NULL CHECK(paid_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 amount NUMERIC(14,2) NOT NULL CHECK(amount<>0),
 method VARCHAR(12) NOT NULL CHECK(method IN ('CASH','TRANSFER','CARD')),
 reverses BIGINT UNIQUE REFERENCES booking_payment(id),
 note VARCHAR(500) NOT NULL DEFAULT '',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((reverses IS NULL AND amount>0) OR (reverses IS NOT NULL AND amount<0))
);
CREATE INDEX booking_payment_booking_idx ON booking_payment(booking_id,paid_on);
-- No historic booking is marked paid; no forecast is converted into actual usage.
