ALTER TABLE payments
    ADD COLUMN counter_payment_requested_at TIMESTAMPTZ;

ALTER TABLE payments
    DROP CONSTRAINT ck_payment_state_consistency,
    DROP CONSTRAINT ck_payment_status;

ALTER TABLE payments
    ADD CONSTRAINT ck_payment_status CHECK (
        status IN (
            'UNPAID',
            'TRANSFER_PENDING_VERIFICATION',
            'CASH_PENDING_HANDOVER',
            'COUNTER_PAYMENT_PENDING',
            'SETTLED'
        )
    ),
    ADD CONSTRAINT ck_payment_state_consistency CHECK (
        (status = 'UNPAID' AND method IS NULL AND settled_at IS NULL)
        OR (status = 'TRANSFER_PENDING_VERIFICATION' AND method = 'BANK_TRANSFER'
            AND transfer_reported_at IS NOT NULL AND settled_at IS NULL)
        OR (status = 'CASH_PENDING_HANDOVER' AND method = 'CASH'
            AND cash_collected_at IS NOT NULL AND collected_by_user_id IS NOT NULL AND settled_at IS NULL)
        OR (status = 'COUNTER_PAYMENT_PENDING' AND method IS NULL
            AND counter_payment_requested_at IS NOT NULL AND settled_at IS NULL)
        OR (status = 'SETTLED' AND method IS NOT NULL
            AND settled_at IS NOT NULL AND settled_by_user_id IS NOT NULL)
    );
