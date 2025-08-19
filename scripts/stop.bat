@echo off
echo Stopping Knowledge Navigator services...

REM Stop production containers
docker-compose down

REM Stop development containers
docker-compose -f docker-compose.dev.yml down

echo All services stopped!