-- Keep V1–V6 intact. Old observations without a matching price snapshot remain unknown.
ALTER TABLE booking_actual_chemical ADD COLUMN unit_cost NUMERIC(30,16) CHECK(unit_cost>=0);
ALTER TABLE booking_actual_chemical ADD COLUMN amount NUMERIC(14,2) CHECK(amount>=0);
UPDATE booking_actual_chemical a SET unit_cost=c.unit_cost, amount=round(a.quantity*c.unit_cost,2)
FROM booking_chemical c WHERE c.booking_id=a.booking_id AND c.item_position=a.item_position
AND c.chemical_id=a.chemical_id AND c.unit=a.unit;
CREATE TABLE dashboard_month (
 month DATE PRIMARY KEY CHECK(extract(day FROM month)=1),
 version BIGINT NOT NULL DEFAULT 0,
 configured BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE dashboard_month_cost (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 month DATE NOT NULL REFERENCES dashboard_month(month),
 name VARCHAR(120) NOT NULL CHECK(length(trim(name))>0),
 category VARCHAR(20) NOT NULL CHECK(category IN ('MARKETING','LEASE','INSURANCE','ACCOUNTING','PHONE','SOFTWARE','OTHER')),
 amount NUMERIC(12,2) NOT NULL CHECK(amount>=0)
);
CREATE TABLE dashboard_fuel (
 day DATE PRIMARY KEY CHECK(day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 version BIGINT NOT NULL DEFAULT 0,
 consumption NUMERIC(8,3) NOT NULL CHECK(consumption>=0),
 litre_price NUMERIC(8,3) NOT NULL CHECK(litre_price>=0),
 planned_km NUMERIC(10,3) CHECK(planned_km>=0),
 actual_km NUMERIC(10,3) CHECK(actual_km>=0),
 actual_cost NUMERIC(12,2) CHECK(actual_cost>=0)
);
