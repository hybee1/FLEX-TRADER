

-- Creates a partial unique index to ensure each user has only one active subscription (is_cancelled = false)
CREATE UNIQUE INDEX one_active_subscription_per_user
ON subscription_plan_tbl (user_id)
WHERE is_cancelled = false;
