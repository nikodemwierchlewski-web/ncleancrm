CREATE TABLE crm_customer (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 name VARCHAR(120) NOT NULL CHECK(length(trim(name))>0),
 phone VARCHAR(20) NOT NULL,
 address VARCHAR(240) NOT NULL DEFAULT '',
 notes VARCHAR(2000) NOT NULL DEFAULT '',
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX customer_phone_idx ON crm_customer(phone);
CREATE TABLE service_category (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name VARCHAR(100) NOT NULL CHECK(length(trim(name))>0)
);
CREATE TABLE service_definition (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 category_id BIGINT NOT NULL REFERENCES service_category(id),
 name VARCHAR(120) NOT NULL CHECK(length(trim(name))>0),
 unit VARCHAR(20) NOT NULL CHECK(length(trim(unit))>0),
 price NUMERIC(12,2) NOT NULL CHECK(price>=0),
 minutes INTEGER NOT NULL CHECK(minutes BETWEEN 1 AND 1440),
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE booking (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 request_token UUID NOT NULL UNIQUE,
 customer_id BIGINT NOT NULL REFERENCES crm_customer(id),
 day DATE NOT NULL CHECK(day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
 starts TIME NOT NULL,
 customer_name VARCHAR(120) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 address VARCHAR(240) NOT NULL CHECK(length(trim(address))>0),
 status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' CHECK(status IN ('PLANNED','DONE','CANCELLED')),
 total NUMERIC(14,2) NOT NULL CHECK(total>=0)
);
CREATE INDEX booking_day_idx ON booking(day,starts);
CREATE INDEX booking_customer_idx ON booking(customer_id,day);
CREATE TABLE booking_item (
 booking_id BIGINT NOT NULL REFERENCES booking(id),
 position INTEGER NOT NULL,
 service_id BIGINT NOT NULL REFERENCES service_definition(id),
 name VARCHAR(120) NOT NULL,
 unit VARCHAR(20) NOT NULL,
 quantity NUMERIC(10,2) NOT NULL CHECK(quantity>0),
 price NUMERIC(12,2) NOT NULL CHECK(price>=0),
 amount NUMERIC(14,2) NOT NULL CHECK(amount>=0),
 PRIMARY KEY(booking_id,position)
);

-- One-time example catalogue; later UI edits are never overwritten at startup.
INSERT INTO service_category(name) VALUES ('Tapicerka meblowa');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Krzesło tapicerowane','szt.',30.0,15 FROM service_category WHERE name='Tapicerka meblowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Fotel','szt.',90.0,40 FROM service_category WHERE name='Tapicerka meblowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Kanapa / sofa','szt.',180.0,90 FROM service_category WHERE name='Tapicerka meblowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Narożnik','szt.',250.0,120 FROM service_category WHERE name='Tapicerka meblowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Materac jednostronnie','szt.',120.0,60 FROM service_category WHERE name='Tapicerka meblowa';
INSERT INTO service_category(name) VALUES ('Tapicerka samochodowa');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Fotel samochodowy','szt.',60.0,30 FROM service_category WHERE name='Tapicerka samochodowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Komplet siedzeń','kpl.',250.0,120 FROM service_category WHERE name='Tapicerka samochodowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Całe wnętrze samochodu','szt.',400.0,180 FROM service_category WHERE name='Tapicerka samochodowa';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Bagażnik','szt.',80.0,40 FROM service_category WHERE name='Tapicerka samochodowa';
INSERT INTO service_category(name) VALUES ('Dywany i wykładziny');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Pranie dywanu','m²',20.0,5 FROM service_category WHERE name='Dywany i wykładziny';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Pranie wykładziny','m²',15.0,4 FROM service_category WHERE name='Dywany i wykładziny';
INSERT INTO service_category(name) VALUES ('Polerowanie reflektorów');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Polerowanie jednego reflektora','szt.',80.0,45 FROM service_category WHERE name='Polerowanie reflektorów';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Polerowanie pary reflektorów','kpl.',150.0,90 FROM service_category WHERE name='Polerowanie reflektorów';
INSERT INTO service_category(name) VALUES ('Sprzątanie domów i biur');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Sprzątanie mieszkania / domu','m²',12.0,5 FROM service_category WHERE name='Sprzątanie domów i biur';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Sprzątanie biura','m²',10.0,4 FROM service_category WHERE name='Sprzątanie domów i biur';
INSERT INTO service_category(name) VALUES ('Ozonowanie');
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Ozonowanie samochodu','szt.',60.0,60 FROM service_category WHERE name='Ozonowanie';
INSERT INTO service_definition(category_id,name,unit,price,minutes) SELECT id,'Ozonowanie pomieszczenia','szt.',100.0,90 FROM service_category WHERE name='Ozonowanie';
