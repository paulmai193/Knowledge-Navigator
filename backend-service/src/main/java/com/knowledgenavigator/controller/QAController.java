package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.QASession;
import com.knowledgenavigator.repository.QASessionRepository;
import com.knowledgenavigator.service.QAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qa")
@CrossOrigin(origins = "*")
public class QAController {

    @Autowired
    private QAService qaService;

    @Autowired
    private QASessionRepository qaSessionRepository;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        // Get user ID from security context instead of request parameter
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        Map<String, Object> result = qaService.processQuestion(userId, question);

        // Format response for frontend compatibility
        if (result.containsKey("answer")) {
            return ResponseEntity.ok(Map.of(
                    "id", result.getOrDefault("session_id", "unknown"),
                    "question", question,
                    "answer", result.get("answer"),
                    "sources", result.getOrDefault("sources", List.of()),
                    "created_date", LocalDateTime.now().toString()));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getQAHistory(@RequestParam(defaultValue = "anonymous") String userId) {
        List<QASession> sessions = qaSessionRepository.findByUserId(userId);

        List<Map<String, Object>> history = sessions.stream()
                .filter(session -> "COMPLETED".equals(session.getStatus()))
                .map(session -> Map.of(
                        "id", session.getId(),
                        "question", session.getQuestion(),
                        "answer", session.getAnswer() != null ? session.getAnswer() : "No answer available",
                        "sources", session.getSources() != null ? session.getSources() : List.of(),
                        "created_date", session.getCreatedDate().toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("qa_history", history));
    }
}