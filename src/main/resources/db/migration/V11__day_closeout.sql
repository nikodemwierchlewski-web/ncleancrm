-- Existing work_note is kept unchanged: historical notes may mix booking/work context.
ALTER TABLE booking ADD COLUMN review_reason VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE booking ADD COLUMN completion_note VARCHAR(2000) NOT NULL DEFAULT '';
ALTER TABLE booking ADD COLUMN completion_no_notes BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE booking ADD COLUMN photo_before_exception VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE booking ADD COLUMN photo_after_exception VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE booking_item ADD COLUMN actual_no_chem BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE day_review (
 day DATE PRIMARY KEY CHECK(day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 fingerprint VARCHAR(32) NOT NULL,
 closed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 closed_by VARCHAR(120) NOT NULL
);
CREATE TABLE booking_move (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 booking_id BIGINT NOT NULL REFERENCES booking(id),
 from_day DATE NOT NULL,
 to_day DATE NOT NULL,
 reason VARCHAR(300) NOT NULL DEFAULT '',
 moved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX booking_move_from_day ON booking_move(from_day);
