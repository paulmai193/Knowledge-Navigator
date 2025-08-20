package com.knowledgenavigator.service;

import com.knowledgenavigator.model.QASession;
import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.repository.QASessionRepository;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QAService {

    @Autowired
    private QASessionRepository qaSessionRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${ai.service.url:http://localhost:8002}")
    private String aiServiceUrl;

    public Map<String, Object> processQuestion(String userId, String question) {
        try {
            // Check user permissions
            if (!securityService.checkUserPermission(userId)) {
                return Map.of("error", "Insufficient permissions", "status", "PERMISSION_DENIED");
            }

            // Create QA session
            QASession session = new QASession();
            session.setId(UUID.randomUUID().toString());
            session.setUserId(userId);
            session.setQuestion(question);
            session.setCreatedDate(LocalDateTime.now());
            session.setStatus("PROCESSING");

            // Preprocess query
            String processedQuery = preprocessQuery(question);
            List<String> keywords = extractKeywords(processedQuery);
            session.setProcessedQuery(processedQuery);
            session.setKeywords(keywords);

            // Create embedding for query
            List<Double> queryEmbedding = createQueryEmbedding(processedQuery);
            session.setQueryEmbedding(queryEmbedding);

            // Find related documents using vector search
            List<String> foundDocuments = vectorSearchService.findRelatedDocuments(queryEmbedding);
            session.setFoundDocuments(foundDocuments);

            if (foundDocuments.isEmpty()) {
                session.setStatus("NO_DOCUMENTS_FOUND");
                session.setAnswer("No suitable information found");
                qaSessionRepository.save(session);
                return Map.of("answer", "No suitable information found", "sources", Collections.emptyList());
            }

            // Get document metadata and check access permissions
            List<DocumentChunk> accessibleChunks = getAccessibleDocuments(userId, foundDocuments);
            
            if (accessibleChunks.isEmpty()) {
                session.setStatus("ACCESS_DENIED");
                session.setAnswer("Access denied to relevant documents");
                qaSessionRepository.save(session);
                return Map.of("error", "Access denied to relevant documents", "status", "ACCESS_DENIED");
            }

            // Summarize answer using LLM
            String answer = summarizeAnswer(processedQuery, accessibleChunks);
            List<String> sources = extractSources(accessibleChunks);

            session.setAnswer(answer);
            session.setSources(sources);
            session.setStatus("COMPLETED");
            qaSessionRepository.save(session);

            return Map.of(
                "answer", answer,
                "sources", sources,
                "session_id", session.getId(),
                "status", "SUCCESS"
            );

        } catch (Exception e) {
            return Map.of("error", "Error processing question: " + e.getMessage(), "status", "ERROR");
        }
    }

    private String preprocessQuery(String query) {
        return query.toLowerCase()
                   .replaceAll("[^\\p{L}\\p{N}\\s]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private List<String> extractKeywords(String query) {
        return Arrays.stream(query.split("\\s+"))
                    .filter(word -> word.length() > 2)
                    .collect(Collectors.toList());
    }

    private List<Double> createQueryEmbedding(String query) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/embedding")
                .bodyValue(Map.of("text", query))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return (List<Double>) response.get("embedding");
        } catch (Exception e) {
            return Collections.nCopies(384, 0.0);
        }
    }

    private List<DocumentChunk> getAccessibleDocuments(String userId, List<String> documentIds) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (String docId : documentIds) {
            if (securityService.checkDocumentAccess(userId, docId)) {
                chunks.addAll(chunkRepository.findByDocumentId(docId));
            }
        }
        return chunks.stream().limit(5).collect(Collectors.toList());
    }

    private String summarizeAnswer(String query, List<DocumentChunk> chunks) {
        try {
            String context = chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));

            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/qa")
                .bodyValue(Map.of(
                    "question", query,
                    "context", context
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return (String) response.get("answer");
        } catch (Exception e) {
            return "Unable to generate answer";
        }
    }

    private List<String> extractSources(List<DocumentChunk> chunks) {
        return chunks.stream()
            .map(chunk -> (String) chunk.getMetadata().get("source"))
            .distinct()
            .collect(Collectors.toList());
    }
}