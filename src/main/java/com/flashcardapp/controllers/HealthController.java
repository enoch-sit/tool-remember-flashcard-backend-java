package com.flashcardapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints.
 * These endpoints are used to verify that the application is running correctly.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class HealthController {

    /**
     * Simple health check endpoint.
     * Returns status "ok" if the application is running.
     *
     * @return A response with status "ok"
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }
}