package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Group;
import com.knowledgenavigator.model.User;
import com.knowledgenavigator.repository.GroupRepository;
import com.knowledgenavigator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuthorizationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    public boolean canAccessDocument(String username, String documentId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        // Admin can access all documents
        if (user.getRole().name().equals("ADMIN")) return true;

        // Check if user's groups have access to document
        List<Group> documentGroups = groupRepository.findByDocumentIdsContaining(documentId);
        return documentGroups.stream()
            .anyMatch(group -> user.getGroupIds().contains(group.getId()));
    }

    public List<String> getAccessibleDocuments(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();

        // Admin can access all documents
        if (user.getRole().name().equals("ADMIN")) {
            return groupRepository.findAll().stream()
                .flatMap(group -> group.getDocumentIds().stream())
                .distinct()
                .toList();
        }

        // Get documents from user's groups
        return groupRepository.findAllById(user.getGroupIds()).stream()
            .flatMap(group -> group.getDocumentIds().stream())
            .distinct()
            .toList();
    }
}