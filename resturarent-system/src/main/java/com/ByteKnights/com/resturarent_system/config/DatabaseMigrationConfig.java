package com.ByteKnights.com.resturarent_system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    public CommandLineRunner migratePaymentStatusEnum(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Starting schema migration for payment_status columns...");
                
                // Convert ENUM to VARCHAR(50) to allow for new Java enum values
                jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN payment_status VARCHAR(50)");
                log.info("Migrated orders.payment_status");
                
                jdbcTemplate.execute("ALTER TABLE payments MODIFY COLUMN payment_status VARCHAR(50)");
                log.info("Migrated payments.payment_status");
                
                jdbcTemplate.execute("ALTER TABLE reservation_payments MODIFY COLUMN payment_status VARCHAR(50)");
                log.info("Migrated reservation_payments.payment_status");
                
                log.info("Migration completed successfully.");
            } catch (Exception e) {
                log.error("Migration failed or already applied: {}", e.getMessage());
            }
        };
    }
}
