package com.knowledgenavigator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityService {
    
    @Autowired
    private AuthorizationService authorizationService;

    public boolean checkUserPermission(String userId) {
        // Implement user permission check logic
        // For now, allow all authenticated users
        return userId != null && !userId.isEmpty();
    }

    public boolean checkDocumentAccess(String userId, String documentId) {
        List<String> accessibleDocIds = authorizationService.getAccessibleDocuments(userId);
        // Implement document-level access control
        // For now, allow access to all documents for authenticated users
        return userId != null && !userId.isEmpty() && accessibleDocIds.contains(documentId);
    }
}