package com.knowledgenavigator.repository;

import com.knowledgenavigator.model.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {
    List<DocumentChunk> findByDocumentId(String documentId);
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(String documentId);
    
    @Query("{'documentId': ?0}")
    List<DocumentChunk> findTopByDocumentIdOrderByChunkIndex(String documentId, org.springframework.data.domain.Pageable pageable);
    
    default List<DocumentChunk> findTopByDocumentIdOrderByChunkIndex(String documentId, int limit) {
        return findTopByDocumentIdOrderByChunkIndex(documentId, 
            org.springframework.data.domain.PageRequest.of(0, limit, 
                org.springframework.data.domain.Sort.by("chunkIndex")));
    }
}