package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.User;
import com.knowledgenavigator.model.Group;
import com.knowledgenavigator.model.DocumentEntity;
import com.knowledgenavigator.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // User Management
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(adminService.createUser(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user) {
        return ResponseEntity.ok(adminService.updateUser(id, user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // Group Management
    @GetMapping("/groups")
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(adminService.getAllGroups());
    }

    @PostMapping("/groups")
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        return ResponseEntity.ok(adminService.createGroup(group));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<Group> updateGroup(@PathVariable String id, @RequestBody Group group) {
        return ResponseEntity.ok(adminService.updateGroup(id, group));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        adminService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    // Document Management
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentEntity>> getAllDocuments() {
        return ResponseEntity.ok(adminService.getAllDocuments());
    }

    @PostMapping("/documents/{id}/assign-groups")
    public ResponseEntity<Void> assignDocumentToGroups(@PathVariable String id, @RequestBody Map<String, List<String>> request) {
        adminService.assignDocumentToGroups(id, request.get("groupIds"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/{id}/remove-groups")
    public ResponseEntity<Void> removeDocumentFromGroups(@PathVariable String id, @RequestBody Map<String, List<String>> request) {
        adminService.removeDocumentFromGroups(id, request.get("groupIds"));
        return ResponseEntity.ok().build();
    }
}