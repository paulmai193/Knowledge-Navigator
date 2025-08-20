package com.knowledgenavigator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@CrossOrigin(origins = "*")
public class QAController {

    private final WebClient webClient;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public QAController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        Map<String, Object> response = webClient
            .post()
            .uri(aiServiceUrl + "/qa")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        return ResponseEntity.ok(response);
    }
}