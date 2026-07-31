ALTER TABLE work_orders
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by VARCHAR(100);

CREATE INDEX idx_work_order_visible_history
    ON work_orders(tenant_id, status, created_at DESC)
    WHERE deleted_at IS NULL;
