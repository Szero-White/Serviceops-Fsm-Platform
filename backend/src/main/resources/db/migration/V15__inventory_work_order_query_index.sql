CREATE INDEX idx_inventory_transaction_work_order_flow
    ON inventory_transactions(tenant_id, work_order_id, transaction_type, created_at)
    WHERE work_order_id IS NOT NULL;
