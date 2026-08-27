ALTER TABLE work_orders
    ADD COLUMN labor_fee NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN incidental_fee NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN incidental_reason VARCHAR(500);

CREATE TABLE work_order_billing_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL REFERENCES work_orders(id),
    parts_total NUMERIC(18,2) NOT NULL,
    labor_fee NUMERIC(18,2) NOT NULL,
    incidental_fee NUMERIC(18,2) NOT NULL,
    incidental_reason VARCHAR(500),
    total_amount NUMERIC(18,2) NOT NULL,
    accepted_by_user_id UUID NOT NULL REFERENCES user_accounts(id),
    accepted_by_username VARCHAR(100) NOT NULL,
    accepted_by_display_name VARCHAR(150) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_order_billing_snapshot UNIQUE (tenant_id, work_order_id),
    CONSTRAINT ck_billing_snapshot_amounts CHECK (
        parts_total >= 0 AND labor_fee >= 0 AND incidental_fee >= 0 AND total_amount >= 0
    )
);

CREATE TABLE work_order_billing_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    billing_snapshot_id UUID NOT NULL REFERENCES work_order_billing_snapshots(id) ON DELETE CASCADE,
    spare_part_id UUID NOT NULL REFERENCES spare_parts(id),
    spare_part_sku VARCHAR(60) NOT NULL,
    spare_part_name VARCHAR(180) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    quantity NUMERIC(18,3) NOT NULL,
    unit_price NUMERIC(18,2) NOT NULL,
    line_total NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_billing_item_values CHECK (quantity > 0 AND unit_price >= 0 AND line_total >= 0)
);

CREATE INDEX idx_billing_items_snapshot
    ON work_order_billing_items(tenant_id, billing_snapshot_id);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL REFERENCES work_orders(id),
    billing_snapshot_id UUID NOT NULL REFERENCES work_order_billing_snapshots(id),
    amount NUMERIC(18,2) NOT NULL,
    method VARCHAR(30),
    status VARCHAR(40) NOT NULL,
    transfer_evidence_attachment_id UUID REFERENCES attachments(id),
    transfer_reported_at TIMESTAMPTZ,
    cash_collected_at TIMESTAMPTZ,
    collected_by_user_id UUID REFERENCES user_accounts(id),
    collected_by_username VARCHAR(100),
    collected_by_display_name VARCHAR(150),
    settled_at TIMESTAMPTZ,
    settled_by_user_id UUID REFERENCES user_accounts(id),
    settled_by_username VARCHAR(100),
    settled_by_display_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_work_order UNIQUE (tenant_id, work_order_id),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0),
    CONSTRAINT ck_payment_method CHECK (method IS NULL OR method IN ('BANK_TRANSFER','CASH')),
    CONSTRAINT ck_payment_status CHECK (status IN ('UNPAID','TRANSFER_PENDING_VERIFICATION','CASH_PENDING_HANDOVER','SETTLED')),
    CONSTRAINT ck_payment_state_consistency CHECK (
        (status = 'UNPAID' AND method IS NULL AND settled_at IS NULL)
        OR (status = 'TRANSFER_PENDING_VERIFICATION' AND method = 'BANK_TRANSFER' AND transfer_reported_at IS NOT NULL AND settled_at IS NULL)
        OR (status = 'CASH_PENDING_HANDOVER' AND method = 'CASH' AND cash_collected_at IS NOT NULL
            AND collected_by_user_id IS NOT NULL AND settled_at IS NULL)
        OR (status = 'SETTLED' AND method IS NOT NULL AND settled_at IS NOT NULL AND settled_by_user_id IS NOT NULL)
    )
);

CREATE INDEX idx_payment_queue
    ON payments(tenant_id, status, updated_at DESC);

CREATE TABLE company_payment_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    bank_name VARCHAR(150) NOT NULL,
    account_holder VARCHAR(180) NOT NULL,
    account_number VARCHAR(80) NOT NULL,
    qr_attachment_id UUID REFERENCES attachments(id),
    updated_by_user_id UUID NOT NULL REFERENCES user_accounts(id),
    updated_by_username VARCHAR(100) NOT NULL,
    updated_by_display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_company_payment_profile_tenant UNIQUE (tenant_id)
);
