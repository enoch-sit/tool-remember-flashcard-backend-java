# Installation Guide

This guide provides step-by-step instructions for setting up the Flashcard Application in various environments.

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- PostgreSQL 15 (for production) or H2 (for development)
- Docker and Docker Compose (for containerized deployment)

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd flashcard-external-auth-backend-java
```

### 2. Configure the Application

The application can be configured using the `application.properties` file located in `src/main/resources`.

For development, you can use the default H2 in-memory database:

```properties
# Database configuration
spring.datasource.url=jdbc:h2:mem:flashcarddb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

For a production environment with PostgreSQL:

```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/flashcard_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 3. Build the Application

```bash
mvn clean package
```

For building without running tests:

```bash
mvn clean package -DskipTests
```

### 4. Run the Application

```bash
java -jar target/flashcard-app.jar
```

The application will be available at <http://localhost:3000> by default.

## Docker Deployment

For a containerized deployment, refer to the [Docker Deployment Guide](docker-deployment.md).

## Testing the Installation

After installation, you can verify that the application is running correctly by accessing the health endpoint:

```
GET http://localhost:3000/health
```

Expected response:

```json
{
  "status": "ok"
}
```

## Next Steps

- Create a user account through the `/api/auth/signup` endpoint
- Explore the API using the documentation provided in the [API Guide](api-documentation.md)
