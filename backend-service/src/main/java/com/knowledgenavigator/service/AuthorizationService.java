package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Group;
import com.knowledgenavigator.model.Project;
import com.knowledgenavigator.model.User;
import com.knowledgenavigator.repository.DocumentRepository;
import com.knowledgenavigator.repository.GroupRepository;
import com.knowledgenavigator.repository.ProjectRepository;
import com.knowledgenavigator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
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
        List<String> accessibleDocIds = new ArrayList<>();
        if (user == null) return List.of();
        
        // Admin can access all documents
        if (user.getRole().name().equals("ADMIN")) {
            return groupRepository.findAll().stream()
                           .flatMap(group -> group.getDocumentIds().stream())
                           .distinct()
                           .toList();
        }
        
        // First priority: Documents in user's projects
        List<Project> userProjects = projectRepository.findAll().stream()
                                             .filter(project -> project.getUserIds() != null && project.getUserIds().contains(username))
                                             .collect(Collectors.toList());
        
        for (Project project : userProjects) {
            List<String> projectDocIds = documentRepository.findAll().stream()
                                                 .filter(doc -> project.getId().equals(doc.getProjectId()))
                                                 .map(doc -> doc.getId())
                                                 .collect(Collectors.toList());
            accessibleDocIds.addAll(projectDocIds);
        }
        
        // Second priority: Documents in user's groups (if not in any project)
        // Get documents from user's groups and projects also
        accessibleDocIds.addAll(groupRepository.findAllById(user.getGroupIds()).stream()
                       .flatMap(group -> group.getDocumentIds().stream())
                       .distinct()
                       .toList());
        
        return accessibleDocIds;
    }
}