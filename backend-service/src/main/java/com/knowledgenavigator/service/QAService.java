package com.knowledgenavigator.service;

import com.knowledgenavigator.model.QASession;
import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.repository.QASessionRepository;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QAService {

    private static final Logger logger = LoggerFactory.getLogger(QAService.class);

    @Autowired
    private QASessionRepository qaSessionRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private VectorSearchService vectorSearchService;
    
    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private com.knowledgenavigator.repository.DocumentRepository documentRepository;

    @Value("${ai.service.url:http://localhost:8002}")
    private String aiServiceUrl;

    @Value("${qa.max.chunks:10}")
    private int maxChunks;

    @Value("${qa.max.context.length:4000}")
    private int maxContextLength;

    @Value("${qa.chunks.per.document:3}")
    private int chunksPerDocument;

    public Map<String, Object> processQuestion(String userId, String question) {
        logger.info("Processing Q&A question for user: {}", userId);
        logger.debug("Question: {}", question);
        try {
            // Check user permissions
            if (!securityService.checkUserPermission(userId)) {
                logger.warn("Permission denied for user: {}", userId);
                return Map.of("error", "Insufficient permissions", "status", "PERMISSION_DENIED");
            }

            // Create QA session
            QASession session = new QASession();
            session.setId(UUID.randomUUID().toString());
            session.setUserId(userId);
            session.setQuestion(question);
            session.setCreatedDate(LocalDateTime.now());
            session.setStatus("PROCESSING");

            // Expand query with AI
            Map<String, Object> expandedQuery = expandQueryWithAI(question);
            String processedQuery = (String) expandedQuery.getOrDefault("expanded_query", preprocessQuery(question));
            List<String> keywords = (List<String>) expandedQuery.getOrDefault("keywords", extractKeywords(processedQuery));
            Boolean isCreative = (Boolean) expandedQuery.getOrDefault("is_creative", false);
            List<String> alternatives = (List<String>) expandedQuery.getOrDefault("alternatives", Arrays.asList(question));
            
            session.setProcessedQuery(processedQuery);
            session.setKeywords(keywords);
            session.setIsCreative(isCreative);
            logger.info("Expanded query with {} keywords and {} alternatives", keywords.size(), alternatives.size());

            // Get all document languages and search iteratively with expanded queries
            List<String> documentLanguages = getDocumentLanguages();
            List<String> foundDocuments = findDocumentsWithExpandedQueries(alternatives, documentLanguages);
            session.setFoundDocuments(foundDocuments);
            logger.info("Found {} related documents across {} languages", foundDocuments.size(), documentLanguages.size());

            if (foundDocuments.isEmpty()) {
                logger.warn("No documents found for query: {}", processedQuery);
                session.setStatus("NO_DOCUMENTS_FOUND");
                session.setAnswer("No suitable information found");
                qaSessionRepository.save(session);
                return Map.of("answer", "No suitable information found", "sources", Collections.emptyList());
            }

            // Get document metadata and check access permissions
            List<DocumentChunk> accessibleChunks = getAccessibleDocuments(userId, foundDocuments);
            logger.info("User {} has access to {} chunks from {} documents", userId, accessibleChunks.size(), foundDocuments.size());
            
            if (accessibleChunks.isEmpty()) {
                logger.warn("Access denied for user {} to all relevant documents", userId);
                session.setStatus("ACCESS_DENIED");
                session.setAnswer("Access denied to relevant documents");
                qaSessionRepository.save(session);
                return Map.of("error", "Access denied to relevant documents", "status", "ACCESS_DENIED");
            }

            // Summarize answer using LLM
            String answer = summarizeAnswer(processedQuery, isCreative, accessibleChunks);
            List<String> sources = extractSources(accessibleChunks);

            session.setAnswer(answer);
            session.setSources(sources);
            session.setStatus("COMPLETED");
            qaSessionRepository.save(session);
            
            logger.info("Successfully completed Q&A session: {} with {} sources", session.getId(), sources.size());

            return Map.of(
                "answer", answer,
                "sources", sources,
                "session_id", session.getId(),
                "status", "SUCCESS"
            );

        } catch (Exception e) {
            logger.error("Error processing Q&A question for user {}: {}", userId, e.getMessage(), e);
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
        List<DocumentChunk> selectedChunks = new ArrayList<>();
        int totalLength = 0;
        
        for (String docId : documentIds) {
            if (!this.checkDocumentAccess(userId, docId)) {
                continue;
            }
            
            // Get limited chunks per document at database level
            List<DocumentChunk> docChunks = chunkRepository.findTopByDocumentIdOrderByChunkIndex(docId, chunksPerDocument);
            
            for (DocumentChunk chunk : docChunks) {
                if (selectedChunks.size() >= maxChunks) {
                    break;
                }
                
                int chunkLength = chunk.getContent().length();
                if (totalLength + chunkLength > maxContextLength) {
                    break;
                }
                
                selectedChunks.add(chunk);
                totalLength += chunkLength;
            }
            
            if (selectedChunks.size() >= maxChunks) {
                break;
            }
        }
        
        return selectedChunks;
    }
    
    public boolean checkDocumentAccess(String userId, String documentId) {
        List<String> accessibleDocIds = authorizationService.getAccessibleDocuments(userId);
        logger.debug("Found {} of accessible Doc", accessibleDocIds);
        // Implement document-level access control
        // For now, allow access to all documents for authenticated users
        logger.debug("Checking access for user {} to document {}", userId, documentId);
        return userId != null && !userId.isEmpty() && accessibleDocIds.contains(documentId);
    }

    private String summarizeAnswer(String query, Boolean isCreative, List<DocumentChunk> chunks) {
        try {
            String context = chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));

            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/qa")
                .bodyValue(Map.of(
                    "question", query,
                    "is_creative", isCreative,
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

    private List<String> getDocumentLanguages() {
        try {
            return documentRepository.findAll().stream()
                .map(doc -> doc.getLanguage())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting document languages: {}", e.getMessage());
            return Arrays.asList("en");
        }
    }

    private Map<String, Object> expandQueryWithAI(String query) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/expand-query")
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return response != null ? response : Map.of("expanded_query", query);
        } catch (Exception e) {
            logger.error("Error expanding query: {}", e.getMessage());
            return Map.of("expanded_query", query, "keywords", extractKeywords(query), "alternatives", Arrays.asList(query));
        }
    }

    private List<String> findDocumentsWithExpandedQueries(List<String> queries, List<String> languages) {
        for (String query : queries) {
            for (String language : languages) {
                try {
                    logger.debug("Searching query '{}' in language: {}", query, language);
                    String translatedQuery = translateToLanguage(query, language);
                    List<Double> queryEmbedding = createQueryEmbedding(translatedQuery);
                    List<String> documents = vectorSearchService.findRelatedDocuments(queryEmbedding);
                    
                    if (!documents.isEmpty()) {
                        logger.info("Found {} documents for query '{}' in language: {}", documents.size(), query, language);
                        return documents;
                    }
                } catch (Exception e) {
                    logger.error("Error searching query '{}' in language {}: {}", query, language, e.getMessage());
                }
            }
        }
        return Collections.emptyList();
    }

    private String translateToLanguage(String query, String targetLanguage) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/translate")
                .bodyValue(Map.of(
                    "text", query,
                    "target_languages", Arrays.asList(targetLanguage)
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            Map<String, String> translations = (Map<String, String>) response.get("translations");
            return translations.getOrDefault(targetLanguage, query);
        } catch (Exception e) {
            logger.error("Error translating to {}: {}", targetLanguage, e.getMessage());
            return query;
        }
    }

    private List<String> extractSources(List<DocumentChunk> chunks) {
        return chunks.stream()
            .map(chunk -> (String) chunk.getMetadata().get("source"))
            .distinct()
            .collect(Collectors.toList());
    }
}