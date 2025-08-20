package com.knowledgenavigator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "documents")
public class Document {
    @Id
    private String id;
    private String filename;
    private String storedFilename;
    private String filePath;
    private String contentType;
    private LocalDateTime uploadDate;
    private boolean processed;
    private long fileSize;
    private ProcessingStatus processingStatus;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
}