from fastapi import FastAPI, File, UploadFile, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import os
import uuid
import asyncio
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel
from motor.motor_asyncio import AsyncIOMotorClient
from dotenv import load_dotenv
import aiofiles
import mimetypes
import json

# Load environment variables
load_dotenv()

app = FastAPI(title="Knowledge Navigator API")

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# MongoDB connection
MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")
EMERGENT_LLM_KEY = os.getenv("EMERGENT_LLM_KEY")

client = AsyncIOMotorClient(MONGO_URL)
db = client.knowledge_navigator

# Create uploads directory
UPLOAD_DIR = "/app/backend/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Serve uploaded files
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")

# Pydantic models
class DocumentResponse(BaseModel):
    id: str
    filename: str
    content_type: str
    upload_date: datetime
    processed: bool = False
    insights_count: int = 0

class InsightResponse(BaseModel):
    id: str
    document_id: str
    title: str
    content: str
    category: str
    importance_score: float
    created_date: datetime

class ProjectResponse(BaseModel):
    id: str
    name: str
    description: str
    documents: List[DocumentResponse]
    insights: List[InsightResponse]
    recommendations: List[str]
    created_date: datetime

class CreateProjectRequest(BaseModel):
    name: str
    description: str

class QARequest(BaseModel):
    question: str

class QAResponse(BaseModel):
    id: str
    question: str
    answer: str
    referenced_documents: List[str]
    referenced_insights: List[str]
    created_date: datetime

@app.get("/api/health")
async def health_check():
    return {"status": "healthy", "service": "Knowledge Navigator API"}

@app.post("/api/documents/upload")
async def upload_document(file: UploadFile = File(...)):
    """Upload a document for analysis"""
    try:
        # Validate file type
        allowed_types = ["application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"]
        if file.content_type not in allowed_types:
            raise HTTPException(status_code=400, detail="Unsupported file type. Only PDF, DOCX, and TXT files are allowed.")
        
        # Generate unique filename
        file_id = str(uuid.uuid4())
        file_extension = os.path.splitext(file.filename)[1]
        stored_filename = f"{file_id}{file_extension}"
        file_path = os.path.join(UPLOAD_DIR, stored_filename)
        
        # Save file
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        # Store document metadata in database
        document = {
            "_id": file_id,
            "filename": file.filename,
            "stored_filename": stored_filename,
            "file_path": file_path,
            "content_type": file.content_type,
            "upload_date": datetime.now(),
            "processed": False,
            "file_size": len(content)
        }
        
        await db.documents.insert_one(document)
        
        # Start background processing
        asyncio.create_task(process_document(file_id, file_path, file.content_type))
        
        return {
            "message": "Document uploaded successfully",
            "document_id": file_id,
            "filename": file.filename
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error uploading document: {str(e)}")

async def process_document(document_id: str, file_path: str, content_type: str):
    """Process document with LLM to extract insights"""
    try:
        # Import LLM integration
        from emergentintegrations.llm.chat import LlmChat, UserMessage, FileContentWithMimeType
        
        # Initialize Gemini chat
        chat = LlmChat(
            api_key=EMERGENT_LLM_KEY,
            session_id=f"doc_analysis_{document_id}",
            system_message="You are an expert knowledge analyst. Analyze documents to extract key insights, project learnings, best practices, and actionable recommendations for future projects."
        ).with_model("gemini", "gemini-2.0-flash")
        
        # Create file attachment
        file_attachment = FileContentWithMimeType(
            file_path=file_path,
            mime_type=content_type
        )
        
        # Analyze document
        analysis_prompt = """
        Please analyze this document and extract:
        1. Key insights and learnings
        2. Best practices mentioned
        3. Technical solutions and approaches
        4. Project challenges and how they were resolved
        5. Recommendations for future similar projects
        
        Format your response as JSON with the following structure:
        {
            "insights": [
                {
                    "title": "insight title",
                    "content": "detailed content",
                    "category": "technical|process|business|general",
                    "importance_score": 0.8
                }
            ],
            "recommendations": [
                "recommendation 1",
                "recommendation 2"
            ],
            "summary": "Overall document summary"
        }
        """
        
        user_message = UserMessage(
            text=analysis_prompt,
            file_contents=[file_attachment]
        )
        
        response = await chat.send_message(user_message)
        
        # Parse response
        try:
            # Try to extract JSON from response
            response_text = str(response)
            json_start = response_text.find('{')
            json_end = response_text.rfind('}') + 1
            
            if json_start != -1 and json_end > json_start:
                json_str = response_text[json_start:json_end]
                analysis_result = json.loads(json_str)
            else:
                # Fallback: create structured data from text response
                analysis_result = {
                    "insights": [
                        {
                            "title": "AI Analysis Results",
                            "content": response_text,
                            "category": "general",
                            "importance_score": 0.7
                        }
                    ],
                    "recommendations": ["Review the analysis results for actionable insights"],
                    "summary": "Document processed successfully"
                }
        except json.JSONDecodeError:
            # Fallback for non-JSON responses
            analysis_result = {
                "insights": [
                    {
                        "title": "Document Analysis",
                        "content": str(response),
                        "category": "general",
                        "importance_score": 0.7
                    }
                ],
                "recommendations": ["Review the analysis for key takeaways"],
                "summary": "Document analysis completed"
            }
        
        # Store insights in database
        insights = []
        for insight_data in analysis_result.get("insights", []):
            insight = {
                "_id": str(uuid.uuid4()),
                "document_id": document_id,
                "title": insight_data.get("title", "Untitled Insight"),
                "content": insight_data.get("content", ""),
                "category": insight_data.get("category", "general"),
                "importance_score": insight_data.get("importance_score", 0.5),
                "created_date": datetime.now()
            }
            insights.append(insight)
        
        if insights:
            await db.insights.insert_many(insights)
        
        # Update document as processed
        await db.documents.update_one(
            {"_id": document_id},
            {
                "$set": {
                    "processed": True,
                    "analysis_summary": analysis_result.get("summary", ""),
                    "recommendations": analysis_result.get("recommendations", [])
                }
            }
        )
        
        print(f"Successfully processed document {document_id}")
        
    except Exception as e:
        print(f"Error processing document {document_id}: {str(e)}")
        # Mark as processed even if failed to avoid infinite retries
        await db.documents.update_one(
            {"_id": document_id},
            {"$set": {"processed": True, "processing_error": str(e)}}
        )

@app.get("/api/documents")
async def get_documents():
    """Get all uploaded documents"""
    try:
        documents = []
        async for doc in db.documents.find():
            # Count insights for this document
            insights_count = await db.insights.count_documents({"document_id": doc["_id"]})
            
            documents.append({
                "id": doc["_id"],
                "filename": doc["filename"],
                "content_type": doc["content_type"],
                "upload_date": doc["upload_date"],
                "processed": doc.get("processed", False),
                "insights_count": insights_count
            })
        
        return {"documents": documents}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching documents: {str(e)}")

@app.get("/api/insights")
async def get_insights(document_id: Optional[str] = None):
    """Get insights, optionally filtered by document"""
    try:
        query = {}
        if document_id:
            query["document_id"] = document_id
        
        insights = []
        async for insight in db.insights.find(query).sort("importance_score", -1):
            insights.append({
                "id": insight["_id"],
                "document_id": insight["document_id"],
                "title": insight["title"],
                "content": insight["content"],
                "category": insight["category"],
                "importance_score": insight["importance_score"],
                "created_date": insight["created_date"]
            })
        
        return {"insights": insights}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching insights: {str(e)}")

@app.get("/api/projects")
async def get_projects():
    """Get all projects with their insights and recommendations"""
    try:
        projects = []
        async for project in db.projects.find():
            # Get project documents and insights
            project_documents = []
            project_insights = []
            
            for doc_id in project.get("document_ids", []):
                doc = await db.documents.find_one({"_id": doc_id})
                if doc:
                    insights_count = await db.insights.count_documents({"document_id": doc_id})
                    project_documents.append({
                        "id": doc["_id"],
                        "filename": doc["filename"],
                        "content_type": doc["content_type"],
                        "upload_date": doc["upload_date"],
                        "processed": doc.get("processed", False),
                        "insights_count": insights_count
                    })
                
                # Get insights for this document
                async for insight in db.insights.find({"document_id": doc_id}):
                    project_insights.append({
                        "id": insight["_id"],
                        "document_id": insight["document_id"],
                        "title": insight["title"],
                        "content": insight["content"],
                        "category": insight["category"],
                        "importance_score": insight["importance_score"],
                        "created_date": insight["created_date"]
                    })
            
            projects.append({
                "id": project["_id"],
                "name": project["name"],
                "description": project["description"],
                "documents": project_documents,
                "insights": project_insights,
                "recommendations": project.get("recommendations", []),
                "created_date": project["created_date"]
            })
        
        return {"projects": projects}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching projects: {str(e)}")

@app.post("/api/projects")
async def create_project(request: CreateProjectRequest):
    """Create a new project"""
    try:
        project = {
            "_id": str(uuid.uuid4()),
            "name": request.name,
            "description": request.description,
            "document_ids": [],
            "recommendations": [],
            "created_date": datetime.now()
        }
        
        await db.projects.insert_one(project)
        
        return {
            "message": "Project created successfully",
            "project_id": project["_id"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating project: {str(e)}")

@app.get("/api/dashboard/stats")
async def get_dashboard_stats():
    """Get dashboard statistics"""
    try:
        total_documents = await db.documents.count_documents({})
        processed_documents = await db.documents.count_documents({"processed": True})
        total_insights = await db.insights.count_documents({})
        total_projects = await db.projects.count_documents({})
        total_qa_sessions = await db.qa_history.count_documents({})
        
        # Get top categories
        pipeline = [
            {"$group": {"_id": "$category", "count": {"$sum": 1}}},
            {"$sort": {"count": -1}},
            {"$limit": 5}
        ]
        
        top_categories = []
        async for result in db.insights.aggregate(pipeline):
            top_categories.append({
                "category": result["_id"],
                "count": result["count"]
            })
        
        return {
            "total_documents": total_documents,
            "processed_documents": processed_documents,
            "total_insights": total_insights,
            "total_projects": total_projects,
            "total_qa_sessions": total_qa_sessions,
            "processing_rate": (processed_documents / total_documents * 100) if total_documents > 0 else 0,
            "top_categories": top_categories
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching dashboard stats: {str(e)}")

@app.post("/api/qa/ask")
async def ask_question(request: QARequest):
    """Ask a question and get an AI-powered answer based on uploaded documents and insights"""
    try:
        question = request.question.strip()
        if not question:
            raise HTTPException(status_code=400, detail="Question cannot be empty")
        
        # Get all processed documents and insights to provide context
        documents = []
        insights = []
        
        async for doc in db.documents.find({"processed": True}):
            documents.append({
                "id": doc["_id"],
                "filename": doc["filename"],
                "content_type": doc["content_type"]
            })
        
        async for insight in db.insights.find():
            insights.append({
                "id": insight["_id"],
                "title": insight["title"],
                "content": insight["content"],
                "category": insight["category"],
                "importance_score": insight["importance_score"]
            })
        
        if not documents and not insights:
            return {
                "id": str(uuid.uuid4()),
                "question": question,
                "answer": "I don't have any documents or insights to reference yet. Please upload some documents first so I can help answer your questions based on your knowledge base.",
                "referenced_documents": [],
                "referenced_insights": [],
                "created_date": datetime.now()
            }
        
        # Prepare context for the AI
        context_parts = []
        
        if insights:
            context_parts.append("Available insights from your documents:")
            for insight in insights[:10]:  # Limit to top 10 insights
                context_parts.append(f"- {insight['title']}: {insight['content'][:200]}...")
        
        if documents:
            context_parts.append(f"\nAvailable documents: {', '.join([doc['filename'] for doc in documents])}")
        
        context = "\n".join(context_parts)
        
        # Import LLM integration
        from emergentintegrations.llm.chat import LlmChat, UserMessage
        
        # Initialize Gemini chat for Q&A
        chat = LlmChat(
            api_key=EMERGENT_LLM_KEY,
            session_id=f"qa_session_{str(uuid.uuid4())}",
            system_message="You are a helpful knowledge assistant. Answer questions based on the provided context from the user's document library. If you reference specific insights or information, mention which documents or insights you're drawing from. Be concise but informative."
        ).with_model("gemini", "gemini-2.0-flash")
        
        # Create the prompt with context
        prompt = f"""
        Based on the following knowledge from the user's document library, please answer this question:
        
        Question: {question}
        
        Context:
        {context}
        
        Please provide a helpful answer and mention which insights or documents you're referencing if applicable.
        """
        
        user_message = UserMessage(text=prompt)
        
        # Get AI response
        response = await chat.send_message(user_message)
        answer = str(response)
        
        # Simple logic to identify referenced documents and insights
        referenced_documents = []
        referenced_insights = []
        
        # Check if any document filenames are mentioned in the answer
        for doc in documents:
            if doc["filename"].lower() in answer.lower():
                referenced_documents.append(doc["id"])
        
        # Check if any insight titles are mentioned in the answer
        for insight in insights:
            if any(word in answer.lower() for word in insight["title"].lower().split() if len(word) > 3):
                referenced_insights.append(insight["id"])
        
        # Create Q&A record
        qa_record = {
            "_id": str(uuid.uuid4()),
            "question": question,
            "answer": answer,
            "referenced_documents": referenced_documents,
            "referenced_insights": referenced_insights,
            "created_date": datetime.now()
        }
        
        # Store in database
        await db.qa_history.insert_one(qa_record)
        
        return {
            "id": qa_record["_id"],
            "question": question,
            "answer": answer,
            "referenced_documents": referenced_documents,
            "referenced_insights": referenced_insights,
            "created_date": qa_record["created_date"]
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing question: {str(e)}")

@app.get("/api/qa/history")
async def get_qa_history():
    """Get Q&A history"""
    try:
        qa_history = []
        async for qa in db.qa_history.find().sort("created_date", -1).limit(50):
            qa_history.append({
                "id": qa["_id"],
                "question": qa["question"],
                "answer": qa["answer"],
                "referenced_documents": qa.get("referenced_documents", []),
                "referenced_insights": qa.get("referenced_insights", []),
                "created_date": qa["created_date"]
            })
        
        return {"qa_history": qa_history}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching Q&A history: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)