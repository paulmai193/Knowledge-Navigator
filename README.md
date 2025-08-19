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
- Windows environment (scripts provided for Windows)

### Production Deployment

1. Clone the repository
2. Run the deployment script:
```bash
scripts\deploy.bat
```

This will:
- Start MongoDB database
- Build and run the FastAPI backend
- Build and run the React frontend
- Set up networking between services

### Development Environment

For development with hot reload:
```bash
scripts\dev.bat
```

### Stop Services

To stop all running services:
```bash
scripts\stop.bat
```

## Access Points

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8001
- **API Documentation**: http://localhost:8001/docs
- **MongoDB**: localhost:27017

## Environment Configuration

The application uses environment variables for configuration:

### Backend (.env)
- `MONGO_URL`: MongoDB connection string
- `EMERGENT_LLM_KEY`: API key for LLM integration
- `DB_NAME`: Database name
- `CORS_ORIGINS`: Allowed CORS origins

### Frontend (.env)
- `REACT_APP_BACKEND_URL`: Backend API URL

## Docker Services

### MongoDB
- **Image**: mongo:7.0
- **Port**: 27017
- **Volume**: Persistent data storage

### Backend (FastAPI)
- **Port**: 8001
- **Features**: Document processing, AI analysis, REST API
- **Volume**: Upload directory for document storage

### Frontend (React)
- **Port**: 3000
- **Features**: Modern UI, document management, Q&A interface

## Development

### Manual Setup (without Docker)

#### Backend
```bash
cd backend
pip install -r requirements.txt
uvicorn server:app --host 0.0.0.0 --port 8001 --reload
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

#### MongoDB
Install and run MongoDB locally on port 27017

## API Endpoints

- `GET /api/health` - Health check
- `POST /api/documents/upload` - Upload document
- `GET /api/documents` - List documents
- `GET /api/insights` - Get insights
- `POST /api/qa/ask` - Ask questions
- `GET /api/dashboard/stats` - Dashboard statistics

## Technology Stack

- **Backend**: FastAPI, Python, MongoDB, Motor (async MongoDB driver)
- **Frontend**: React, Tailwind CSS, Radix UI components
- **Database**: MongoDB
- **AI Integration**: Emergent LLM (Gemini)
- **Deployment**: Docker, Docker Compose