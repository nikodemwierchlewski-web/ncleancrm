CREATE TABLE equipment_item (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(160) NOT NULL CHECK(length(trim(name))>0),
 category VARCHAR(100) NOT NULL DEFAULT 'Sprzęt',
 model VARCHAR(160) NOT NULL DEFAULT '',
 serial_number VARCHAR(160) NOT NULL DEFAULT '',
 warranty_until DATE CHECK(warranty_until BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 supplier VARCHAR(160) NOT NULL DEFAULT '',
 note VARCHAR(2000) NOT NULL DEFAULT '',
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE cost_entry (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 version BIGINT NOT NULL DEFAULT 0,
 spent_on DATE NOT NULL CHECK(spent_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 category VARCHAR(24) NOT NULL CHECK(category IN ('CAR_REPAIR','CAR_SERVICE','CAR_PARTS','CAR_OTHER','EQUIPMENT_PURCHASE','EQUIPMENT_SERVICE','MATERIAL','CHEMISTRY','MARKETING','OTHER')),
 title VARCHAR(160) NOT NULL CHECK(length(trim(title))>0),
 amount NUMERIC(14,2) NOT NULL CHECK(amount>=0),
 effect VARCHAR(16) NOT NULL CHECK(effect IN ('OPERATING','INVESTMENT','COVERED','PURCHASE_ONLY')),
 quantity NUMERIC(14,3) CHECK(quantity>0),
 supplier VARCHAR(160) NOT NULL DEFAULT '',
 note VARCHAR(2000) NOT NULL DEFAULT '',
 odometer NUMERIC(10,1) CHECK(odometer>=0),
 asset_id BIGINT REFERENCES equipment_item(id),
 chemical_id BIGINT REFERENCES chemical(id),
 chemical_name VARCHAR(120),
 covered_month DATE REFERENCES dashboard_month(month),
 covered_category VARCHAR(20),
 receipt_id UUID UNIQUE REFERENCES fuel_receipt(id),
 cancelled BOOLEAN NOT NULL DEFAULT FALSE,
 cancel_reason VARCHAR(300) NOT NULL DEFAULT '',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((effect='COVERED')=(covered_month IS NOT NULL)),
 CHECK((effect='COVERED')=(covered_category IS NOT NULL)),
 CHECK((category='EQUIPMENT_PURCHASE')=(effect='INVESTMENT')),
 CHECK((category='CHEMISTRY')=(effect='PURCHASE_ONLY'))
);
CREATE INDEX cost_entry_day ON cost_entry(spent_on,id);
CREATE INDEX cost_entry_asset ON cost_entry(asset_id,spent_on);
CREATE INDEX cost_entry_monthly ON cost_entry(covered_month,covered_category) WHERE NOT cancelled;
CREATE TABLE cost_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 entry_id BIGINT REFERENCES cost_entry(id),
 asset_id BIGINT REFERENCES equipment_item(id),
 before_data JSONB,
 after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE fuel_purchase ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE fuel_purchase ADD COLUMN cancel_reason VARCHAR(300) NOT NULL DEFAULT '';
