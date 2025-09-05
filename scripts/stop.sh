#!/bin/bash
echo "Stopping Knowledge Navigator services..."

# Stop production containers
docker-compose down

# Stop development containers
docker-compose -f docker-compose.dev.yml down

echo "All services stopped!"