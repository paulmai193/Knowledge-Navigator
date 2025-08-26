package com.knowledgenavigator.service;

import com.knowledgenavigator.model.User;
import com.knowledgenavigator.model.Group;
import com.knowledgenavigator.repository.UserRepository;
import com.knowledgenavigator.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GroupAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(GroupAssignmentService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    public void assignDocumentToUserGroups(String documentId, String username) {
        logger.debug("Assigning document {} to groups for user: {}", documentId, username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getGroupIds() == null) {
            logger.warn("User {} not found or has no groups", username);
            return;
        }

        for (String groupId : user.getGroupIds()) {
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group != null && !group.getDocumentIds().contains(documentId)) {
                group.getDocumentIds().add(documentId);
                groupRepository.save(group);
                logger.debug("Assigned document {} to group: {}", documentId, group.getName());
            }
        }
        
        logger.info("Document {} assigned to {} groups for user {}", documentId, user.getGroupIds().size(), username);
    }
}