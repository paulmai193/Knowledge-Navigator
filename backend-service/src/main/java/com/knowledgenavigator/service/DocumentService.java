package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Document;
import com.knowledgenavigator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Value("${ai.service.url:http://localhost:8002}")
    private String aiServiceUrl;

    public Map<String, Object> uploadDocument(MultipartFile file) {
        try {
            String fileId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String storedFilename = fileId + fileExtension;
            
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            
            Path filePath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath);

            Document document = new Document();
            document.setId(fileId);
            document.setFilename(originalFilename);
            document.setStoredFilename(storedFilename);
            document.setFilePath(filePath.toString());
            document.setContentType(file.getContentType());
            document.setUploadDate(LocalDateTime.now());
            document.setProcessed(false);
            document.setFileSize(file.getSize());

            documentRepository.save(document);

            // Send to AI service for processing
            sendToAiService(fileId, filePath.toString(), file.getContentType());

            return Map.of(
                "message", "Document uploaded successfully",
                "document_id", fileId,
                "filename", originalFilename
            );
        } catch (IOException e) {
            throw new RuntimeException("Error uploading document: " + e.getMessage());
        }
    }

    private void sendToAiService(String documentId, String filePath, String contentType) {
        webClientBuilder.build()
            .post()
            .uri(aiServiceUrl + "/process")
            .bodyValue(Map.of(
                "document_id", documentId,
                "file_path", filePath,
                "content_type", contentType
            ))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe();
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document getDocumentById(String id) {
        return documentRepository.findById(id).orElse(null);
    }
}