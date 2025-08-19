@echo off
echo Starting Knowledge Navigator in development mode...

REM Stop existing containers
docker-compose -f docker-compose.dev.yml down

REM Build and start development services
docker-compose -f docker-compose.dev.yml up --build -d

echo Development environment started!
echo Frontend: http://localhost:3000
echo Backend API: http://localhost:8001
echo MongoDB: localhost:27017