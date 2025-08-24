package com.knowledgenavigator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "document_chunks")
public class DocumentChunk {
    @Id
    private String id;
    private String documentId;
    private String content;
    private int chunkIndex;
    private Map<String, Object> metadata;
    private List<Double> embedding;
    private LocalDateTime createdDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}