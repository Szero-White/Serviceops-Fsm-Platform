CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE service_request_channels (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(240),
    color VARCHAR(30) NOT NULL DEFAULT 'blue',
    sort_order INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_defined BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_service_channel_code_tenant UNIQUE (tenant_id, code)
);

CREATE INDEX idx_service_channel_tenant_active ON service_request_channels(tenant_id, active, sort_order);

INSERT INTO service_request_channels (
    id, tenant_id, code, name, description, color, sort_order, active, system_defined, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    t.id,
    seed.code,
    seed.name,
    seed.description,
    seed.color,
    seed.sort_order,
    TRUE,
    TRUE,
    NOW(),
    NOW(),
    0
FROM tenants t
CROSS JOIN (
    VALUES
        ('PHONE', 'Điện thoại', 'Cuộc gọi hotline hoặc số chăm sóc khách hàng', 'green', 10),
        ('EMAIL', 'Email', 'Yêu cầu gửi qua hộp thư hỗ trợ', 'blue', 20),
        ('WEBSITE', 'Website', 'Biểu mẫu tiếp nhận trên website hoặc portal', 'geekblue', 30),
        ('ZALO', 'Zalo', 'Tin nhắn từ Zalo OA hoặc nhân viên CSKH', 'cyan', 40),
        ('WALK_IN', 'Trực tiếp', 'Khách đến trực tiếp quầy hoặc văn phòng', 'orange', 50),
        ('INTERNAL', 'Nội bộ', 'Yêu cầu được tạo bởi đội vận hành nội bộ', 'purple', 60)
) AS seed(code, name, description, color, sort_order)
ON CONFLICT (tenant_id, code) DO NOTHING;

ALTER TABLE service_requests
    ADD CONSTRAINT fk_service_request_channel
    FOREIGN KEY (tenant_id, channel)
    REFERENCES service_request_channels(tenant_id, code);
