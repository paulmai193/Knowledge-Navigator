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
import logging
from langdetect import detect
from langdetect.lang_detect_exception import LangDetectException
from googletrans import Translator

# Configure logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

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

# Ollama Models Configuration
OLLAMA_EMBEDDING_MODEL = os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text")
OLLAMA_INSIGHT_MODEL = os.getenv("OLLAMA_INSIGHT_MODEL", "llama3.2")
OLLAMA_QA_MODEL = os.getenv("OLLAMA_QA_MODEL", "llama3.2")

client = AsyncIOMotorClient(MONGO_URL)
db = client.knowledge_navigator

# Initialize Google Translator
translator = Translator()



class ProcessRequest(BaseModel):
    document_id: str
    file_path: str
    content_type: str

class QARequest(BaseModel):
    question: str
    context: str = ""
    user_id: str = "anonymous"

class EmbeddingRequest(BaseModel):
    text: str

class ModelConfig(BaseModel):
    chat_model: str = None
    embedding_model: str = None
    insight_model: str = None
    qa_model: str = None

class LanguageDetectionRequest(BaseModel):
    content: str

class TranslationRequest(BaseModel):
    text: str
    target_languages: List[str]

class QueryExpansionRequest(BaseModel):
    query: str

@app.post("/process")
async def process_document(request: ProcessRequest):
    """Process document with AI analysis"""
    logger.info(f"Processing document: {request.document_id}, type: {request.content_type}")
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
        
        logger.info(f"Successfully processed document {request.document_id} with {len(insights)} insights")
        return {"status": "success", "insights_count": len(insights)}
    except Exception as e:
        logger.error(f"Error processing document {request.document_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/qa")
async def answer_question(request: dict):
    """Answer questions using AI with context"""
    logger.info(f"Processing Q&A request with context length: {len(request.get('context', ''))}")
    try:
        question = request.get("question")
        context = request.get("context", "")
        is_creative = request.get("is_creative", False)
        logger.debug(f"Question: {question[:100]}...")
        
        # Generate answer using LLM with provided context
        answer = await generate_answer_with_context(question, is_creative, context)
        
        # Log the Q&A interaction
        qa_record = {
            "_id": str(uuid.uuid4()),
            "question": question,
            "answer": answer,
            "context_length": len(context),
            "created_date": datetime.now()
        }
        await db.qa_history.insert_one(qa_record)
        
        logger.info(f"Successfully generated answer for Q&A session: {qa_record['_id']}")
        return {"answer": answer, "qa_id": qa_record["_id"]}
    except Exception as e:
        logger.error(f"Error in Q&A processing: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/embedding")
async def create_embedding(request: EmbeddingRequest):
    """Generate text embeddings"""
    logger.debug(f"Generating embedding for text length: {len(request.text)}")
    try:
        embedding = await generate_text_embedding(request.text)
        logger.debug(f"Generated embedding with dimension: {len(embedding)}")
        return {"embedding": embedding}
    except Exception as e:
        logger.error(f"Error generating embedding: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/insights")
async def generate_document_insights(request: dict):
    """Generate insights for document content"""
    logger.info(f"Generating insights for document: {request.get('document_id')}")
    try:
        content = request.get("content", "")
        insights = await generate_insights(content)
        logger.info(f"Generated {len(insights)} insights")
        return {"insights": insights}
    except Exception as e:
        logger.error(f"Error generating insights: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/detect-language")
async def detect_language(request: dict):
    """Detect language of document content"""
    logger.info(f"Detecting language for content length: {len(request.get('content', ''))}")
    try:
        content = request.get("content", "")
        language = await detect_document_language(content)
        logger.info(f"Detected language: {language}")
        return {"language": language}
    except Exception as e:
        logger.error(f"Error detecting language: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/translate")
async def translate_text(request: dict):
    """Translate text to multiple languages"""
    logger.info(f"Translating text to {len(request.get('target_languages', []))} languages")
    try:
        text = request.get("text", "")
        target_languages = request.get("target_languages", [])
        
        translations = await translate_to_languages(text, target_languages)
        logger.info(f"Generated {len(translations)} translations")
        return {"translations": translations}
    except Exception as e:
        logger.error(f"Error translating text: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# @app.post("/expand-query")
# async def expand_query(request: dict):
#     """Expand query with keywords and alternatives"""
#     logger.info(f"Expanding query: {request.get('query', '')[:50]}...")
#     try:
#         query = request.get("query", "")
#         expanded = await expand_query_with_ai(query)
#         logger.info(f"Generated rephrased query and {len(expanded.get('keywords', []))} keywords")
#         return expanded
#     except Exception as e:
#         logger.error(f"Error expanding query: {str(e)}")
#         raise HTTPException(status_code=500, detail=str(e))

async def extract_text_content(file_path: str, content_type: str) -> str:
    """Extract text from uploaded files"""
    try:
        if content_type == "text/plain":
            with open(file_path, 'r', encoding='utf-8') as f:
                return f.read()
        elif content_type == "application/pdf":
            # Placeholder for PDF extraction
            return "PDF content extraction - implement with PyPDF2 or similar"
        elif content_type in ["application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"]:
            # Placeholder for DOCX extraction
            return "DOCX content extraction - implement with python-docx"
        else:
            return f"Content extraction not implemented for: {content_type}"
    except Exception as e:
        return f"Error extracting content: {str(e)}"

async def generate_insights(content: str) -> List[Dict[str, Any]]:
    """Generate insights using LLM"""
    logger.debug(f"Generating insights for content length: {len(content)}")
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
    
    logger.debug(f"Calling Ollama with model: {OLLAMA_INSIGHT_MODEL}")
    async with aiohttp.ClientSession() as session:
        payload = {
            "model": OLLAMA_INSIGHT_MODEL,
            "prompt": prompt,
            "stream": False
        }
        
        async with session.post(f"{OLLAMA_URL}/api/generate", json=payload) as resp:
            logger.debug(f"Ollama insights response status: {resp.status}")
            result = await resp.json()
            response = result.get("response", "")
            logger.debug(f"Raw Ollama response length: {len(response)}")
            
            try:
                json_start = response.find('{')
                json_end = response.rfind('}') + 1
                json_str = response[json_start:json_end]
                logger.debug(f"Extracted JSON: {json_str[:200]}...")
                parsed = json.loads(json_str)
                insights = parsed.get("insights", [])
                logger.info(f"Successfully parsed {len(insights)} insights")
                return insights
            except Exception as e:
                logger.warning(f"Failed to parse JSON insights: {str(e)}, using fallback")
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

async def generate_answer_with_context(question: str, is_creative: bool, context: str) -> str:
    """Generate answer using LLM with specific context"""
    logger.debug(f"Generating answer for question length: {len(question)}, context length: {len(context)}")
    
    # Detect if this is a creative task
    logger.debug(f"Creative task detected: {is_creative}")
    
    if is_creative:
        prompt = f"""
        You are a creative assistant that generates content based on provided context.
        
        Question: {question}
        
        Context: {context}
        
        Instructions:
        - Use the context as inspiration and reference material
        - Generate creative, helpful content that addresses the request
        - Be innovative while staying relevant to the context
        - Provide practical, actionable suggestions
        - Feel free to expand beyond the context when it helps fulfill the creative request
        
        Creative Response:
        """
        temperature = 0.9
        top_p = 0.95
    else:
        prompt = f"""
        You are a helpful assistant that answers questions based on provided context.
        
        Question: {question}
        
        Context: {context}
        
        Instructions:
        - Answer the question based only on the provided context
        - If the context doesn't contain relevant information, say so
        - Be concise and accurate
        - Cite specific parts of the context when possible
        
        Answer:
        """
        temperature = 0.7
        top_p = 0.9

    logger.debug(f"Using model: {OLLAMA_QA_MODEL}, temperature: {temperature}, top_p: {top_p}")
    async with aiohttp.ClientSession() as session:
        payload = {
            "model": OLLAMA_QA_MODEL,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": temperature,
                "top_p": top_p
            }
        }
        
        async with session.post(f"{OLLAMA_URL}/api/generate", json=payload) as resp:
            logger.debug(f"Ollama Q&A response status: {resp.status}")
            result = await resp.json()
            answer = result.get("response", "Unable to generate answer")
            logger.debug(f"Generated answer length: {len(answer)}")
            return answer

async def generate_text_embedding(text: str) -> List[float]:
    """Generate text embeddings using Ollama"""
    logger.debug(f"Generating embedding for text length: {len(text)}")
    try:
        async with aiohttp.ClientSession() as session:
            payload = {
                "model": OLLAMA_EMBEDDING_MODEL,
                "prompt": text
            }
            
            logger.debug(f"Calling Ollama embeddings API: {OLLAMA_URL}/api/embeddings")
            async with session.post(f"{OLLAMA_URL}/api/embeddings", json=payload) as resp:
                logger.debug(f"Ollama embeddings response status: {resp.status}")
                if resp.status == 200:
                    result = await resp.json()
                    embedding = result.get("embedding", [0.0] * 384)
                    logger.debug(f"Generated embedding with dimension: {len(embedding)}")
                    return embedding
                else:
                    logger.warning(f"Ollama embeddings failed with status {resp.status}, using dummy embedding")
                    return [0.0] * 384
    except Exception as e:
        logger.error(f"Error generating embedding: {str(e)}")
        return [0.0] * 384

async def detect_document_language(content: str) -> str:
    """Detect language of document content using langdetect"""
    try:
        # Clean content for better detection
        clean_content = content.replace('\n', ' ').strip()
        if len(clean_content) < 10:
            return "en"
        
        # Detect language
        language_code = detect(clean_content)
        logger.debug(f"Detected language: {language_code}")
        
        return language_code
        
    except LangDetectException as e:
        logger.warning(f"Language detection failed: {str(e)}, defaulting to 'en'")
        return "en"
    except Exception as e:
        logger.error(f"Error detecting language: {str(e)}")
        return "en"

async def translate_to_languages(text: str, target_languages: List[str]) -> Dict[str, str]:
    """Translate text to multiple target languages using argos-translate"""
    translations = {}
    
    for lang in target_languages:
        try:
            translation = translate_with_googletrans(text, lang)
            translations[lang] = translation
        except Exception as e:
            logger.error(f"Error translating to {lang}: {str(e)}")
            translations[lang] = text  # Fallback to original text
    
    return translations

def translate_with_googletrans(text: str, target_language: str) -> str:
    """Translate text using googletrans"""
    logger.debug(f"Translating text (length: {len(text)}) to {target_language}")
    try:
        # Skip translation if text is too short
        if len(text.strip()) < 3:
            logger.debug("Text too short, skipping translation")
            return text
            
        # Translate using googletrans
        logger.debug(f"Calling Google Translate API for {target_language}")
        result = translator.translate(text, dest=target_language)
        translated = result.text
        
        logger.debug(f"Successfully translated from {result.src} to {target_language} (length: {len(translated)})")
        return translated if translated else text
        
    except Exception as e:
        logger.error(f"Google translation error for {target_language}: {str(e)}")
        return text

# async def expand_query_with_ai(query: str) -> Dict[str, Any]:
#     """Expand query with AI-generated keywords and alternatives"""
#     logger.debug(f"Expanding query: '{query}' (length: {len(query)})")
#     prompt = f"""
#     Analyze this query and detect if it's a creative task (naming, suggesting, creating, designing, generating examples, templates, structures, functions, methods, patterns, solutions, etc.).
    
#     Original Query: {query}
    
#     Return JSON format without any explanation or extra text:
#     {{
#         "keywords": ["keyword1", "keyword2", "keyword3"],
#         "rephrased_query": "single rephrased version of the question",
#         "is_creative": true|false,
#         "expanded_query": "comprehensive expanded version"
#     }}
    
#     If creative task (is_creative: true), focus on:
#     - Naming conventions and terminology
#     - Structural patterns and architectural concepts
#     - Implementation approaches and methodologies
#     - Examples, templates, and best practices
#     - Creative solutions and innovative techniques
    
#     If informational task (is_creative: false), focus on:
#     - Synonyms and related terms
#     - Different ways to phrase the same question
#     - Technical and common terminology
#     - Broader and narrower concepts
#     """
    
#     try:
#         logger.debug(f"Calling Ollama for query expansion with model: {OLLAMA_QA_MODEL}")
#         async with aiohttp.ClientSession() as session:
#             payload = {
#                 "model": OLLAMA_QA_MODEL,
#                 "prompt": prompt,
#                 "stream": False,
#                 "options": {
#                     "temperature": 0.8,
#                     "top_p": 0.9
#                 }
#             }
            
#             async with session.post(f"{OLLAMA_URL}/api/generate", json=payload) as resp:
#                 logger.debug(f"Ollama expansion response status: {resp.status}")
#                 result = await resp.json()
#                 response = result.get("response", "")
#                 logger.debug(f"Raw expansion response length: {len(response)}")
                
#                 try:
#                     json_start = response.find('{')
#                     json_end = response.rfind('}') + 1
#                     json_str = response[json_start:json_end]
#                     logger.debug(f"Extracted expansion JSON: {json_str}")
#                     parsed = json.loads(json_str)
                    
#                     result_data = {
#                         "keywords": parsed.get("keywords", []),
#                         "rephrased_query": parsed.get("rephrased_query", query),
#                         "is_creative": parsed.get("is_creative", False),
#                         "expanded_query": parsed.get("expanded_query", query)
#                     }
#                     logger.info(f"Successfully expanded query: {len(result_data['keywords'])} keywords, {len(result_data['rephrased_query'])} rephrased_query")
#                     return result_data
#                 except Exception as parse_error:
#                     logger.warning(f"Failed to parse expansion JSON: {str(parse_error)}, using fallback")
#                     # Fallback: extract keywords from response
#                     words = query.split()
#                     return {
#                         "keywords": words,
#                         "rephrased_query": query,
#                         "is_creative": False,
#                         "expanded_query": query
#                     }
#     except Exception as e:
#         logger.error(f"Error expanding query '{query}': {str(e)}")
#         return {
#             "keywords": query.split(),
#             "rephrased_query": query,
#             "is_creative": False,
#             "expanded_query": query
#         }

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "AI/NLP Service"}

@app.get("/config/models")
async def get_model_config():
    """Get current model configuration"""
    return {
        "embedding_model": OLLAMA_EMBEDDING_MODEL,
        "insight_model": OLLAMA_INSIGHT_MODEL,
        "qa_model": OLLAMA_QA_MODEL,
        "ollama_url": OLLAMA_URL
    }

@app.post("/config/models")
async def update_model_config(config: dict):
    """Update model configuration at runtime"""
    global OLLAMA_EMBEDDING_MODEL, OLLAMA_INSIGHT_MODEL, OLLAMA_QA_MODEL
    
    if "embedding_model" in config:
        OLLAMA_EMBEDDING_MODEL = config["embedding_model"]
    if "insight_model" in config:
        OLLAMA_INSIGHT_MODEL = config["insight_model"]
    if "qa_model" in config:
        OLLAMA_QA_MODEL = config["qa_model"]
    
    return {
        "status": "updated",
        "current_config": {
            "embedding_model": OLLAMA_EMBEDDING_MODEL,
            "insight_model": OLLAMA_INSIGHT_MODEL,
            "qa_model": OLLAMA_QA_MODEL
        }
    }

@app.get("/models/available")
async def list_available_models():
    """List available models from Ollama"""
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(f"{OLLAMA_URL}/api/tags") as resp:
                if resp.status == 200:
                    result = await resp.json()
                    models = [model["name"] for model in result.get("models", [])]
                    return {"available_models": models}
                else:
                    return {"error": "Failed to fetch models from Ollama"}
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)