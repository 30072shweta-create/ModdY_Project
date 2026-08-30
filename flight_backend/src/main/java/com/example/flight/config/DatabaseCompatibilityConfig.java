package com.example.flight.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseCompatibilityConfig {

    @Bean
    CommandLineRunner removeLegacyRefreshTokenSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.refresh_tokens "
                    + "DROP CONSTRAINT IF EXISTS refresh_tokens_user_id_key");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.refresh_tokens "
                    + "DROP COLUMN IF EXISTS token");
        };
    }
}
