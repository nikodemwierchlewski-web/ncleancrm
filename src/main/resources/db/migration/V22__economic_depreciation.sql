-- Economic information only: never a cost_entry or a payment.
-- Revisions and their scheduled monthly amounts are append-only.
CREATE TABLE equipment_depreciation_revision (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 asset_id BIGINT NOT NULL REFERENCES equipment_item(id),
 version BIGINT NOT NULL CHECK(version>=0),
 request_token UUID NOT NULL UNIQUE,
 payload JSONB NOT NULL,
 effective_month DATE NOT NULL CHECK(effective_month BETWEEN DATE '2000-01-01' AND DATE '2100-12-01' AND EXTRACT(DAY FROM effective_month)=1),
 start_month DATE NOT NULL CHECK(start_month BETWEEN DATE '2000-01-01' AND DATE '2100-12-01' AND EXTRACT(DAY FROM start_month)=1),
 initial_value NUMERIC(12,2) NOT NULL CHECK(initial_value>=0),
 residual_value NUMERIC(12,2) NOT NULL CHECK(residual_value>=0 AND residual_value<=initial_value),
 months INTEGER NOT NULL CHECK(months BETWEEN 1 AND 1200),
 enabled BOOLEAN NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(asset_id,version),
 CHECK(effective_month>=start_month)
);
CREATE INDEX equipment_depreciation_effective ON equipment_depreciation_revision(asset_id,effective_month,version);
CREATE TABLE equipment_depreciation_charge (
 revision_id BIGINT NOT NULL REFERENCES equipment_depreciation_revision(id),
 month DATE NOT NULL CHECK(month BETWEEN DATE '2000-01-01' AND DATE '2100-12-01' AND EXTRACT(DAY FROM month)=1),
 amount NUMERIC(12,2) NOT NULL CHECK(amount>=0),
 PRIMARY KEY(revision_id,month)
);
CREATE INDEX equipment_depreciation_month ON equipment_depreciation_charge(month);
