package com.knowledgenavigator.repository;

import com.knowledgenavigator.model.DocumentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {
}