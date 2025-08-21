package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.DocumentEntity;
import com.knowledgenavigator.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadDocument(file));
    }

    @GetMapping
    public ResponseEntity<Map<String, List<DocumentEntity>>> getDocuments() {
        return ResponseEntity.ok(Map.of("documents", documentService.getAllDocuments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }
}