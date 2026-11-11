CREATE TABLE fuel_write_guard(id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO fuel_write_guard VALUES(1);
CREATE TABLE fuel_parameters (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 request_token UUID NOT NULL UNIQUE,
 effective_day DATE NOT NULL CHECK(effective_day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 car_rate NUMERIC(8,3) NOT NULL CHECK(car_rate>=0),
 generator_rate NUMERIC(8,3) NOT NULL CHECK(generator_rate>=0),
 car_fuel VARCHAR(10) NOT NULL CHECK(car_fuel IN ('PETROL','DIESEL','LPG')),
 generator_fuel VARCHAR(10) NOT NULL CHECK(generator_fuel IN ('PETROL','DIESEL','LPG')),
 car_price NUMERIC(8,3) NOT NULL CHECK(car_price>=0),
 generator_price NUMERIC(8,3) NOT NULL CHECK(generator_price>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX fuel_parameters_day_idx ON fuel_parameters(effective_day,id);
CREATE TABLE fuel_day (
 day DATE PRIMARY KEY CHECK(day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 version BIGINT NOT NULL DEFAULT 0,
 odometer NUMERIC(10,1) NOT NULL CHECK(odometer>=0),
 generator_minutes INTEGER NOT NULL CHECK(generator_minutes BETWEEN 0 AND 1440),
 parameters_id BIGINT NOT NULL REFERENCES fuel_parameters(id),
 car_rate NUMERIC(8,3) NOT NULL,
 generator_rate NUMERIC(8,3) NOT NULL,
 car_price NUMERIC(8,3) NOT NULL,
 generator_price NUMERIC(8,3) NOT NULL,
 car_fuel VARCHAR(10) NOT NULL,
 generator_fuel VARCHAR(10) NOT NULL
);
CREATE TABLE fuel_receipt (
 id UUID PRIMARY KEY,
 image BYTEA NOT NULL CHECK(octet_length(image)<=8388608),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE fuel_purchase (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 request_token UUID NOT NULL UNIQUE,
 purchased_at TIMESTAMP NOT NULL,
 target VARCHAR(12) NOT NULL CHECK(target IN ('CAR','GENERATOR','SHARED')),
 fuel_type VARCHAR(10) NOT NULL CHECK(fuel_type IN ('PETROL','DIESEL','LPG')),
 amount NUMERIC(12,2) NOT NULL CHECK(amount>=0),
 litres NUMERIC(10,3) CHECK(litres>0),
 litre_price NUMERIC(10,3) CHECK(litre_price>0),
 full_tank BOOLEAN NOT NULL DEFAULT FALSE,
 odometer NUMERIC(10,1) CHECK(odometer>=0),
 records_complete BOOLEAN NOT NULL DEFAULT FALSE,
 note VARCHAR(1000) NOT NULL DEFAULT '',
 receipt_id UUID UNIQUE REFERENCES fuel_receipt(id),
 CHECK(NOT full_tank OR (target='CAR' AND odometer IS NOT NULL))
);
CREATE INDEX fuel_purchase_time_idx ON fuel_purchase(purchased_at,id);
CREATE TABLE fuel_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 kind VARCHAR(12) NOT NULL CHECK(kind IN ('DAY','PURCHASE')),
 record_key VARCHAR(40) NOT NULL,
 before_data JSONB,
 after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
