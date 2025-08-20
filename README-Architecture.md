# Knowledge Navigator - Microservices Architecture

## Architecture Overview

The system has been split into two main services following the component diagram:

### 1. Backend Service (Java Spring Boot) - Port 8001
- **Technology**: Java 17, Spring Boot 3.2, MongoDB
- **Responsibilities**:
  - REST API for frontend
  - Document upload and metadata management
  - User authentication and authorization
  - Project management
  - Data/metadata CRUD operations
  - Scheduling and preprocessing

### 2. AI/NLP Service (Python) - Port 8002
- **Technology**: Python 3.11, FastAPI, Ollama
- **Responsibilities**:
  - Document text extraction and analysis
  - AI-powered insight generation
  - Q&A processing with LLM
  - Text embeddings for search
  - NLP tasks and processing

## Service Communication

- Backend Service communicates with AI Service via REST API
- AI Service processes documents asynchronously
- Both services share MongoDB for data persistence
- Frontend communicates only with Backend Service

## Data Storage

- **MongoDB**: Document metadata, insights, projects, Q&A history
- **Qdrant/Weaviate**: Vector embeddings for semantic search (future)
- **PostgreSQL**: User management and logs (future)

## Deployment

```bash
# Build and start all services
docker-compose up --build

# Access points:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8001
# AI Service: http://localhost:8002
# MongoDB: localhost:27017
# Ollama: http://localhost:11434
```

## API Endpoints

### Backend Service (8001)
- `GET /api/health` - Health check
- `POST /api/documents/upload` - Upload document
- `GET /api/documents` - List documents
- `POST /api/qa/ask` - Ask questions (proxies to AI service)

### AI Service (8002)
- `GET /health` - Health check
- `POST /process` - Process document
- `POST /qa` - Answer questions
- `POST /embedding` - Generate embeddings