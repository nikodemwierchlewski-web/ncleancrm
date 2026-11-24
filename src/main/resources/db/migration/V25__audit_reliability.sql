-- Rebase only an exact existing signature. Actual edits remain open. V1-V24 stay untouched.
CREATE FUNCTION pg_temp.crm_legacy_day(p_day DATE,p_ordinary BOOLEAN) RETURNS TEXT LANGUAGE SQL STABLE AS $$
SELECT md5(jsonb_build_array(
          (SELECT COALESCE(jsonb_agg(to_jsonb(b)-'removed_at'-'historical'-'customer_type' ORDER BY id),'[]') FROM booking b WHERE (NOT p_ordinary OR NOT b.historical) AND b.removed_at IS NULL AND day=p_day),
          (SELECT COALESCE(jsonb_agg(to_jsonb(i) ORDER BY booking_id,position),'[]') FROM booking_item i WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(a) ORDER BY booking_id,item_position,chemical_id),'[]') FROM booking_actual_chemical a WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg(jsonb_build_object('id',p.id,'booking_id',p.booking_id,'stage',p.stage,'name',p.original_name,'time',p.captured_at) ORDER BY p.id),'[]') FROM booking_photo p WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg(to_jsonb(m) ORDER BY id),'[]') FROM booking_move m WHERE (NOT p_ordinary OR booking_id IN (SELECT id FROM booking WHERE NOT historical)) AND from_day=p_day)
        )::text);
$$;
CREATE FUNCTION pg_temp.crm_stable_day(p_day DATE,p_ordinary BOOLEAN) RETURNS TEXT LANGUAGE SQL STABLE AS $$
SELECT md5(jsonb_build_array(
          (SELECT COALESCE(jsonb_agg((SELECT jsonb_object_agg(k,v) FROM jsonb_each(to_jsonb(b)) e(k,v) WHERE k IN ('id','version','request_token','customer_id','day','starts','customer_name','phone','address','status','total','chemical_cost','final_total','work_minutes','timer_started','work_note','work_time_source','work_started_at','work_finished_at','review_reason','completion_note','completion_no_notes','photo_before_exception','photo_after_exception'))-'removed_at'-'historical'-'customer_type' ORDER BY id),'[]') FROM booking b WHERE (NOT p_ordinary OR NOT b.historical) AND b.removed_at IS NULL AND day=p_day),
          (SELECT COALESCE(jsonb_agg((SELECT jsonb_object_agg(k,v) FROM jsonb_each(to_jsonb(i)) e(k,v) WHERE k IN ('booking_id','position','service_id','name','unit','quantity','price','amount','suggested_price','forecast_defined','chemical_cost','actual_defined','actual_version','final_amount','work_minutes','actual_no_chem')) ORDER BY booking_id,position),'[]') FROM booking_item i WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg((SELECT jsonb_object_agg(k,v) FROM jsonb_each(to_jsonb(a)) e(k,v) WHERE k IN ('booking_id','item_position','chemical_id','name','unit','quantity','unit_cost','amount')) ORDER BY booking_id,item_position,chemical_id),'[]') FROM booking_actual_chemical a WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg(jsonb_build_object('id',p.id,'booking_id',p.booking_id,'stage',p.stage,'name',p.original_name,'time',p.captured_at) ORDER BY p.id),'[]') FROM booking_photo p WHERE booking_id IN (SELECT id FROM booking WHERE (NOT p_ordinary OR NOT historical) AND removed_at IS NULL AND day=p_day)),
          (SELECT COALESCE(jsonb_agg((SELECT jsonb_object_agg(k,v) FROM jsonb_each(to_jsonb(m)) e(k,v) WHERE k IN ('id','booking_id','from_day','to_day','reason','moved_at')) ORDER BY id),'[]') FROM booking_move m WHERE (NOT p_ordinary OR booking_id IN (SELECT id FROM booking WHERE NOT historical)) AND from_day=p_day)
        )::text);
$$;
UPDATE day_review r SET fingerprint=CASE
 WHEN r.fingerprint=pg_temp.crm_legacy_day(r.day,FALSE) THEN pg_temp.crm_stable_day(r.day,FALSE)
 ELSE pg_temp.crm_stable_day(r.day,TRUE) END
 WHERE r.fingerprint=pg_temp.crm_legacy_day(r.day,FALSE) OR r.fingerprint=pg_temp.crm_legacy_day(r.day,TRUE);

-- Existing minutes are preserved exactly. Precision starts with subsequent timer segments.
ALTER TABLE booking ADD COLUMN timer_seconds BIGINT NOT NULL DEFAULT 0 CHECK(timer_seconds BETWEEN 0 AND 604800);
UPDATE booking SET timer_seconds=COALESCE(work_minutes,0)*60 WHERE work_time_source='TIMER';
CREATE TABLE request_replay (
 scope VARCHAR(20) NOT NULL,
 token UUID NOT NULL,
 payload JSONB NOT NULL,
 entity_id BIGINT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(scope,token)
);
CREATE TABLE chemical_usage_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 booking_id BIGINT NOT NULL,
 item_position INTEGER NOT NULL,
 action VARCHAR(12) NOT NULL CHECK(action IN ('MEASURED','ZERO','STANDARD')),
 before_data JSONB NOT NULL,
 after_data JSONB NOT NULL,
 actor VARCHAR(120) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(booking_id,item_position) REFERENCES booking_item(booking_id,position)
);
CREATE INDEX chemical_usage_audit_booking ON chemical_usage_audit(booking_id,id DESC);
