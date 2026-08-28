ALTER TABLE inventory_transactions
    ADD COLUMN recipient_user_id UUID,
    ADD COLUMN recipient_display_name VARCHAR(150);

-- New ISSUE transactions snapshot the recipient directly in application code.
-- Legacy ISSUE rows are backfilled only when exactly one strong candidate exists.
-- Ambiguous historical rows intentionally remain NULL instead of guessing a technician.
WITH recipient_candidates AS (
    SELECT
        tx.id AS transaction_id,
        request.received_by_user_id,
        request.received_by_display_name,
        COUNT(*) OVER (PARTITION BY tx.id) AS candidate_count
    FROM inventory_transactions tx
    JOIN work_order_part_requests request
      ON request.tenant_id = tx.tenant_id
     AND request.work_order_id = tx.work_order_id
     AND request.spare_part_id = tx.spare_part_id
     AND request.status = 'ISSUED'
     AND request.issued_quantity = tx.quantity
     AND lower(coalesce(request.issued_by_username, '')) = lower(tx.created_by)
     AND request.issued_at BETWEEN tx.created_at - INTERVAL '2 minutes'
                               AND tx.created_at + INTERVAL '2 minutes'
    WHERE tx.transaction_type = 'ISSUE'
      AND request.received_by_user_id IS NOT NULL
      AND request.received_by_display_name IS NOT NULL
)
UPDATE inventory_transactions tx
SET recipient_user_id = candidate.received_by_user_id,
    recipient_display_name = candidate.received_by_display_name
FROM recipient_candidates candidate
WHERE tx.id = candidate.transaction_id
  AND candidate.candidate_count = 1;