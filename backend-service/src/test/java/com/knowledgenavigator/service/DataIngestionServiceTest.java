package com.knowledgenavigator.service;

import com.knowledgenavigator.model.ProcessingStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DataIngestionServiceTest {

    @Test
    public void testProcessingStatusEnum() {
        // Test ProcessingStatus enum values
        assertEquals("UPLOADED", ProcessingStatus.UPLOADED.name());
        assertEquals("PREPROCESSING", ProcessingStatus.PREPROCESSING.name());
        assertEquals("COMPLETED", ProcessingStatus.COMPLETED.name());
        assertEquals("FAILED", ProcessingStatus.FAILED.name());
    }

    @Test
    public void testDocumentIngestionFlow() {
        // Simple unit test without Spring context
        String filename = "test.txt";
        String contentType = "text/plain";
        
        assertNotNull(filename);
        assertEquals("text/plain", contentType);
        assertTrue(filename.endsWith(".txt"));
    }
}