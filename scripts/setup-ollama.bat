@echo off
echo Setting up Ollama with required models...

REM Load environment variables
for /f "tokens=1,2 delims==" %%a in ('type .env ^| findstr OLLAMA_') do (
    set %%a=%%b
)

REM Pull configured models
echo Pulling chat model: %OLLAMA_CHAT_MODEL%
docker exec knowledge-navigator-ollama ollama pull %OLLAMA_CHAT_MODEL%

echo Pulling embedding model: %OLLAMA_EMBEDDING_MODEL%
docker exec knowledge-navigator-ollama ollama pull %OLLAMA_EMBEDDING_MODEL%

echo Pulling insight model: %OLLAMA_INSIGHT_MODEL%
docker exec knowledge-navigator-ollama ollama pull %OLLAMA_INSIGHT_MODEL%

echo Pulling QnA model: %OLLAMA_QA_MODEL%
docker exec knowledge-navigator-ollama ollama pull %OLLAMA_QA_MODEL%

echo Ollama setup complete!
echo Available at: http://localhost:11434
echo Models configured:
echo - Chat: %OLLAMA_CHAT_MODEL%
echo - Embedding: %OLLAMA_EMBEDDING_MODEL%
echo - Insights: %OLLAMA_INSIGHT_MODEL%
echo - Q&A: %OLLAMA_QA_MODEL%