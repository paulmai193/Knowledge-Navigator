package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentEntity;
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

    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }

    public DocumentEntity getDocumentById(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    public List<DocumentEntity> getAccessibleDocuments(String username) {
        List<String> accessibleDocIds = authorizationService.getAccessibleDocuments(username);
        return documentRepository.findAllById(accessibleDocIds).stream()
            .filter(doc -> doc.isProcessed())
            .collect(java.util.stream.Collectors.toList());
    }

    @Autowired
    private AuthorizationService authorizationService;
}