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

            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.payments "
                    + "ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("UPDATE flight_booking_system.payments "
                    + "SET created_at = COALESCE(created_at, updated_at, paid_at, CURRENT_TIMESTAMP) "
                    + "WHERE created_at IS NULL");
            jdbcTemplate.execute("UPDATE flight_booking_system.payments "
                    + "SET status = UPPER(status) "
                    + "WHERE status IS NOT NULL AND status <> UPPER(status)");
            jdbcTemplate.execute("UPDATE flight_booking_system.payments "
                    + "SET payment_method = UPPER(payment_method) "
                    + "WHERE payment_method IS NOT NULL AND payment_method <> UPPER(payment_method)");

            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS tax NUMERIC(12, 2) NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS taxes NUMERIC(12, 2) DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS airport_fee NUMERIC(12, 2) NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS convenience_fee NUMERIC(12, 2) NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS baggage_fee NUMERIC(12, 2) NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS discount NUMERIC(12, 2) NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS final_price NUMERIC(12, 2)");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'INR'");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS flight_booking_system.flight_pricing "
                    + "ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("UPDATE flight_booking_system.flight_pricing "
                    + "SET taxes = COALESCE(taxes, tax, 0), "
                    + "tax = COALESCE(tax, taxes, 0), "
                    + "airport_fee = COALESCE(airport_fee, 0), "
                    + "convenience_fee = COALESCE(convenience_fee, 0), "
                    + "baggage_fee = COALESCE(baggage_fee, 0), "
                    + "discount = COALESCE(discount, 0), "
                    + "currency = COALESCE(currency, 'INR'), "
                    + "final_price = COALESCE(final_price, base_fare + COALESCE(tax, taxes, 0) + COALESCE(airport_fee, 0) + COALESCE(convenience_fee, 0) + COALESCE(baggage_fee, 0) - COALESCE(discount, 0)) "
                    + "WHERE true");
        };
    }
}
