-- Existing dashboard pins keep their meaning; Today is a separate explicit choice.
ALTER TABLE memo_entry ADD COLUMN today_pinned BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX memo_today_pins ON memo_entry(updated_at DESC,id DESC)
 WHERE today_pinned AND NOT completed AND NOT archived;
CREATE TABLE memo_reminder (
 entry_id BIGINT PRIMARY KEY REFERENCES memo_entry(id),
 revision BIGINT NOT NULL DEFAULT 1 CHECK(revision>0),
 frequency VARCHAR(8) NOT NULL CHECK(frequency IN ('NONE','ONCE','DAILY','WEEKDAYS','INTERVAL')),
 starts DATE NOT NULL CHECK(starts BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 at_time TIME NOT NULL,
 weekdays INTEGER NOT NULL DEFAULT 0 CHECK(weekdays BETWEEN 0 AND 127),
 interval_days INTEGER NOT NULL DEFAULT 1 CHECK(interval_days BETWEEN 1 AND 365),
 CHECK(frequency<>'WEEKDAYS' OR weekdays>0)
);
CREATE TABLE memo_reminder_event (
 entry_id BIGINT NOT NULL REFERENCES memo_entry(id),
 revision BIGINT NOT NULL,
 scheduled_date DATE NOT NULL,
 scheduled_at TIMESTAMPTZ NOT NULL,
 state VARCHAR(8) NOT NULL CHECK(state IN ('PENDING','DONE','SNOOZED')),
 snoozed_until TIMESTAMPTZ,
 seen_at TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(entry_id,revision,scheduled_date),
 CHECK((state='SNOOZED')=(snoozed_until IS NOT NULL))
);
