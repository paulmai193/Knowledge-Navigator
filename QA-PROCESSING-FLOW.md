# Q&A Processing Flow

This document describes the complete Q&A processing flow implemented following the QnA-sequence-diagram.png.

## Flow Sequence

### 1. User Authentication & Query Submission
- **User Login**: User authenticates through Web UI
- **Enter Query/Question**: User submits question via Web UI
- **Send Query/Question**: Web UI sends request to Spring Boot API

### 2. Permission & Security Check
- **Check User Permission**: Spring Security validates user permissions
- **Permission Status**: Returns granted/denied status
- **Alt Flow**: If permission denied, return error message

### 3. Query Preprocessing
- **Preprocess Query**: Normalize, extract keywords from user question
- **Create Embedding for Query**: Generate vector embedding using Python NLP Microservice
- **Return Embedding**: Vector representation of the query

### 4. Document Discovery
- **Find Related Documents**: Search vector database (QDRANT/WEAVIATE) for similar documents
- **Return Found Documents**: List of relevant document IDs

### 5. Access Control & Metadata Retrieval
- **Get Document Metadata**: Retrieve document information from MongoDB
- **Return Metadata**: Document details and access permissions
- **Check Document Access Permission**: Verify user can access found documents
- **Return Access Status**: Permitted/denied for each document

### 6. Answer Generation
- **Alt Flow - Suitable Documents Found**:
  - **Summarize Answer**: Generate response using LLM Service with document context
  - **Return Summary**: AI-generated answer
  - **Log Query & Result**: Store Q&A session in PostgreSQL
  - **Return Answer & Source Link**: Display answer with source references

- **Alt Flow - No Suitable Documents Found**:
  - **Log Query & Result**: Record failed search attempt
  - **Return Notification**: "No suitable information found"

- **Alt Flow - Permission Denied**:
  - **Log Query & Result**: Record access denied attempt
  - **Return Error Notification**: "Insufficient permissions"

## Key Components

### Models
- `QASession`: Tracks complete Q&A interaction lifecycle
- `DocumentChunk`: Text chunks with embeddings for semantic search

### Services
- `QAService`: Main orchestrator for Q&A processing
- `SecurityService`: User and document access control
- `VectorSearchService`: Semantic similarity search
- `PreprocessingService`: Query normalization and keyword extraction

### External Services
- **Python NLP Microservice**: Embedding generation and answer summarization
- **QDRANT/WEAVIATE**: Vector database for semantic search
- **MongoDB**: Document metadata and chunk storage
- **PostgreSQL**: Q&A session logging
- **LLM Service**: Answer generation using language models

## API Endpoints

- `POST /api/qa/ask` - Submit question and get answer
- `GET /api/qa/history/{userId}` - Get user's Q&A history
- `GET /api/qa/session/{sessionId}` - Get specific Q&A session details

## Processing Status Flow

```
PROCESSING → PERMISSION_CHECK → QUERY_PREPROCESSING → DOCUMENT_SEARCH → ACCESS_CONTROL → ANSWER_GENERATION → COMPLETED
                ↓                                                                                                    ↓
        PERMISSION_DENIED                                                                                    NO_DOCUMENTS_FOUND
                ↓                                                                                                    ↓
            ERROR_LOGGED                                                                                        ERROR_LOGGED
```

## Configuration

```properties
# Q&A Service settings
qa.max.documents=5
qa.similarity.threshold=0.7
qa.answer.max.length=500

# Security settings
security.document.access.enabled=true
security.user.permission.required=true

# Vector Search settings
vector.search.top.k=10
vector.similarity.algorithm=cosine
```

## Error Handling

- **Permission Denied**: User lacks access to Q&A system
- **No Documents Found**: No relevant documents match the query
- **Access Denied**: User cannot access relevant documents
- **Processing Error**: Technical failure in Q&A pipeline

All errors are logged with session details for audit and debugging purposes.