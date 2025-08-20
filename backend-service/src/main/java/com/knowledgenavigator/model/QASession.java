package com.knowledgenavigator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "qa_sessions")
public class QASession {
    @Id
    private String id;
    private String userId;
    private String question;
    private String processedQuery;
    private List<String> keywords;
    private List<Double> queryEmbedding;
    private List<String> foundDocuments;
    private String answer;
    private List<String> sources;
    private LocalDateTime createdDate;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getProcessedQuery() { return processedQuery; }
    public void setProcessedQuery(String processedQuery) { this.processedQuery = processedQuery; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<Double> getQueryEmbedding() { return queryEmbedding; }
    public void setQueryEmbedding(List<Double> queryEmbedding) { this.queryEmbedding = queryEmbedding; }

    public List<String> getFoundDocuments() { return foundDocuments; }
    public void setFoundDocuments(List<String> foundDocuments) { this.foundDocuments = foundDocuments; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}