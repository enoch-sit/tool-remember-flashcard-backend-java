Write-Host "===================================" -ForegroundColor Green
Write-Host "Flashcard App Docker Rebuild Script" -ForegroundColor Green
Write-Host "===================================" -ForegroundColor Green

Write-Host "`nStep 1: Building the Java application with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

Write-Host "`nStep 2: Stopping existing Docker containers..." -ForegroundColor Yellow
docker-compose down

Write-Host "`nStep 3: Removing old Docker images..." -ForegroundColor Yellow
$oldImages = docker images -q flashcard-external-auth-backend-java_app
if ($oldImages) {
    docker rmi $oldImages
}

Write-Host "`nStep 4: Rebuilding and starting Docker containers..." -ForegroundColor Yellow
docker-compose build --no-cache
docker-compose up -d

Write-Host "`n===================================" -ForegroundColor Green
Write-Host "Docker rebuild complete!" -ForegroundColor Green
Write-Host "The application is now running at http://localhost:8080" -ForegroundColor Green
Write-Host "===================================" -ForegroundColor Green