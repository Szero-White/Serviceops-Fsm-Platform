CREATE TABLE work_order_part_requests (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    spare_part_id UUID NOT NULL REFERENCES spare_parts(id),
    requested_quantity NUMERIC(18,3) NOT NULL,
    request_note VARCHAR(300) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_by_user_id UUID NOT NULL REFERENCES user_accounts(id),
    requested_by_username VARCHAR(100) NOT NULL,
    requested_by_display_name VARCHAR(150) NOT NULL,
    issued_quantity NUMERIC(18,3),
    issued_by_user_id UUID REFERENCES user_accounts(id),
    issued_by_username VARCHAR(100),
    issued_by_display_name VARCHAR(150),
    issued_at TIMESTAMPTZ,
    received_by_user_id UUID REFERENCES user_accounts(id),
    received_by_display_name VARCHAR(150),
    resolution_reason VARCHAR(500),
    resolved_by_user_id UUID REFERENCES user_accounts(id),
    resolved_by_username VARCHAR(100),
    resolved_by_display_name VARCHAR(150),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_work_order_part_request_quantity CHECK (requested_quantity > 0),
    CONSTRAINT ck_work_order_part_issued_quantity CHECK (issued_quantity IS NULL OR issued_quantity > 0),
    CONSTRAINT ck_work_order_part_request_status CHECK (status IN ('REQUESTED','ISSUED','CANCELLED','UNAVAILABLE','EXPIRED'))
);

CREATE UNIQUE INDEX uk_work_order_part_request_active
    ON work_order_part_requests(tenant_id, work_order_id, spare_part_id)
    WHERE status = 'REQUESTED';

CREATE INDEX idx_work_order_part_request_queue
    ON work_order_part_requests(tenant_id, status, created_at ASC);

CREATE INDEX idx_work_order_part_request_work_order
    ON work_order_part_requests(tenant_id, work_order_id, created_at ASC);

CREATE TABLE work_order_part_usage (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    spare_part_id UUID NOT NULL REFERENCES spare_parts(id),
    used_quantity NUMERIC(18,3) NOT NULL,
    updated_by_user_id UUID NOT NULL REFERENCES user_accounts(id),
    updated_by_username VARCHAR(100) NOT NULL,
    updated_by_display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_order_part_usage UNIQUE (tenant_id, work_order_id, spare_part_id),
    CONSTRAINT ck_work_order_part_usage_quantity CHECK (used_quantity >= 0)
);

CREATE INDEX idx_work_order_part_usage_work_order
    ON work_order_part_usage(tenant_id, work_order_id);
