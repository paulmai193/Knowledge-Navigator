package com.knowledgenavigator.controller;

import com.knowledgenavigator.repository.DocumentRepository;
import com.knowledgenavigator.repository.QASessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private QASessionRepository qaSessionRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalDocuments = documentRepository.count();
        long processedDocuments = documentRepository.findAll().stream()
            .mapToLong(doc -> doc.isProcessed() ? 1 : 0)
            .sum();
        long totalQASessions = qaSessionRepository.count();

        double processingRate = totalDocuments > 0 ? 
            (double) processedDocuments / totalDocuments * 100 : 0;

        Map<String, Object> stats = Map.of(
            "total_documents", totalDocuments,
            "processed_documents", processedDocuments,
            "total_insights", processedDocuments * 2, // Mock: 2 insights per document
            "total_projects", 1L, // Mock data
            "processing_rate", processingRate,
            "total_qa_sessions", totalQASessions
        );

        return ResponseEntity.ok(stats);
    }
}