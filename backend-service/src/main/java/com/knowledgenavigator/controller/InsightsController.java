package com.knowledgenavigator.controller;

import com.knowledgenavigator.model.Insight;
import com.knowledgenavigator.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class InsightsController {

    @Autowired
    private InsightService insightService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInsights() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<Insight> insights = insightService.getAccessibleInsights(username);
        return ResponseEntity.ok(Map.of("insights", insights));
    }
}