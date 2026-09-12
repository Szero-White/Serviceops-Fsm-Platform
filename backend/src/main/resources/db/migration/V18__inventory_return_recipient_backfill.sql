-- Backfill legacy RETURN rows after recipient snapshot support was introduced.
-- Safety rule: only fill a RETURN row when every prior ISSUE for the same
-- work order + spare part resolves to exactly one technician. Ambiguous
-- historical data intentionally remains NULL instead of guessing.
WITH unique_issue_recipients AS (
    SELECT
        return_tx.id AS return_transaction_id,
        issue_tx.recipient_user_id,
        MAX(issue_tx.recipient_display_name) AS recipient_display_name
    FROM inventory_transactions return_tx
    JOIN inventory_transactions issue_tx
      ON issue_tx.tenant_id = return_tx.tenant_id
     AND issue_tx.work_order_id = return_tx.work_order_id
     AND issue_tx.spare_part_id = return_tx.spare_part_id
     AND issue_tx.transaction_type = 'ISSUE'
     AND issue_tx.created_at <= return_tx.created_at
     AND issue_tx.recipient_user_id IS NOT NULL
     AND issue_tx.recipient_display_name IS NOT NULL
    WHERE return_tx.transaction_type = 'RETURN'
      AND return_tx.recipient_user_id IS NULL
      AND return_tx.work_order_id IS NOT NULL
    GROUP BY return_tx.id, issue_tx.recipient_user_id
),
candidate_counts AS (
    SELECT return_transaction_id, COUNT(*) AS candidate_count
    FROM unique_issue_recipients
    GROUP BY return_transaction_id
)
UPDATE inventory_transactions return_tx
SET recipient_user_id = candidate.recipient_user_id,
    recipient_display_name = candidate.recipient_display_name
FROM unique_issue_recipients candidate
JOIN candidate_counts counts
  ON counts.return_transaction_id = candidate.return_transaction_id
WHERE return_tx.id = candidate.return_transaction_id
  AND counts.candidate_count = 1
