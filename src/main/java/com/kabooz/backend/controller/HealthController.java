package com.kabooz.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight health-check endpoint.
 * Used by Render and UptimeRobot to verify the service is running.
 * No authentication required (permitted in SecurityConfig).
 * Handles both the root URL "/" and "/api/health" so any uptime
 * monitor target keeps the Render free-tier container alive.
 */
@RestController
public class HealthController {

    /** UptimeRobot default target — HEAD/GET https://kabooz-backend.onrender.com/ */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "kabooz-backend",
                "timestamp", Instant.now().toString()
        ));
    }

    /** Standard health endpoint — GET /api/health */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "kabooz-backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
