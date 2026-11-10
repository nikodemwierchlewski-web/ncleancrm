CREATE TABLE chemical (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(120) NOT NULL CHECK(length(trim(name))>0),
 package_unit VARCHAR(4) NOT NULL CHECK(package_unit IN ('ml','l','g','kg','szt')),
 package_quantity NUMERIC(14,3) NOT NULL CHECK(package_quantity>0),
 package_price NUMERIC(12,2) NOT NULL CHECK(package_price>=0)
);
ALTER TABLE service_definition ADD COLUMN chemistry_defined BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE service_chemical (
 service_id BIGINT NOT NULL REFERENCES service_definition(id),
 chemical_id BIGINT NOT NULL REFERENCES chemical(id),
 quantity NUMERIC(14,3) NOT NULL CHECK(quantity>=0),
 PRIMARY KEY(service_id,chemical_id)
);
ALTER TABLE booking ADD COLUMN chemical_cost NUMERIC(14,2) CHECK(chemical_cost>=0);
ALTER TABLE booking_item ADD COLUMN suggested_price NUMERIC(12,2);
-- Before V5 the agreed price was always the catalogue price at booking time.
UPDATE booking_item SET suggested_price=price;
ALTER TABLE booking_item ALTER COLUMN suggested_price SET NOT NULL;
ALTER TABLE booking_item ADD COLUMN forecast_defined BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE booking_item ADD COLUMN chemical_cost NUMERIC(14,2) CHECK(chemical_cost>=0);
CREATE TABLE booking_chemical (
 booking_id BIGINT NOT NULL,
 item_position INTEGER NOT NULL,
 chemical_id BIGINT NOT NULL REFERENCES chemical(id),
 name VARCHAR(120) NOT NULL,
 unit VARCHAR(4) NOT NULL CHECK(unit IN ('ml','g','szt')),
 quantity NUMERIC(14,3) NOT NULL CHECK(quantity>=0),
 unit_cost NUMERIC(30,16) NOT NULL CHECK(unit_cost>=0),
 amount NUMERIC(14,2) NOT NULL CHECK(amount>=0),
 PRIMARY KEY(booking_id,item_position,chemical_id),
 FOREIGN KEY(booking_id,item_position) REFERENCES booking_item(booking_id,position)
);
