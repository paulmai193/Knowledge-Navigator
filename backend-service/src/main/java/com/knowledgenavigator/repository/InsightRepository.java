package com.knowledgenavigator.repository;

import com.knowledgenavigator.model.Insight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InsightRepository extends MongoRepository<Insight, String> {
    List<Insight> findByDocumentIdIn(List<String> documentIds);
    List<Insight> findByDocumentId(String documentId);
}