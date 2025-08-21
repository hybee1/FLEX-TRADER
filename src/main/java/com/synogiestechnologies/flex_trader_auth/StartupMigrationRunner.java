package com.synogiestechnologies.flex_trader_auth;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void runIndexCreation() {
        // Token index (wrapped in DO block)
        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_indexes
                    WHERE indexname = 'unique_active_token_per_user'
                ) THEN
                    CREATE UNIQUE INDEX unique_active_token_per_user
                    ON users_tokens_tbl(user_id)
                    WHERE is_revoked = false;
                END IF;
            END
            $$;
        """);

        // Subscription index
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS one_active_subscription_per_user
            ON subscription_plan_tbl (user_id)
            WHERE is_cancelled = false;
        """);
    }
}
