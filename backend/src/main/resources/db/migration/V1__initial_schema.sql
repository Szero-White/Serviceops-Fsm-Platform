CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    username VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(180) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    address VARCHAR(300),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_code_tenant UNIQUE (tenant_id, code)
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    category VARCHAR(80) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(120) NOT NULL,
    installed_at DATE,
    warranty_until DATE,
    status VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_asset_serial_tenant UNIQUE (tenant_id, serial_number)
);

CREATE TABLE technician_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL UNIQUE REFERENCES user_accounts(id),
    phone VARCHAR(30),
    skills VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    asset_id UUID REFERENCES assets(id),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE work_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    service_request_id UUID REFERENCES service_requests(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    asset_id UUID REFERENCES assets(id),
    technician_id UUID REFERENCES technician_profiles(id),
    code VARCHAR(40) NOT NULL,
    summary VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    scheduled_start TIMESTAMPTZ,
    scheduled_end TIMESTAMPTZ,
    diagnosis TEXT,
    resolution TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_order_code_tenant UNIQUE (tenant_id, code),
    CONSTRAINT ck_work_order_schedule CHECK (scheduled_end IS NULL OR scheduled_start IS NULL OR scheduled_end > scheduled_start)
);

CREATE SEQUENCE work_order_number_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL UNIQUE REFERENCES work_orders(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES technician_profiles(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_appointment_time CHECK (end_time > start_time)
);

CREATE TABLE work_order_status_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    note TEXT,
    changed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE spare_parts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    sku VARCHAR(60) NOT NULL,
    name VARCHAR(180) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    stock_quantity NUMERIC(18,3) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(18,3) NOT NULL DEFAULT 0,
    unit_price NUMERIC(18,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_spare_part_sku_tenant UNIQUE (tenant_id, sku),
    CONSTRAINT ck_spare_part_stock CHECK (stock_quantity >= 0)
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    spare_part_id UUID NOT NULL REFERENCES spare_parts(id),
    work_order_id UUID REFERENCES work_orders(id),
    transaction_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(18,3) NOT NULL,
    balance_after NUMERIC(18,3) NOT NULL,
    note VARCHAR(300),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inventory_quantity CHECK (quantity > 0)
);

CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    actor_username VARCHAR(100) NOT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    recipient_user_id UUID NOT NULL REFERENCES user_accounts(id),
    title VARCHAR(180) NOT NULL,
    message VARCHAR(500) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_tenant_name ON customers(tenant_id, name);
CREATE INDEX idx_asset_tenant_serial ON assets(tenant_id, serial_number);
CREATE INDEX idx_service_request_tenant_status ON service_requests(tenant_id, status, created_at DESC);
CREATE INDEX idx_work_order_tenant_status ON work_orders(tenant_id, status, created_at DESC);
CREATE INDEX idx_work_order_technician_schedule ON work_orders(technician_id, scheduled_start, scheduled_end);
CREATE INDEX idx_appointment_technician_time ON appointments(technician_id, start_time, end_time);
CREATE INDEX idx_inventory_part_created ON inventory_transactions(spare_part_id, created_at DESC);
CREATE INDEX idx_audit_tenant_created ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX idx_notification_recipient_read ON notifications(recipient_user_id, read_at, created_at DESC);
