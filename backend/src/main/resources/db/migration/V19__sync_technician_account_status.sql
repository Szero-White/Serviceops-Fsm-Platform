-- Technician status and login status are one business state.
-- Heal legacy rows created while technician_profiles.active could diverge from user_accounts.active.
UPDATE technician_profiles tp
SET active = ua.active,
    updated_at = CURRENT_TIMESTAMP,
    version = tp.version + 1
FROM user_accounts ua
WHERE tp.user_id = ua.id
  AND ua.role = 'TECHNICIAN'
  AND tp.active IS DISTINCT FROM ua.active;
