package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.Project;
import com.knowledgenavigator.service.ProjectService;
import com.knowledgenavigator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectsController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(Map.of("projects", projects));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        String name = request.get("name");
        String description = request.get("description");
        
        Project project = projectService.createProject(name, description, username);
        return ResponseEntity.ok(Map.of("message", "Project created successfully", "project", project));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        Project project = projectService.getProjectById(id);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable String id, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        
        Project project = projectService.updateProject(id, name, description);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/add-user")
    public ResponseEntity<Project> addUserToProject(@PathVariable String id, @RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        
        // Check if user has permission to add users to this project
        if (!canManageProject(id, currentUser)) {
            return ResponseEntity.status(403).build();
        }
        
        String userId = request.get("userId");
        Project project = projectService.addUserToProject(id, userId);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.notFound().build();
    }

    private boolean canManageProject(String projectId, String username) {
        com.knowledgenavigator.model.User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        
        // ADMIN can manage any project
        if (user.getRole() == com.knowledgenavigator.model.Role.ADMIN) {
            return true;
        }
        
        // PROJECT_OWNER can manage their own projects
        if (user.getRole() == com.knowledgenavigator.model.Role.PROJECT_OWNER) {
            Project project = projectService.getProjectById(projectId);
            return project != null && username.equals(project.getCreatedBy());
        }
        
        return false;
    }
}