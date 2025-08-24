# Knowledge Navigator

A modern document analysis and knowledge management system powered by AI with microservices architecture.

## Features

- **Document Upload & Analysis**: Upload PDF, DOCX, and TXT files for AI-powered analysis
- **Intelligent Insights**: Extract key insights, best practices, and recommendations
- **Q&A System**: Ask questions about your documents and get AI-powered answers
- **User Authentication**: JWT-based authentication with group-based access control
- **Project Management**: Organize documents into projects
- **Dashboard Analytics**: View statistics and insights about your knowledge base

## Architecture Overview

The system follows a microservices architecture with two main services:

### 1. Backend Service (Java Spring Boot) - Port 8001
- **Technology**: Java 17, Spring Boot 3.2, MongoDB
- **Responsibilities**:
  - REST API for frontend
  - Document upload and metadata management
  - User authentication and authorization (JWT)
  - Group-based access control
  - Project management
  - Data/metadata CRUD operations

### 2. AI/NLP Service (Python) - Port 8002
- **Technology**: Python 3.11, FastAPI, Ollama
- **Responsibilities**:
  - Document text extraction (PDF, DOCX, TXT)
  - AI-powered insight generation
  - Q&A processing with LLM
  - Text embeddings for search
  - NLP tasks and processing

## Data Ingestion Pipeline

The system implements a three-stage data ingestion pipeline:

### Stage 1: Data Connector/Ingestion
- Accepts multipart file uploads
- Handles various file types (PDF, DOCX, TXT)
- Stores files in upload directory
- Creates Document record with UPLOADED status

### Stage 2: Preprocessing/NLP
- Extracts and cleans text content
- Normalizes whitespace and characters
- Splits content into manageable chunks (~1000 chars)
- Extracts metadata (source, date, file info)
- Generates vector embeddings using AI service

### Stage 3: Knowledge Base & Indexing
- Stores processed chunks in MongoDB
- Creates full-text search indexes
- Updates document status to COMPLETED

**Processing Status Flow:**
```
UPLOADED → PREPROCESSING → CHUNKING → EMBEDDING → INDEXING → COMPLETED
                                                                ↓
                                                            FAILED (on error)
```

## Q&A Processing Flow

Complete Q&A processing with authentication and access control:

1. **User Authentication**: JWT-based login validation
2. **Permission Check**: Verify user access to Q&A system
3. **Query Preprocessing**: Normalize and extract keywords
4. **Document Discovery**: Vector similarity search
5. **Access Control**: Check user's group permissions for documents
6. **Answer Generation**: LLM-powered response with source attribution

## Authentication & Authorization

- **Registration**: Simple registration without email verification
- **JWT Authentication**: Secure token-based authentication
- **Group-based Access**: Users belong to multiple groups
- **Document Permissions**: Groups have access rights to specific documents
- **Role-based Control**: Admin and User roles with different permissions

## Quick Start with Docker

### Prerequisites
- Docker and Docker Compose installed
- Cross-platform support (Windows, Linux, macOS)

### Production Deployment

**Windows:**
```bash
scripts\deploy.bat
```

**Linux/macOS:**
```bash
./scripts/deploy.sh
```

This will:
- Start MongoDB database
- Start Ollama LLM service
- Build and run the Java backend service
- Build and run the Python AI service
- Build and run the React frontend
- Set up networking between services

### Development Environment

**Windows:**
```bash
scripts\dev.bat
```

**Linux/macOS:**
```bash
./scripts/dev.sh
```

### Setup Ollama Models

After deployment, setup the required AI models:

**Windows:**
```bash
scripts\setup-ollama.bat
```

**Linux/macOS:**
```bash
./scripts/setup-ollama.sh
```

### Stop Services

**Windows:**
```bash
scripts\stop.bat
```

**Linux/macOS:**
```bash
./scripts/stop.sh
```

## Access Points

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8001
- **AI Service**: http://localhost:8002
- **API Documentation**: http://localhost:8001/docs
- **MongoDB**: localhost:27017
- **Ollama**: http://localhost:11434

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Document Management
- `POST /api/documents/upload` - Upload document
- `GET /api/documents` - List accessible documents
- `GET /api/documents/{id}` - Get document details

### Q&A System
- `POST /api/qa/ask` - Ask questions about documents
- `GET /api/qa/history` - Get Q&A history

### Analytics
- `GET /api/dashboard/stats` - Dashboard statistics
- `GET /api/insights` - Get document insights

### System
- `GET /api/health` - Health check

## Environment Configuration

### Backend (.env)
- `MONGO_URL`: MongoDB connection string
- `OLLAMA_URL`: Ollama API URL
- `OLLAMA_MODEL`: AI model name (default: llama3.2)
- `DB_NAME`: Database name
- `CORS_ORIGINS`: Allowed CORS origins

### Frontend (.env)
- `REACT_APP_BACKEND_URL`: Backend API URL

### AI Service (.env)
- `MONGO_URL`: MongoDB connection string
- `OLLAMA_URL`: Ollama API URL
- `OLLAMA_MODEL`: AI model name

## Docker Services

### MongoDB
- **Image**: mongo:7.0
- **Port**: 27017
- **Volume**: Persistent data storage

### Ollama
- **Image**: ollama/ollama:latest
- **Port**: 11434
- **Volume**: Model storage
- **Features**: Local AI model serving

### Backend Service (Java Spring Boot)
- **Port**: 8001
- **Features**: REST API, authentication, document management
- **Volume**: Upload directory for document storage

### AI Service (Python FastAPI)
- **Port**: 8002
- **Features**: Document processing, AI analysis, Q&A
- **Integration**: Ollama for LLM capabilities

### Frontend (React)
- **Port**: 3000
- **Features**: Modern UI, authentication, document management, Q&A interface

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security, JWT, MongoDB, Motor
- **AI Service**: Python 3.11, FastAPI, Ollama, Motor (async MongoDB)
- **Frontend**: React, Axios, Tailwind CSS
- **Database**: MongoDB
- **AI Integration**: Ollama (Local LLM)
- **Document Processing**: Apache PDFBox, Apache POI
- **Deployment**: Docker, Docker Compose

## Supported File Types

- **PDF**: Full text extraction using Apache PDFBox
- **DOCX**: Modern Word documents using Apache POI
- **DOC**: Legacy Word documents using Apache POI
- **TXT**: Plain text files

## Security Features

- JWT-based authentication
- Password encryption with BCrypt
- Group-based document access control
- CORS protection
- Input validation and sanitization
- Secure file upload handling

## Development

### Backend Development
```bash
cd backend-service
./gradlew bootRun
```

### AI Service Development
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8002
```

### Frontend Development
```bash
cd frontend
npm install
npm start
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.