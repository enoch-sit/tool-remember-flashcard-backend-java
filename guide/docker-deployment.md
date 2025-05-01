# Docker Deployment Guide

This guide provides instructions for deploying the Flashcard Application using Docker and Docker Compose.

## Prerequisites

- Docker 20.10 or newer
- Docker Compose 2.0 or newer

## Deployment Overview

The Flashcard Application is containerized using Docker with a multi-container setup:

- A Java Spring Boot application container
- A PostgreSQL database container
- A MailHog container for email testing

Docker Compose is used to orchestrate these containers, manage networking, and handle volume persistence.

## Docker Compose Configuration

The application uses a `docker-compose.yml` file to define the container configuration:

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:3000"  # Map to the server.port in application.properties
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/flashcard_db
      # Other environment variables including email config
    depends_on:
      - db
      - mailhog
    networks:
      - flashcard-network

  db:
    image: postgres:15-alpine
    # Database configuration
    # ...
    
  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"  # SMTP port
      - "8025:8025"  # Web UI port
    networks:
      - flashcard-network
```

## Deployment Steps

### 1. Build and Start Containers

To deploy the application for the first time:

```bash
docker-compose up -d
```

This command:

- Builds the application container using the Dockerfile
- Pulls the PostgreSQL image
- Pulls the MailHog image
- Creates and starts all containers in detached mode
- Creates necessary networks and volumes

The application will be available at <http://localhost:8080>.

### 2. View Container Logs

To see the logs from the running containers:

```bash
docker-compose logs -f
```

For just the application container:

```bash
docker-compose logs -f app
```

### 3. Stop and Remove Containers

To stop and remove the containers while preserving volumes:

```bash
docker-compose down
```

To stop and remove everything including volumes:

```bash
docker-compose down -v
```

## Rebuilding the Docker Environment

The project includes three scripts to rebuild the Docker environment when code changes are made:

### Using the Windows Batch Script (.bat)

```bash
# Run in Command Prompt
rebuild-docker.bat
```

### Using the Unix Shell Script (.sh)

```bash
# Make it executable first (on Unix systems)
chmod +x rebuild-docker.sh
./rebuild-docker.sh
```

### Using the PowerShell Script (.ps1)

```powershell
# Run in PowerShell
.\rebuild-docker.ps1
```

These scripts perform the following steps:

1. Build the Java application using Maven
2. Stop existing Docker containers
3. Remove old Docker images
4. Rebuild the Docker images with no cache
5. Start the containers in detached mode

## Environment Variables

The Docker Compose configuration includes several important environment variables:

### Application Container

| Variable | Description | Default Value |
|----------|-------------|---------------|
| SPRING_DATASOURCE_URL | Database connection URL | jdbc:postgresql://db:5432/flashcard_db |
| SPRING_DATASOURCE_USERNAME | Database username | postgres |
| SPRING_DATASOURCE_PASSWORD | Database password | postgres |
| JWT_SECRET | Base64 encoded secret for JWT tokens | [Encoded value] |

### Database Container

| Variable | Description | Default Value |
|----------|-------------|---------------|
| POSTGRES_DB | Database name | flashcard_db |
| POSTGRES_USER | Database username | postgres |
| POSTGRES_PASSWORD | Database password | postgres |

## Using MailHog for Email Testing

The application is configured to use MailHog for handling emails in the development environment. This allows you to test email verification and notification features without actually sending emails.

### MailHog Features

- Captures all outgoing emails from the application
- Provides a web interface to view and inspect emails
- Simulates an SMTP server for testing

### Accessing MailHog

Once the Docker containers are running:

1. Open your browser and navigate to <http://localhost:8025>
2. The web interface displays all emails sent by the application
3. You can view email content, headers, and HTML/plain text versions

### Email Configuration

The application is configured to send emails to MailHog with these settings:

```
SMTP Host: mailhog (Docker internal hostname)
SMTP Port: 1025
Authentication: Disabled (not needed in development)
```

### Testing Email Verification

1. Register a new user in the application
2. Check MailHog's web interface at <http://localhost:8025> to see the verification email
3. Open the email and click the verification link or copy the verification token
4. Complete the verification process in the application

## Security Notes

The JWT secret in the Docker Compose file is Base64 encoded. For production deployments, you should:

1. Generate a strong, random secret
2. Encode it with Base64
3. Store it securely (not in the repository)
4. Pass it as an environment variable or through a secrets manager

## Troubleshooting

### Container Won't Start

If the application container fails to start:

```bash
docker-compose logs app
```

Look for error messages that might indicate the cause.

### Database Connection Issues

If the application can't connect to the database:

1. Ensure the database container is running: `docker-compose ps`
2. Check that the database is properly initialized: `docker-compose logs db`
3. Verify the connection string and credentials in the docker-compose.yml file

### Persistence Issues

If data is not being persisted between container restarts:

1. Check that the volume is properly mounted: `docker volume ls`
2. Inspect the volume: `docker volume inspect flashcard-external-auth-backend-java_postgres_data`
