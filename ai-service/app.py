from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import os
import json
import aiohttp
import asyncio
from motor.motor_asyncio import AsyncIOMotorClient
from datetime import datetime
import uuid
from typing import List, Dict, Any

app = FastAPI(title="AI/NLP Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")

client = AsyncIOMotorClient(MONGO_URL)
db = client.knowledge_navigator

class ProcessRequest(BaseModel):
    document_id: str
    file_path: str
    content_type: str

class QARequest(BaseModel):
    question: str

class EmbeddingRequest(BaseModel):
    text: str

@app.post("/process")
async def process_document(request: ProcessRequest):
    """Process document with AI analysis"""
    try:
        # Extract text content
        content = await extract_text_content(request.file_path, request.content_type)
        
        # Generate insights
        insights = await generate_insights(content)
        
        # Store insights in database
        await store_insights(request.document_id, insights)
        
        # Generate embeddings for search
        await generate_embeddings(request.document_id, content)
        
        # Update document status
        await db.documents.update_one(
            {"_id": request.document_id},
            {"$set": {"processed": True}}
        )
        
        return {"status": "success", "insights_count": len(insights)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/qa")
async def answer_question(request: QARequest):
    """Answer questions using AI"""
    try:
        # Get relevant context from embeddings
        context = await get_relevant_context(request.question)
        
        # Generate answer using LLM
        answer = await generate_answer(request.question, context)
        
        # Store Q&A record
        qa_record = {
            "_id": str(uuid.uuid4()),
            "question": request.question,
            "answer": answer,
            "created_date": datetime.now()
        }
        await db.qa_history.insert_one(qa_record)
        
        return {"answer": answer, "qa_id": qa_record["_id"]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/embedding")
async def create_embedding(request: EmbeddingRequest):
    """Generate text embeddings"""
    try:
        embedding = await generate_text_embedding(request.text)
        return {"embedding": embedding}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

async def extract_text_content(file_path: str, content_type: str) -> str:
    """Extract text from uploaded files"""
    if content_type == "text/plain":
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    else:
        # Basic implementation - extend for PDF/DOCX
        return "Content extraction not implemented for this file type"

async def generate_insights(content: str) -> List[Dict[str, Any]]:
    """Generate insights using LLM"""
    prompt = f"""
    Analyze this document and extract key insights. Return JSON format:
    {{
        "insights": [
            {{
                "title": "insight title",
                "content": "detailed content", 
                "category": "technical|process|business|general",
                "importance_score": 0.8
            }}
        ]
    }}
    
    Document: {content[:4000]}
    """
    
    async with aiohttp.ClientSession() as session:
        payload = {
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False
        }
        
        async with session.post(f"{OLLAMA_URL}/api/generate", json=payload) as resp:
            result = await resp.json()
            response = result.get("response", "")
            
            try:
                json_start = response.find('{')
                json_end = response.rfind('}') + 1
                json_str = response[json_start:json_end]
                parsed = json.loads(json_str)
                return parsed.get("insights", [])
            except:
                return [{
                    "title": "AI Analysis",
                    "content": response,
                    "category": "general",
                    "importance_score": 0.7
                }]

async def store_insights(document_id: str, insights: List[Dict[str, Any]]):
    """Store insights in database"""
    insight_docs = []
    for insight in insights:
        insight_doc = {
            "_id": str(uuid.uuid4()),
            "document_id": document_id,
            "title": insight.get("title", ""),
            "content": insight.get("content", ""),
            "category": insight.get("category", "general"),
            "importance_score": insight.get("importance_score", 0.5),
            "created_date": datetime.now()
        }
        insight_docs.append(insight_doc)
    
    if insight_docs:
        await db.insights.insert_many(insight_docs)

async def generate_embeddings(document_id: str, content: str):
    """Generate and store embeddings for search"""
    # Placeholder for embedding generation
    # In production, use sentence-transformers or similar
    pass

async def get_relevant_context(question: str) -> str:
    """Get relevant context for Q&A"""
    # Get recent insights as context
    insights = []
    async for insight in db.insights.find().limit(5):
        insights.append(f"{insight['title']}: {insight['content']}")
    
    return "\n".join(insights)

async def generate_answer(question: str, context: str) -> str:
    """Generate answer using LLM"""
    prompt = f"""
    Answer this question based on the provided context:
    
    Question: {question}
    
    Context: {context}
    
    Provide a helpful answer based on the context.
    """
    
    async with aiohttp.ClientSession() as session:
        payload = {
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False
        }
        
        async with session.post(f"{OLLAMA_URL}/api/generate", json=payload) as resp:
            result = await resp.json()
            return result.get("response", "Unable to generate answer")

async def generate_text_embedding(text: str) -> List[float]:
    """Generate text embeddings"""
    # Placeholder - implement with actual embedding model
    return [0.0] * 384

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "AI/NLP Service"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)