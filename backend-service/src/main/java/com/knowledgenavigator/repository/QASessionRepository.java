package com.knowledgenavigator.repository;

import com.knowledgenavigator.model.QASession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QASessionRepository extends MongoRepository<QASession, String> {
    List<QASession> findByUserId(String userId);
}