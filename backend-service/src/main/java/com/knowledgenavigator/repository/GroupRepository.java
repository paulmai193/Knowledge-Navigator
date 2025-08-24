package com.knowledgenavigator.repository;

import com.knowledgenavigator.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    List<Group> findByDocumentIdsContaining(String documentId);
}