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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PreprocessingService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private TextExtractionService textExtractionService;

    @Autowired
    private com.knowledgenavigator.repository.InsightRepository insightRepository;

    @Value("${ai.service.url:http://localhost:8002}")
    private String aiServiceUrl;

    @Value("${preprocessing.chunk.size:1000}")
    private int chunkSize;

    public void processDocument(String documentId) {
        logger.info("Starting document processing for ID: {}", documentId);
        try {
            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                logger.warn("Document not found: {}", documentId);
                return;
            }
            logger.debug("Processing document: {} ({})", document.getFilename(), document.getContentType());

            // Update status to preprocessing
            document.setProcessingStatus(ProcessingStatus.PREPROCESSING);
            documentRepository.save(document);

            // Extract and clean text
            String content = textExtractionService.extractText(document.getFilePath(), document.getContentType());
            String cleanedContent = cleanText(content);

            // Update status to chunking
            document.setProcessingStatus(ProcessingStatus.CHUNKING);
            documentRepository.save(document);

            // Split into chunks
            List<String> chunks = splitTextToChunks(cleanedContent);
            logger.info("Split document {} into {} chunks", documentId, chunks.size());

            // Extract metadata
            Map<String, Object> metadata = extractMetadata(document, cleanedContent);
            logger.debug("Extracted metadata: word count = {}", metadata.get("wordCount"));

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

            // Store embeddings in Weaviate
            storeEmbeddingsInWeaviate(documentChunks);

            // Store in knowledge base and create search index
            knowledgeBaseService.indexDocument(documentId, documentChunks);

            // Generate and store insights
            generateAndStoreInsights(documentId, cleanedContent);

            // Update final status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessed(true);
            documentRepository.save(document);
            
            logger.info("Successfully completed processing document: {}", documentId);

        } catch (Exception e) {
            logger.error("Error processing document {}: {}", documentId, e.getMessage(), e);
            // Update status to failed
            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(document);
            }
            throw new RuntimeException("Error processing document: " + e.getMessage());
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

    private void storeEmbeddingsInWeaviate(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            vectorSearchService.storeEmbedding(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getContent(),
                chunk.getEmbedding()
            );
        }
    }

    private void generateAndStoreInsights(String documentId, String content) {
        logger.info("Generating insights for document: {}", documentId);
        try {
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/insights")
                .bodyValue(Map.of(
                    "document_id", documentId,
                    "content", content.substring(0, Math.min(4000, content.length()))
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map<String, Object>> insights = (List<Map<String, Object>>) response.get("insights");
            if (insights != null) {
                for (Map<String, Object> insightData : insights) {
                    com.knowledgenavigator.model.Insight insight = new com.knowledgenavigator.model.Insight();
                    insight.setId(UUID.randomUUID().toString());
                    insight.setDocumentId(documentId);
                    insight.setTitle((String) insightData.get("title"));
                    insight.setContent((String) insightData.get("content"));
                    insight.setCategory((String) insightData.get("category"));
                    insight.setImportanceScore(((Number) insightData.get("importance_score")).doubleValue());
                    insight.setCreatedDate(LocalDateTime.now());
                    
                    insightRepository.save(insight);
                }
                logger.info("Stored {} insights for document: {}", insights.size(), documentId);
            }
        } catch (Exception e) {
            logger.error("Error generating insights for document {}: {}", documentId, e.getMessage(), e);
        }
    }
}