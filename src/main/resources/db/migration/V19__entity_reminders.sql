-- Additive: legacy schedules and their occurrence history remain untouched.
ALTER TABLE memo_entry ADD COLUMN due_time TIME;
CREATE TABLE reminder_owner (
 source_key VARCHAR(40) PRIMARY KEY,
 booking_id BIGINT UNIQUE REFERENCES booking(id),
 memo_id BIGINT UNIQUE REFERENCES memo_entry(id),
 version BIGINT NOT NULL DEFAULT 0,
 CHECK ((booking_id IS NOT NULL)::int + (memo_id IS NOT NULL)::int = 1)
);
CREATE TABLE entity_reminder (
 id UUID PRIMARY KEY,
 source_key VARCHAR(40) NOT NULL REFERENCES reminder_owner(source_key),
 offset_minutes BIGINT CHECK(offset_minutes BETWEEN 0 AND 5256000),
 absolute_at TIMESTAMPTZ,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 CHECK ((offset_minutes IS NOT NULL)::int + (absolute_at IS NOT NULL)::int = 1)
);
CREATE INDEX entity_reminder_source ON entity_reminder(source_key) WHERE enabled;
CREATE TABLE notification_occurrence (
 id UUID PRIMARY KEY,
 reminder_id UUID NOT NULL REFERENCES entity_reminder(id),
 scheduled_at TIMESTAMPTZ NOT NULL,
 state VARCHAR(8) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','READ','SNOOZED')),
 valid BOOLEAN NOT NULL DEFAULT TRUE,
 snoozed_until TIMESTAMPTZ,
 displayed_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(reminder_id,scheduled_at),
 CHECK ((state='SNOOZED')=(snoozed_until IS NOT NULL))
);
CREATE INDEX notification_available ON notification_occurrence(scheduled_at) WHERE valid AND state<>'READ';
