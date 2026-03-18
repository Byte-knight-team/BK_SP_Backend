package com.ByteKnights.com.resturarent_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

        // Disabled due to mismatch with current Order entity and schema.
        // Use data.sql for seeding for now.

        @Override
        public void run(String... args) throws Exception {
                // No-op
        }
}
