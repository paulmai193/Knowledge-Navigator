package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentEntity;
import com.knowledgenavigator.model.ProcessingStatus;
import com.knowledgenavigator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DataIngestionService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PreprocessingService preprocessingService;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    public Map<String, Object> ingestDocument(MultipartFile file) {
        try {
            String fileId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String storedFilename = fileId + fileExtension;
            
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            
            Path filePath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath);

            DocumentEntity document = new DocumentEntity();
            document.setId(fileId);
            document.setFilename(originalFilename);
            document.setStoredFilename(storedFilename);
            document.setFilePath(filePath.toString());
            document.setContentType(file.getContentType());
            document.setUploadDate(LocalDateTime.now());
            document.setProcessed(false);
            document.setFileSize(file.getSize());
            document.setProcessingStatus(ProcessingStatus.UPLOADED);

            documentRepository.save(document);

            // Start preprocessing pipeline
            preprocessingService.processDocument(fileId);

            return Map.of(
                "message", "Document ingested successfully",
                "document_id", fileId,
                "filename", originalFilename,
                "status", ProcessingStatus.UPLOADED
            );
        } catch (IOException e) {
            throw new RuntimeException("Error ingesting document: " + e.getMessage());
        }
    }
}