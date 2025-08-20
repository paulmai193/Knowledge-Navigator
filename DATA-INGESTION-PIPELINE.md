# Data Ingestion Pipeline

This document describes the three-stage data ingestion pipeline implemented following the data-ingestion-process-diagram.png.

## Pipeline Stages

### 1. Data Connector/Ingestion
**Service**: `DataIngestionService`
- **Start data connector**: Accepts multipart file uploads
- **Connect to data source**: Handles various file types (PDF, DOCX, TXT)
- **Collecting raw data**: Stores files in upload directory
- **Save temporary raw data**: Creates Document record with UPLOADED status

### 2. Preprocessing/NLP
**Service**: `PreprocessingService`
- **Pre-processing data**: Extracts and cleans text content
- **Clean text, format conversion**: Normalizes whitespace and characters
- **Split text to chunk**: Divides content into manageable chunks (~1000 chars)
- **Extract metadata**: Captures source, author, date, tags, copyright info
- **Generate embedding for each chunk**: Creates vector embeddings using AI service
- **Tag embedding into chunk**: Associates embeddings with chunk metadata

### 3. Knowledge Base & Indexing
**Service**: `KnowledgeBaseService`
- **Store chunk & metadata into MongoDB**: Persists processed chunks
- **Store embedding into Vector DB**: Saves embeddings for similarity search
- **Index chunk/document into Search system**: Creates full-text search indexes
- **Change status ingest to success**: Updates document status to COMPLETED

## Processing Status Flow

```
UPLOADED → PREPROCESSING → CHUNKING → EMBEDDING → INDEXING → COMPLETED
                                                                ↓
                                                            FAILED (on error)
```

## Key Components

### Models
- `Document`: Main document entity with processing status
- `DocumentChunk`: Individual text chunks with embeddings
- `ProcessingStatus`: Enum tracking pipeline stages

### Services
- `DataIngestionService`: Stage 1 - File upload and storage
- `PreprocessingService`: Stage 2 - Text processing and chunking
- `KnowledgeBaseService`: Stage 3 - Storage and indexing

### Controllers
- `ProcessingController`: Exposes status tracking and search endpoints

## API Endpoints

- `POST /api/documents/upload` - Upload document (triggers pipeline)
- `GET /api/processing/status/{id}` - Check processing status
- `POST /api/processing/search` - Search processed documents

## Configuration

```properties
# Application settings
app.upload.dir=/app/uploads
ai.service.url=http://localhost:8002
preprocessing.chunk.size=1000

# AI Service settings
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
```

## Error Handling

- Failed documents are marked with FAILED status
- Processing can be retried by resubmitting to preprocessing
- Partial failures are logged but don't stop the pipeline