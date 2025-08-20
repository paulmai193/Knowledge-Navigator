package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.Document;
import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.repository.DocumentRepository;
import com.knowledgenavigator.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processing")
@CrossOrigin(origins = "*")
public class ProcessingController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/status/{documentId}")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable String documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
            "document_id", documentId,
            "filename", document.getFilename(),
            "status", document.getProcessingStatus(),
            "processed", document.isProcessed(),
            "upload_date", document.getUploadDate()
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        int limit = Integer.parseInt(request.getOrDefault("limit", "10"));

        List<DocumentChunk> results = knowledgeBaseService.searchSimilarChunks(query, limit);

        return ResponseEntity.ok(Map.of(
            "query", query,
            "results", results,
            "count", results.size()
        ));
    }
}