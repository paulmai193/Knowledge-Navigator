package com.knowledgenavigator.model;

public enum ProcessingStatus {
    UPLOADED,
    PREPROCESSING,
    CHUNKING,
    EMBEDDING,
    INDEXING,
    COMPLETED,
    FAILED
}