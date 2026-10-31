-- Keep every V1 row; only widen its key to permit more areas on the same date.
ALTER TABLE day_district DROP CONSTRAINT day_district_pkey;
ALTER TABLE day_district ADD CONSTRAINT day_district_pkey PRIMARY KEY (day, district_id);

CREATE INDEX day_district_district_day_idx ON day_district(district_id, day);
