@echo off
echo Setting up Ollama with required models...

REM Pull the default model
docker exec knowledge-navigator-ollama ollama pull llama3.2

echo Ollama setup complete!
echo Available at: http://localhost:11434