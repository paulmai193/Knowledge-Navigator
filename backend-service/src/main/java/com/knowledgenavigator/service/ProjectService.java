package com.knowledgenavigator.service;

import com.knowledgenavigator.model.Project;
import com.knowledgenavigator.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(String id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Project createProject(String name, String description, String createdBy) {
        Project project = new Project();
        project.setId(UUID.randomUUID().toString());
        project.setName(name);
        project.setDescription(description);
        project.setCreatedBy(createdBy);
        project.setCreatedDate(LocalDateTime.now());
        project.setDocumentIds(new ArrayList<>());
        return projectRepository.save(project);
    }

    public Project updateProject(String id, String name, String description) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project != null) {
            project.setName(name);
            project.setDescription(description);
            return projectRepository.save(project);
        }
        return null;
    }

    public void deleteProject(String id) {
        projectRepository.deleteById(id);
    }
}