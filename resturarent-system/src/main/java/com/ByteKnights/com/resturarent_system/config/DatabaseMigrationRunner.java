package com.ByteKnights.com.resturarent_system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Running database schema migrations...");
            jdbcTemplate.execute("ALTER TABLE reservations MODIFY COLUMN status VARCHAR(50) NOT NULL;");
            log.info("Successfully altered 'status' column in 'reservations' table to VARCHAR(50).");
        } catch (Exception e) {
            log.error("Failed to run database migrations (this is fine if it was already applied): {}", e.getMessage());
        }
    }
}
