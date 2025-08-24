package com.knowledgenavigator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class InsightsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInsights() {
        // Mock insights data for frontend compatibility
        List<Map<String, Object>> insights = List.of(
            Map.of(
                "id", "1",
                "title", "Document Processing Pipeline",
                "content", "The system now supports a three-stage processing pipeline with data ingestion, preprocessing, and knowledge base indexing.",
                "category", "technical",
                "importance_score", 0.9,
                "created_date", LocalDateTime.now().toString()
            ),
            Map.of(
                "id", "2", 
                "title", "Q&A System Enhancement",
                "content", "Enhanced Q&A system with user authentication, semantic search, and context-aware answer generation.",
                "category", "process",
                "importance_score", 0.85,
                "created_date", LocalDateTime.now().toString()
            )
        );

        return ResponseEntity.ok(Map.of("insights", insights));
    }
}