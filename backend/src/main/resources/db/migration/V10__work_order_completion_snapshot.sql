-- Preserve the diagnosis and resolution that belonged to each completed repair cycle.
-- The work_orders table remains the current/latest summary. Historical completion rows
-- keep immutable snapshots so reopened work can be completed again without erasing prior results.
ALTER TABLE work_order_status_history
    ADD COLUMN diagnosis_snapshot TEXT,
    ADD COLUMN resolution_snapshot TEXT;

-- Older data did not persist per-cycle completion details. Backfill only the most recent
-- completed history row from the current Work Order summary; earlier cycles cannot be
-- reconstructed reliably and are intentionally left NULL rather than fabricating history.
WITH latest_completion AS (
    SELECT DISTINCT ON (work_order_id)
           id,
           work_order_id
    FROM work_order_status_history
    WHERE to_status = 'COMPLETED'
    ORDER BY work_order_id, created_at DESC, id DESC
)
UPDATE work_order_status_history h
SET diagnosis_snapshot = w.diagnosis,
    resolution_snapshot = w.resolution
FROM latest_completion lc
JOIN work_orders w ON w.id = lc.work_order_id
WHERE h.id = lc.id
  AND h.tenant_id = w.tenant_id;
