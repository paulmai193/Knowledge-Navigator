package com.knowledgenavigator.service;

import com.knowledgenavigator.model.User;
import com.knowledgenavigator.model.Group;
import com.knowledgenavigator.model.DocumentEntity;
import com.knowledgenavigator.model.Role;
import com.knowledgenavigator.repository.UserRepository;
import com.knowledgenavigator.repository.GroupRepository;
import com.knowledgenavigator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void createAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@system.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setGroupIds(new ArrayList<>());
            admin.setCreatedDate(LocalDateTime.now());
            admin.setActive(true);
            userRepository.save(admin);
        }
    }

    // User Management
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedDate(LocalDateTime.now());
        user.setActive(true);
        if (user.getGroupIds() == null) user.setGroupIds(new ArrayList<>());
        return userRepository.save(user);
    }

    public User updateUser(String id, User user) {
        User existing = userRepository.findById(id).orElseThrow();
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setRole(user.getRole());
        existing.setGroupIds(user.getGroupIds());
        existing.setActive(user.isActive());
        return userRepository.save(existing);
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    // Group Management
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group createGroup(Group group) {
        group.setCreatedDate(LocalDateTime.now());
        if (group.getDocumentIds() == null) group.setDocumentIds(new ArrayList<>());
        return groupRepository.save(group);
    }

    public Group updateGroup(String id, Group group) {
        Group existing = groupRepository.findById(id).orElseThrow();
        existing.setName(group.getName());
        existing.setDescription(group.getDescription());
        existing.setDocumentIds(group.getDocumentIds());
        return groupRepository.save(existing);
    }

    public void deleteGroup(String id) {
        // Remove group from all users
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getGroupIds().contains(id)) {
                user.getGroupIds().remove(id);
                userRepository.save(user);
            }
        }
        groupRepository.deleteById(id);
    }

    // Document Management
    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }

    public void assignDocumentToGroups(String documentId, List<String> groupIds) {
        for (String groupId : groupIds) {
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group != null && !group.getDocumentIds().contains(documentId)) {
                group.getDocumentIds().add(documentId);
                groupRepository.save(group);
            }
        }
    }

    public void removeDocumentFromGroups(String documentId, List<String> groupIds) {
        for (String groupId : groupIds) {
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group != null) {
                group.getDocumentIds().remove(documentId);
                groupRepository.save(group);
            }
        }
    }
}