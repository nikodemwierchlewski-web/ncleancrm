CREATE TABLE day_area_override (
 day DATE PRIMARY KEY CHECK(day BETWEEN DATE '2000-01-01' AND DATE '2100-12-31')
);
-- Existing assignments are explicit day exceptions, including inactive history.
INSERT INTO day_area_override(day) SELECT DISTINCT day FROM day_district;
CREATE TABLE weekly_area_plan (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 valid_from DATE NOT NULL UNIQUE CHECK(valid_from BETWEEN DATE '2000-01-01' AND DATE '2100-12-31')
);
CREATE TABLE weekly_area (
 plan_id BIGINT NOT NULL REFERENCES weekly_area_plan(id),
 weekday SMALLINT NOT NULL CHECK(weekday BETWEEN 1 AND 7),
 district_id BIGINT NOT NULL REFERENCES district(id),
 PRIMARY KEY(plan_id,weekday,district_id)
);
