@echo off
echo ===================================
echo Flashcard App Docker Rebuild Script
echo ===================================

echo.
echo Step 1: Building the Java application with Maven...
call mvn clean package -DskipTests

echo.
echo Step 2: Stopping existing Docker containers...
docker-compose down

echo.
echo Step 3: Removing old Docker images...
FOR /F "tokens=*" %%i IN ('docker images -q flashcard-external-auth-backend-java_app') DO (
    docker rmi %%i
)

echo.
echo Step 4: Rebuilding and starting Docker containers...
docker-compose build --no-cache
docker-compose up -d

echo.
echo ===================================
echo Docker rebuild complete!
echo The application is now running at http://localhost:8080
echo ===================================