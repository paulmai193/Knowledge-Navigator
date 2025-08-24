package com.knowledgenavigator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Backend Service"
        ));
    }
}