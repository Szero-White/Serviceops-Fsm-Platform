-- Time-based overdue alerts can be discovered repeatedly by the scheduler.
-- event_key makes delivery idempotent per recipient and per concrete appointment window.
ALTER TABLE notifications
    ADD COLUMN event_key VARCHAR(180);

CREATE UNIQUE INDEX uq_notification_recipient_event_key
    ON notifications(tenant_id, recipient_user_id, event_key);

CREATE INDEX idx_appointment_overdue_scan
    ON appointments(status, end_time);
