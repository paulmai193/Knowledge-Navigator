package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Document;
import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.model.ProcessingStatus;
import com.knowledgenavigator.repository.DocumentRepository;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class PreprocessingService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${ai.service.url:http://localhost:8002}")
    private String aiServiceUrl;

    @Value("${preprocessing.chunk.size:1000}")
    private int chunkSize;

    public void processDocument(String documentId) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null) return;

            // Update status to preprocessing
            document.setProcessingStatus(ProcessingStatus.PREPROCESSING);
            documentRepository.save(document);

            // Extract and clean text
            String content = extractTextContent(document.getFilePath(), document.getContentType());
            String cleanedContent = cleanText(content);

            // Update status to chunking
            document.setProcessingStatus(ProcessingStatus.CHUNKING);
            documentRepository.save(document);

            // Split into chunks
            List<String> chunks = splitTextToChunks(cleanedContent);

            // Extract metadata
            Map<String, Object> metadata = extractMetadata(document, cleanedContent);

            // Update status to embedding
            document.setProcessingStatus(ProcessingStatus.EMBEDDING);
            documentRepository.save(document);

            // Generate embeddings and store chunks
            List<DocumentChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setId(UUID.randomUUID().toString());
                chunk.setDocumentId(documentId);
                chunk.setContent(chunks.get(i));
                chunk.setChunkIndex(i);
                chunk.setMetadata(metadata);
                chunk.setCreatedDate(LocalDateTime.now());

                // Generate embedding for chunk
                List<Double> embedding = generateEmbedding(chunks.get(i));
                chunk.setEmbedding(embedding);

                documentChunks.add(chunk);
            }

            chunkRepository.saveAll(documentChunks);

            // Update status to indexing
            document.setProcessingStatus(ProcessingStatus.INDEXING);
            documentRepository.save(document);

            // Store in knowledge base and create search index
            knowledgeBaseService.indexDocument(documentId, documentChunks);

            // Update final status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessed(true);
            documentRepository.save(document);

        } catch (Exception e) {
            // Update status to failed
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(document);
            }
            throw new RuntimeException("Error processing document: " + e.getMessage());
        }
    }

    private String extractTextContent(String filePath, String contentType) throws IOException {
        if ("text/plain".equals(contentType)) {
            return Files.readString(Paths.get(filePath));
        }
        // Extend for PDF/DOCX extraction
        return "Content extraction not implemented for: " + contentType;
    }

    private String cleanText(String content) {
        return content.replaceAll("\\s+", " ")
                     .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")
                     .trim();
    }

    private List<String> splitTextToChunks(String content) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = content.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }

    private Map<String, Object> extractMetadata(Document document, String content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", document.getFilename());
        metadata.put("contentType", document.getContentType());
        metadata.put("uploadDate", document.getUploadDate());
        metadata.put("fileSize", document.getFileSize());
        metadata.put("wordCount", content.split("\\s+").length);
        return metadata;
    }

    private List<Double> generateEmbedding(String text) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/embedding")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return (List<Double>) response.get("embedding");
        } catch (Exception e) {
            // Return dummy embedding on failure
            return Collections.nCopies(384, 0.0);
        }
    }
}