package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentEntity;
import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.model.ProcessingStatus;
import com.knowledgenavigator.repository.DocumentRepository;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

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
            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
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
            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(document);
            }
            throw new RuntimeException("Error processing document: " + e.getMessage());
        }
    }

    private String extractTextContent(String filePath, String contentType) throws IOException {
        try {
            switch (contentType) {
                case "text/plain":
                    return Files.readString(Paths.get(filePath));
                case "application/pdf":
                    return extractPdfText(filePath);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return extractDocxText(filePath);
                case "application/msword":
                    return extractDocText(filePath);
                default:
                    return "Content extraction not supported for: " + contentType;
            }
        } catch (Exception e) {
            throw new IOException("Error extracting text from file: " + e.getMessage(), e);
        }
    }

    private String extractPdfText(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new FileInputStream(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocText(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
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

    private Map<String, Object> extractMetadata(DocumentEntity document, String content) {
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