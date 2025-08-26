package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Project;
import com.knowledgenavigator.model.User;
import com.knowledgenavigator.model.Role;
import com.knowledgenavigator.repository.ProjectRepository;
import com.knowledgenavigator.repository.UserRepository;
import com.knowledgenavigator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class ProjectAuthorizationService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuthorizationService authorizationService;

    public List<String> getAccessibleDocuments(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return new ArrayList<>();

        // ADMIN has full access
        if (user.getRole() == Role.ADMIN) {
            return documentRepository.findAll().stream()
                .map(doc -> doc.getId())
                .collect(Collectors.toList());
        }

        List<String> accessibleDocIds = new ArrayList<>();

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
        if (accessibleDocIds.isEmpty()) {
            accessibleDocIds.addAll(authorizationService.getAccessibleDocuments(username));
        }

        return accessibleDocIds;
    }
}