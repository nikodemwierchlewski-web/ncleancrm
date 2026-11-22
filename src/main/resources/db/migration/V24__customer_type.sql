-- Existing customers and visits are not guessed to be private or business.
ALTER TABLE crm_customer ADD COLUMN customer_type VARCHAR(8) NOT NULL DEFAULT 'UNKNOWN'
 CHECK(customer_type IN ('UNKNOWN','PRIVATE','BUSINESS'));
-- Preserve the type selected for each visit even if the profile changes later.
ALTER TABLE booking ADD COLUMN customer_type VARCHAR(8) NOT NULL DEFAULT 'UNKNOWN'
 CHECK(customer_type IN ('UNKNOWN','PRIVATE','BUSINESS'));
