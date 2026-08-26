-- Preserve the actor identity shown in Work Order timelines and audit history.
-- Username remains the stable technical identifier; display name and role are snapshots
-- so historical activity does not change when an account is renamed later.
ALTER TABLE work_order_status_history
    ADD COLUMN actor_display_name VARCHAR(150),
    ADD COLUMN actor_role VARCHAR(40);

ALTER TABLE audit_logs
    ADD COLUMN actor_display_name VARCHAR(150),
    ADD COLUMN actor_role VARCHAR(40);

UPDATE work_order_status_history h
SET actor_display_name = u.display_name,
    actor_role = u.role
FROM user_accounts u
WHERE u.tenant_id = h.tenant_id
  AND lower(u.username) = lower(h.changed_by);

UPDATE audit_logs a
SET actor_display_name = u.display_name,
    actor_role = u.role
FROM user_accounts u
WHERE u.tenant_id = a.tenant_id
  AND lower(u.username) = lower(a.actor_username);

UPDATE work_order_status_history
SET actor_display_name = 'Hệ thống',
    actor_role = 'SYSTEM'
WHERE lower(changed_by) = 'system'
  AND actor_display_name IS NULL;

UPDATE audit_logs
SET actor_display_name = 'Hệ thống',
    actor_role = 'SYSTEM'
WHERE lower(actor_username) = 'system'
  AND actor_display_name IS NULL;

UPDATE work_order_status_history
SET actor_display_name = changed_by
WHERE actor_display_name IS NULL;

UPDATE audit_logs
SET actor_display_name = actor_username
WHERE actor_display_name IS NULL;
