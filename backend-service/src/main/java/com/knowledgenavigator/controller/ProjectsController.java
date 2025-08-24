package com.knowledgenavigator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjects() {
        // Mock projects data for frontend compatibility
        List<Map<String, Object>> projects = List.of(
            Map.of(
                "id", "1",
                "name", "Knowledge Navigator System",
                "description", "Main project for document processing and Q&A system",
                "created_date", LocalDateTime.now().toString(),
                "documents", List.of(),
                "insights", List.of()
            )
        );

        return ResponseEntity.ok(Map.of("projects", projects));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");

        Map<String, Object> project = Map.of(
            "id", String.valueOf(System.currentTimeMillis()),
            "name", name,
            "description", description,
            "created_date", LocalDateTime.now().toString(),
            "documents", List.of(),
            "insights", List.of()
        );

        return ResponseEntity.ok(Map.of("message", "Project created successfully", "project", project));
    }
}