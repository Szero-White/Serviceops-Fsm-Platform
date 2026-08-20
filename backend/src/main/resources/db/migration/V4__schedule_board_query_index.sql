CREATE INDEX idx_appointment_tenant_active_time
    ON appointments(tenant_id, start_time, end_time)
    WHERE status = 'ACTIVE';
