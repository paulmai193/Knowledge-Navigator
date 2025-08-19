# Knowledge Navigator

A modern document analysis and knowledge management system powered by AI.

## Features

- **Document Upload & Analysis**: Upload PDF, DOCX, and TXT files for AI-powered analysis
- **Intelligent Insights**: Extract key insights, best practices, and recommendations
- **Q&A System**: Ask questions about your documents and get AI-powered answers
- **Project Management**: Organize documents into projects
- **Dashboard Analytics**: View statistics and insights about your knowledge base

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
- Build and run the FastAPI backend
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
- **API Documentation**: http://localhost:8001/docs
- **MongoDB**: localhost:27017
- **Ollama**: http://localhost:11434

## Environment Configuration

### Backend (.env)
- `MONGO_URL`: MongoDB connection string
- `OLLAMA_URL`: Ollama API URL
- `OLLAMA_MODEL`: AI model name (default: llama3.2)
- `DB_NAME`: Database name
- `CORS_ORIGINS`: Allowed CORS origins

### Frontend (.env)
- `REACT_APP_BACKEND_URL`: Backend API URL

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

### Backend (FastAPI)
- **Port**: 8001
- **Features**: Document processing, AI analysis, REST API
- **Volume**: Upload directory for document storage

### Frontend (React)
- **Port**: 3000
- **Features**: Modern UI, document management, Q&A interface

## API Endpoints

- `GET /api/health` - Health check
- `POST /api/documents/upload` - Upload document
- `GET /api/documents` - List documents
- `GET /api/insights` - Get insights
- `POST /api/qa/ask` - Ask questions
- `GET /api/dashboard/stats` - Dashboard statistics

## Technology Stack

- **Backend**: FastAPI, Python, MongoDB, Motor (async MongoDB driver)
- **Frontend**: React, Axios
- **Database**: MongoDB
- **AI Integration**: Ollama (Local LLM)
- **Deployment**: Docker, Docker Compose