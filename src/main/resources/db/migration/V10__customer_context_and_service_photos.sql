ALTER TABLE crm_customer ADD COLUMN source VARCHAR(12) NOT NULL DEFAULT 'UNKNOWN' CHECK(source IN ('UNKNOWN','GOOGLE','FACEBOOK','REFERRAL','OLX','OTHER'));
ALTER TABLE crm_customer ADD COLUMN referral_note VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE crm_customer ADD COLUMN contact_due DATE;
ALTER TABLE crm_customer ADD COLUMN last_contact DATE;
CREATE TABLE customer_contact (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 customer_id BIGINT NOT NULL REFERENCES crm_customer(id),
 contacted_on DATE NOT NULL, next_on DATE NOT NULL,
 note VARCHAR(1000) NOT NULL DEFAULT ''
);
ALTER TABLE booking ADD COLUMN work_time_source VARCHAR(8) NOT NULL DEFAULT 'UNKNOWN' CHECK(work_time_source IN ('UNKNOWN','MANUAL','TIMER','PHOTO'));
UPDATE booking SET work_time_source='MANUAL' WHERE work_minutes IS NOT NULL;
ALTER TABLE booking ADD COLUMN work_started_at TIMESTAMP;
ALTER TABLE booking ADD COLUMN work_finished_at TIMESTAMP;
CREATE TABLE booking_photo (
 id UUID PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 booking_id BIGINT NOT NULL REFERENCES booking(id),
 stage VARCHAR(6) NOT NULL CHECK(stage IN ('BEFORE','AFTER')),
 original_name VARCHAR(180) NOT NULL,
 captured_at TIMESTAMP,
 image BYTEA NOT NULL CHECK(octet_length(image)<=8388608),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX booking_photo_booking_idx ON booking_photo(booking_id,stage,captured_at);
