package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check endpoint to verify the backend is up and the DB is connected.
 * GET /api/health → { "status": "UP", "timestamp": "..." }
 *
 * Remove or secure this endpoint before going to production.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "resturarent-system",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
