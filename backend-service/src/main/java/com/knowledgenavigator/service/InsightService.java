package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Insight;
import com.knowledgenavigator.repository.InsightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InsightService {

    @Autowired
    private InsightRepository insightRepository;

    @Autowired
    private AuthorizationService authorizationService;

    public List<Insight> getAccessibleInsights(String username) {
        List<String> accessibleDocIds = authorizationService.getAccessibleDocuments(username);
        return insightRepository.findByDocumentIdIn(accessibleDocIds);
    }
}