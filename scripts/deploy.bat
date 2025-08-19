@echo off
echo Starting Knowledge Navigator deployment...

REM Stop existing containers
docker-compose down

REM Build and start services
docker-compose up --build -d

echo Deployment complete!
echo Frontend: http://localhost:3000
echo Backend API: http://localhost:8001
echo MongoDB: localhost:27017

REM Show running containers
docker-compose ps