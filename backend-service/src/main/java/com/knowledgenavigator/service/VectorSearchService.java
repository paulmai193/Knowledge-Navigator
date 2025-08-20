package com.knowledgenavigator.service;

import com.knowledgenavigator.model.DocumentChunk;
import com.knowledgenavigator.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    @Autowired
    private DocumentChunkRepository chunkRepository;

    public List<String> findRelatedDocuments(List<Double> queryEmbedding) {
        // Placeholder for vector similarity search
        // In production, use vector database (Qdrant/Weaviate) for semantic search
        
        // For now, return recent documents as fallback
        List<DocumentChunk> recentChunks = chunkRepository.findAll()
            .stream()
            .limit(10)
            .collect(Collectors.toList());

        return recentChunks.stream()
            .map(DocumentChunk::getDocumentId)
            .distinct()
            .collect(Collectors.toList());
    }

    public double calculateSimilarity(List<Double> embedding1, List<Double> embedding2) {
        // Cosine similarity calculation
        if (embedding1.size() != embedding2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += Math.pow(embedding1.get(i), 2);
            norm2 += Math.pow(embedding2.get(i), 2);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}