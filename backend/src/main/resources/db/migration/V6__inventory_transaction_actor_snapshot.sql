ALTER TABLE inventory_transactions
    ADD COLUMN actor_display_name VARCHAR(150),
    ADD COLUMN actor_role VARCHAR(40);

UPDATE inventory_transactions tx
SET actor_display_name = ua.display_name,
    actor_role = ua.role
FROM user_accounts ua
WHERE tx.tenant_id = ua.tenant_id
  AND lower(tx.created_by) = lower(ua.username);

UPDATE inventory_transactions
SET actor_display_name = 'Hệ thống',
    actor_role = 'SYSTEM'
WHERE lower(created_by) = 'system'
  AND actor_display_name IS NULL;

UPDATE inventory_transactions
SET actor_display_name = created_by
WHERE actor_display_name IS NULL;
