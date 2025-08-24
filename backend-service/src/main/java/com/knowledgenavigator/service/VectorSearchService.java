package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private WeaviateClient weaviateClient;

    @Value("${weaviate.class.name:DocumentChunk}")
    private String className;

    @Value("${weaviate.similarity.threshold:0.7}")
    private double similarityThreshold;

    public List<String> findRelatedDocuments(List<Double> queryEmbedding) {
        logger.info("Starting vector search with embedding size: {}", queryEmbedding.size());
        try {
            Float[] vector = queryEmbedding.stream()
                .map(Double::floatValue)
                .toArray(Float[]::new);

            logger.debug("Querying Weaviate with class: {}, vector: {}, threshold: {}", className, vector, similarityThreshold);
            Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                .withClassName(className)
                .withFields(
                    Field.builder().name("documentId").build(),
                    Field.builder().name("_additional").fields(
                        Field.builder().name("distance").build()
                    ).build()
                )
                .withNearVector(NearVectorArgument.builder()
                    .vector(vector)
                    .distance(1.0f - (float) similarityThreshold)
                    .build())
                .withLimit(10)
                .run();

            if (result.hasErrors()) {
                logger.warn("Weaviate query failed with errors: {}", result.getError());
                return fallbackSearch();
            }

            List<String> documentIds = extractDocumentIds(result.getResult());
            logger.info("Found {} related documents from Weaviate", documentIds.size());
            return documentIds;
        } catch (Exception e) {
            logger.error("Error in vector search: {}", e.getMessage(), e);
            return fallbackSearch();
        }
    }

    public void storeEmbedding(String chunkId, String documentId, String content, List<Double> embedding) {
        logger.debug("Storing embedding for chunk: {} in document: {}", chunkId, documentId);
        try {
            Float[] vector = embedding.stream()
                .map(Double::floatValue)
                .toArray(Float[]::new);

            Map<String, Object> properties = new HashMap<>();
            properties.put("chunkId", chunkId);
            properties.put("documentId", documentId);
            properties.put("content", content.substring(0, Math.min(100, content.length())) + "...");

            weaviateClient.data().creator()
                .withClassName(className)
                .withProperties(properties)
                .withVector(vector)
                .run();
            
            logger.info("Successfully stored embedding for chunk: {}", chunkId);
        } catch (Exception e) {
            logger.error("Failed to store embedding in Weaviate for chunk {}: {}", chunkId, e.getMessage(), e);
        }
    }

    private List<String> fallbackSearch() {
        logger.warn("Using fallback search - Weaviate unavailable");
        List<String> fallbackResults = chunkRepository.findAll().stream()
            .limit(10)
            .map(DocumentChunk::getDocumentId)
            .distinct()
            .collect(Collectors.toList());
        logger.info("Fallback search returned {} documents", fallbackResults.size());
        return fallbackResults;
    }

    private List<String> extractDocumentIds(GraphQLResponse response) {
        List<String> documentIds = new ArrayList<>();
        try {
            Object dataObj = response.getData();
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                if (data.containsKey("Get")) {
                    Object getObj = data.get("Get");
                    if (getObj instanceof Map) {
                        Map<String, Object> get = (Map<String, Object>) getObj;
                        if (get.containsKey(className)) {
                            Object chunksObj = get.get(className);
                            if (chunksObj instanceof List) {
                                List<?> chunks = (List<?>) chunksObj;
                                for (Object chunkObj : chunks) {
                                    if (chunkObj instanceof Map) {
                                        Map<String, Object> chunk = (Map<String, Object>) chunkObj;
                                        String docId = (String) chunk.get("documentId");
                                        if (docId != null) {
                                            documentIds.add(docId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Weaviate response: {}", e.getMessage(), e);
        }
        return documentIds.stream().distinct().collect(Collectors.toList());
    }

    public double calculateSimilarity(List<Double> embedding1, List<Double> embedding2) {
        // Cosine similarity calculation
        if (embedding1.size() != embedding2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += Math.pow(embedding1.get(i), 2);
            norm2 += Math.pow(embedding2.get(i), 2);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}