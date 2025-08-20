# Frontend-Backend Integration

## Integration Status: ✅ COMPATIBLE

The current frontend is fully compatible with the new Java backend-service. All required endpoints have been implemented.

## API Endpoint Mapping

### Frontend Expectations → Backend Implementation

| Frontend Call | Backend Endpoint | Status | Controller |
|---------------|------------------|--------|------------|
| `GET /api/documents` | `DocumentController.getDocuments()` | ✅ | DocumentController |
| `POST /api/documents/upload` | `DocumentController.uploadDocument()` | ✅ | DocumentController |
| `GET /api/insights` | `InsightsController.getInsights()` | ✅ | InsightsController |
| `GET /api/projects` | `ProjectsController.getProjects()` | ✅ | ProjectsController |
| `POST /api/projects` | `ProjectsController.createProject()` | ✅ | ProjectsController |
| `GET /api/dashboard/stats` | `DashboardController.getDashboardStats()` | ✅ | DashboardController |
| `POST /api/qa/ask` | `QAController.askQuestion()` | ✅ | QAController |
| `GET /api/qa/history` | `QAController.getQAHistory()` | ✅ | QAController |

## Response Format Compatibility

### Documents Response
```json
{
  "documents": [
    {
      "id": "string",
      "filename": "string", 
      "content_type": "string",
      "upload_date": "string",
      "processed": boolean,
      "insights_count": number
    }
  ]
}
```

### Q&A Response
```json
{
  "id": "string",
  "question": "string",
  "answer": "string", 
  "sources": ["string"],
  "created_date": "string"
}
```

### Dashboard Stats Response
```json
{
  "total_documents": number,
  "processed_documents": number,
  "total_insights": number,
  "total_projects": number,
  "processing_rate": number,
  "total_qa_sessions": number
}
```

## Key Features Preserved

### ✅ Document Management
- File upload with progress tracking
- Document listing with processing status
- Processing status indicators (Processing/Completed)

### ✅ Q&A System  
- Question submission with real-time processing
- Chat-like interface with user/bot messages
- Q&A history tracking
- Source attribution for answers

### ✅ Dashboard Analytics
- Real-time statistics from database
- Processing rate calculations
- Document and insight counters

### ✅ Insights Display
- AI-generated insights with categories
- Importance scoring visualization
- Search and filtering capabilities

### ✅ Project Management
- Project creation and listing
- Document organization by project
- Project-specific insights

## Configuration

### Frontend (.env)
```
REACT_APP_BACKEND_URL=http://localhost:8001
```

### Backend (application.yml)
```yaml
server:
  port: 8001
  
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/knowledge_navigator
      
ai:
  service:
    url: http://localhost:8002
```

## Enhanced Features

### New Backend Capabilities
1. **Three-Stage Processing Pipeline**: Data ingestion → Preprocessing → Knowledge base indexing
2. **Enhanced Q&A Flow**: User authentication → Query preprocessing → Vector search → Answer generation
3. **Processing Status Tracking**: Real-time status updates through pipeline stages
4. **Security Integration**: User permissions and document access control
5. **Vector Search**: Semantic similarity search for better Q&A results

### Frontend Enhancements
- All existing UI components preserved
- Real-time processing status updates
- Enhanced error handling for new backend responses
- Improved Q&A interface with source attribution

## Deployment

The system maintains the same deployment process:

```bash
# Windows
scripts\deploy.bat

# Linux/macOS  
./scripts/deploy.sh
```

All services (MongoDB, Ollama, Java Backend, AI Service, React Frontend) work together seamlessly with the existing Docker Compose configuration.