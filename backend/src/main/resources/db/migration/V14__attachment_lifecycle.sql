ALTER TABLE attachments
    ADD COLUMN purpose VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN locked_at TIMESTAMPTZ;

UPDATE attachments
SET purpose = 'WORK_EVIDENCE'
WHERE reference_type = 'WORK_ORDER';

UPDATE attachments a
SET purpose = 'PAYMENT_EVIDENCE',
    locked_at = COALESCE(a.locked_at, p.transfer_reported_at, p.updated_at)
FROM payments p
WHERE p.transfer_evidence_attachment_id = a.id;

UPDATE attachments a
SET locked_at = COALESCE(a.locked_at, w.updated_at)
FROM work_orders w
WHERE a.reference_type = 'WORK_ORDER'
  AND a.reference_id = w.id
  AND a.purpose = 'WORK_EVIDENCE'
  AND w.status IN ('CUSTOMER_ACCEPTED', 'CLOSED', 'CANCELLED');

ALTER TABLE attachments
    ADD CONSTRAINT ck_attachments_purpose
        CHECK (purpose IN ('GENERAL', 'WORK_EVIDENCE', 'PAYMENT_EVIDENCE'));

CREATE INDEX idx_attachments_reference_purpose
    ON attachments (tenant_id, reference_type, reference_id, purpose, created_at DESC);
