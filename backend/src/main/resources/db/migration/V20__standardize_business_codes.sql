CREATE TABLE business_code_counters (
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code_type VARCHAR(40) NOT NULL,
    business_date DATE NOT NULL,
    last_value BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, code_type, business_date),
    CONSTRAINT ck_business_code_counter_positive CHECK (last_value > 0)
);

-- Capture old -> new mappings first so snapshots and user-facing history can be kept consistent.
CREATE TEMP TABLE tmp_customer_codes ON COMMIT DROP AS
SELECT id,
       tenant_id,
       code AS old_code,
       'KH-' || to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' ||
       lpad(row_number() OVER (
           PARTITION BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
           ORDER BY created_at, id
       )::text, 3, '0') AS new_code
FROM customers;

CREATE TEMP TABLE tmp_work_order_codes ON COMMIT DROP AS
SELECT id,
       tenant_id,
       code AS old_code,
       'WO-' || to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' ||
       lpad(row_number() OVER (
           PARTITION BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
           ORDER BY created_at, id
       )::text, 3, '0') AS new_code
FROM work_orders;

CREATE TEMP TABLE tmp_spare_part_codes ON COMMIT DROP AS
SELECT id,
       tenant_id,
       sku AS old_code,
       'PT-' || to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' ||
       lpad(row_number() OVER (
           PARTITION BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
           ORDER BY created_at, id
       )::text, 3, '0') AS new_code
FROM spare_parts;

CREATE TEMP TABLE tmp_receipt_codes ON COMMIT DROP AS
SELECT id,
       tenant_id,
       receipt_code AS old_code,
       'BN-' || to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' ||
       lpad(row_number() OVER (
           PARTITION BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
           ORDER BY created_at, id
       )::text, 3, '0') AS new_code
FROM payment_receipts;

-- Move through temporary unique values so migration also succeeds if canonical-looking codes already exist.
UPDATE customers SET code = 'TMP-' || id::text;
UPDATE work_orders SET code = 'TMP-' || id::text;
UPDATE spare_parts SET sku = 'TMP-' || id::text;
UPDATE payment_receipts SET receipt_code = 'TMP-' || id::text;

UPDATE customers c SET code = m.new_code FROM tmp_customer_codes m WHERE c.id = m.id;
UPDATE work_orders w SET code = m.new_code FROM tmp_work_order_codes m WHERE w.id = m.id;
UPDATE spare_parts s SET sku = m.new_code FROM tmp_spare_part_codes m WHERE s.id = m.id;
UPDATE payment_receipts r SET receipt_code = m.new_code FROM tmp_receipt_codes m WHERE r.id = m.id;

-- Keep immutable snapshots aligned with their canonical business identifiers.
UPDATE payment_receipts r
SET work_order_code_snapshot = w.code
FROM work_orders w
WHERE r.work_order_id = w.id;

UPDATE work_order_billing_items b
SET spare_part_sku = s.sku
FROM spare_parts s
WHERE b.spare_part_id = s.id;

-- Audit logs and notification text are historical records. Do not rewrite their free-text payloads here:
-- changing past audit content would weaken traceability and arbitrary legacy codes can be unsafe to
-- replace inside prose. Structural snapshots above are updated because they are canonical identifiers;
-- new audit/notification records automatically use the standardized business codes.

-- Start new counters after the highest value already assigned for each tenant/business day.
INSERT INTO business_code_counters (tenant_id, code_type, business_date, last_value)
SELECT tenant_id, 'CUSTOMER', (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, count(*)
FROM customers GROUP BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;

INSERT INTO business_code_counters (tenant_id, code_type, business_date, last_value)
SELECT tenant_id, 'WORK_ORDER', (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, count(*)
FROM work_orders GROUP BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;

INSERT INTO business_code_counters (tenant_id, code_type, business_date, last_value)
SELECT tenant_id, 'SPARE_PART', (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, count(*)
FROM spare_parts GROUP BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;

INSERT INTO business_code_counters (tenant_id, code_type, business_date, last_value)
SELECT tenant_id, 'PAYMENT_RECEIPT', (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, count(*)
FROM payment_receipts GROUP BY tenant_id, (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;

-- The legacy global Work Order sequence is no longer referenced after V20.
DROP SEQUENCE IF EXISTS work_order_number_seq;
