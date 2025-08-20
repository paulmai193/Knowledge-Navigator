package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Document;
import com.knowledgenavigator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DataIngestionService dataIngestionService;

    public Map<String, Object> uploadDocument(MultipartFile file) {
        return dataIngestionService.ingestDocument(file);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document getDocumentById(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    public List<Document> getAccessibleDocuments(String userId) {
        // Filter documents based on user access permissions
        return documentRepository.findAll().stream()
            .filter(doc -> doc.isProcessed())
            .collect(java.util.stream.Collectors.toList());
    }
}