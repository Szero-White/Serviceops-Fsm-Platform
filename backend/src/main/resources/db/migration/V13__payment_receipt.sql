CREATE TABLE payment_receipts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    work_order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    billing_snapshot_id UUID NOT NULL,
    receipt_code VARCHAR(60) NOT NULL,
    work_order_code_snapshot VARCHAR(40) NOT NULL,
    customer_name_snapshot VARCHAR(200) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    settled_by_display_name VARCHAR(150) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    issued_by_user_id UUID NOT NULL,
    issued_by_username VARCHAR(100) NOT NULL,
    issued_by_display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_receipts_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_payment_receipts_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_receipts_billing_snapshot FOREIGN KEY (billing_snapshot_id) REFERENCES work_order_billing_snapshots(id),
    CONSTRAINT uq_payment_receipts_work_order UNIQUE (tenant_id, work_order_id),
    CONSTRAINT uq_payment_receipts_payment UNIQUE (tenant_id, payment_id),
    CONSTRAINT uq_payment_receipts_code UNIQUE (tenant_id, receipt_code)
);

CREATE INDEX idx_payment_receipts_tenant_issued_at
    ON payment_receipts (tenant_id, issued_at DESC);
