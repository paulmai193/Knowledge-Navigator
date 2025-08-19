#!/bin/bash
echo "Setting up Ollama with required models..."

# Pull the default model
docker exec knowledge-navigator-ollama ollama pull llama3.2

echo "Ollama setup complete!"
echo "Available at: http://localhost:11434"