package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void indexDocument(String documentId, List<DocumentChunk> chunks) {
        try {
            // Store chunks in MongoDB
            storeChunksInMongoDB(chunks);
            
            // Store embeddings in vector database (placeholder)
            storeEmbeddingsInVectorDB(chunks);
            
            // Create search indexes
            createSearchIndexes();
            
        } catch (Exception e) {
            throw new RuntimeException("Error indexing document: " + e.getMessage());
        }
    }

    private void storeChunksInMongoDB(List<DocumentChunk> chunks) {
        // Chunks are already saved via repository, this is for additional indexing
        for (DocumentChunk chunk : chunks) {
            // Additional processing if needed
        }
    }

    private void storeEmbeddingsInVectorDB(List<DocumentChunk> chunks) {
        // Placeholder for vector database integration (Qdrant/Weaviate)
        // In production, integrate with actual vector database
        for (DocumentChunk chunk : chunks) {
            // Store embedding in vector DB with metadata
        }
    }

    private void createSearchIndexes() {
        try {
            // Create text index for full-text search
            mongoTemplate.indexOps(DocumentChunk.class)
                .ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("content")
                    .build());

            // Create compound index for efficient queries
            mongoTemplate.indexOps(DocumentChunk.class)
                .ensureIndex(new Index()
                    .on("documentId", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("chunkIndex", org.springframework.data.domain.Sort.Direction.ASC));

        } catch (Exception e) {
            // Log error but don't fail the process
            System.err.println("Error creating search indexes: " + e.getMessage());
        }
    }

    public List<DocumentChunk> searchSimilarChunks(String query, int limit) {
        // Placeholder for vector similarity search
        // In production, use vector database for semantic search
        return mongoTemplate.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("content")
                    .regex(query, "i")
            ).limit(limit),
            DocumentChunk.class
        );
    }
}