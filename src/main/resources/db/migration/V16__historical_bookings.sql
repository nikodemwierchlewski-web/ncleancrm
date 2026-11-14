-- Existing bookings keep their identity, snapshots and ordinary workflow.
ALTER TABLE booking ADD COLUMN historical BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE booking ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE booking ADD CONSTRAINT booking_customer_required CHECK (historical OR customer_id IS NOT NULL);
ALTER TABLE booking DROP CONSTRAINT booking_address_check;
ALTER TABLE booking ADD CONSTRAINT booking_address_check CHECK (historical OR length(trim(address))>0);
