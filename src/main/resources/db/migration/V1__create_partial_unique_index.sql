
-- V1__create_partial_unique_index.sql

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE indexname = 'unique_active_token_per_user'
    ) THEN
        CREATE UNIQUE INDEX unique_active_token_per_user
        ON users_tokens_tbl(user_id)
        WHERE is_revoked = false AND toke_expiry_date > now();
    END IF;
END
$$;
