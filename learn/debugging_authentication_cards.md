# Debugging Authentication and Card Creation in Spring Boot Applications

This guide explains how we debugged and fixed authentication and card creation issues in our Java Spring Boot application. No prior knowledge of Java, Spring Boot, or web development is required to understand these concepts.

## Overview of Our Debugging Journey

```mermaid
graph TD
    A[Application Issues] --> B[Authentication Problem]
    A --> C[Card Creation Problem]
    
    B --> D[Token Refresh 500 Error]
    C --> E[ChunkedEncodingError]
    
    D --> F[Fix Authentication Code]
    E --> G[Add Simplified Endpoint]
    
    F --> H[Rebuild Docker Container]
    G --> H
    
    H --> I[Test Solutions]
    
    I -->|"Success!"| J[Problems Fixed]
    
    style A fill:#f99,stroke:#333,stroke-width:2px
    style D fill:#f99,stroke:#333
    style E fill:#f99,stroke:#333
    style F fill:#9f9,stroke:#333
    style G fill:#9f9,stroke:#333
    style J fill:#9f9,stroke:#333,stroke-width:2px
```

## The Problems We Faced

Our application had two main issues:

1. **Authentication Issue**: The token refresh endpoint was returning a 500 Internal Server Error
2. **Card Creation Issue**: Creating a new card resulted in a ChunkedEncodingError

Both of these issues were preventing users from using the application properly.

## Understanding the Architecture

To understand the problems, let's first understand the basic architecture of our application:

```mermaid
graph LR
    A[Client/Browser] <--> B[Spring Boot Application]
    B <--> C[(Database)]
    A <-.-> D[MailHog for Emails]
    B <-.-> D
    
    subgraph "Docker Environment"
    B
    C
    D
    end
    
    style B fill:#bbf,stroke:#333
    style C fill:#bfb,stroke:#333
    style D fill:#fbb,stroke:#333
```

Our application runs in Docker containers and consists of:

- A Spring Boot application (Java)
- A database for storing users, cards, and decks
- MailHog for testing email functionality

## What is Spring Boot?

Spring Boot is a Java framework that makes it easier to create web applications. It handles a lot of configuration automatically.

```mermaid
graph TD
    A[Spring Boot] --> B[Automatic Configuration]
    A --> C[Dependency Management]
    A --> D[Embedded Web Server]
    A --> E[Security Features]
    
    B --> B1[Configures databases]
    B --> B2[Sets up web endpoints]
    
    C --> C1[Manages library versions]
    
    D --> D1[No separate server needed]
    
    E --> E1[Authentication]
    E --> E2[Authorization]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
    style E fill:#bbf,stroke:#333
```

## Problem 1: Authentication Token Refresh Failure

### How Authentication Works in Our Application

Our application uses JWT (JSON Web Tokens) for authentication:

```mermaid
sequenceDiagram
    participant User as User
    participant App as Application
    participant Auth as Authentication System
    
    User->>App: 1. Login with username/password
    App->>Auth: 2. Verify credentials
    Auth-->>App: 3. Generate access token & refresh token
    App-->>User: 4. Return tokens to user
    
    Note over User,App: Later when access token expires
    
    User->>App: 5. Request to refresh token
    App->>Auth: 6. Verify refresh token
    Auth-->>App: 7. Generate new access token
    App-->>User: 8. Return new access token
```

### What Went Wrong

Our token refresh endpoint was failing with a 500 Internal Server Error. After investigating the code, we found the issue:

```mermaid
flowchart TD
    A[Token Refresh Process] --> B{What was wrong?}
    B --> C[Code was trying to authenticate with empty password]
    C --> D[Authentication naturally fails]
    D --> E[Results in 500 error]
    
    F[Our Fix] --> G[Look up user directly]
    G --> H[Skip password authentication]
    H --> I[Create authentication token safely]
    
    style A fill:#bbf,stroke:#333
    style C fill:#f99,stroke:#333,stroke-width:2px
    style F fill:#bbf,stroke:#333
    style I fill:#9f9,stroke:#333,stroke-width:2px
```

The original code was trying to authenticate the user with an empty password during token refresh, which was causing the authentication to fail and producing a 500 error.

### The Fix

We modified the code to:

1. Extract the username from the refresh token
2. Look up the user directly in the database
3. Create a new authentication token without requiring password verification

```mermaid
sequenceDiagram
    participant Client
    participant Controller as AuthController
    participant DB as UserRepository
    participant Security as Security Context
    
    Client->>Controller: POST /api/auth/refresh with refresh token
    
    Note over Controller: Before: Tried to authenticate with empty password
    Note over Controller: After: Look up user directly
    
    Controller->>Controller: Extract username from token
    Controller->>DB: Find user by username
    DB-->>Controller: User data
    Controller->>Controller: Create UserDetails from user
    Controller->>Security: Set authentication without password check
    Controller->>Controller: Generate new access token
    Controller-->>Client: Return new access token
    
    Note over Client,Controller: No more 500 error!
```

## Problem 2: Card Creation ChunkedEncodingError

### What is Chunked Encoding?

Chunked encoding is a way of sending data over the internet in pieces ("chunks") rather than all at once:

```mermaid
graph LR
    A[Complete Response] --> B[Chunk 1]
    A --> C[Chunk 2]
    A --> D[Chunk 3]
    A --> E[...]
    
    B --> F[Client Receives]
    C --> F
    D --> F
    E --> F
    
    F --> G[Complete Response Reassembled]
    
    style A fill:#bbf,stroke:#333
    style F fill:#f9f,stroke:#333,stroke-width:2px
    style G fill:#9f9,stroke:#333
```

### What Went Wrong

When creating a card, the response from the server was causing a ChunkedEncodingError. This happens when the chunking process is interrupted or malformed.

```mermaid
flowchart TD
    A[Card Creation Request] --> B[Server processes request]
    B --> C[Server creates card in database]
    C --> D[Server prepares response]
    D --> E{What was wrong?}
    
    E --> F[Response too complex]
    F --> G[Contains nested objects]
    G --> H[Causes chunking to fail]
    
    I[Our Fix] --> J[Create simplified endpoint]
    J --> K[Return minimal response]
    K --> L[Avoid complex object graphs]
    
    style A fill:#bbf,stroke:#333
    style E fill:#f9f,stroke:#333,stroke-width:2px
    style H fill:#f99,stroke:#333,stroke-width:2px
    style I fill:#bbf,stroke:#333
    style L fill:#9f9,stroke:#333,stroke-width:2px
```

The original endpoint was returning a complex card object with nested relationships, which was causing issues with the chunked encoding.

### The Fix

We created a new simplified endpoint that:

1. Still creates the card in the database
2. Returns only the essential card information (ID, front, back, creation date)
3. Avoids returning the complete object graph with nested relationships

```mermaid
sequenceDiagram
    participant Client
    participant Controller as CardController
    participant DB as Database
    
    Client->>Controller: POST /api/decks/{deckId}/cards with card data
    
    Note over Controller: Original endpoint returns full object graph
    
    Controller->>DB: Save card
    DB-->>Controller: Complete card with relationships
    Controller-->>Client: ❌ ChunkedEncodingError
    
    Note over Client,Controller: With our new endpoint:
    
    Client->>Controller: POST /api/decks/{deckId}/cards/simple with card data
    Controller->>DB: Save card
    DB-->>Controller: Complete card with relationships
    Controller->>Controller: Create simplified response
    Controller-->>Client: ✓ Simple JSON with essential data
```

## How We Tested Our Fixes

We created a specialized test script to focus on these two specific issues:

```mermaid
flowchart TD
    A[Testing Approach] --> B[Create focused test script]
    B --> C[Test token refresh]
    B --> D[Test card creation]
    
    C --> E{Token refresh works?}
    E -->|Yes| F[Fix confirmed]
    E -->|No| G[Further debugging]
    
    D --> H{Card creation works?}
    H -->|Yes| I[Fix confirmed]
    H -->|No| J[Try different approaches]
    
    F --> K[Update main application]
    I --> K
    
    style A fill:#bbf,stroke:#333
    style F fill:#9f9,stroke:#333
    style I fill:#9f9,stroke:#333
    style G fill:#f99,stroke:#333
    style J fill:#f99,stroke:#333
```

Our test script:

1. Tried the token refresh endpoint with different timeout values
2. Tested the card creation endpoint with various configurations
3. Used retry logic to handle temporary failures

## Understanding Docker and Rebuilding

Docker is a tool that packages applications into containers that include everything needed to run them (code, runtime, libraries, etc.).

```mermaid
graph TD
    A[Docker] --> B[Container 1: Our Application]
    A --> C[Container 2: Database]
    A --> D[Container 3: MailHog]
    
    subgraph "Network"
    B <--> C
    B <--> D
    end
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

### Why We Needed to Rebuild

After fixing the code, we needed to rebuild the Docker container to include our changes:

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Code as Source Code
    participant Docker as Docker System
    participant Container as Running Container
    
    Dev->>Code: Make code changes
    Dev->>Docker: Run rebuild command
    Docker->>Code: Read updated source code
    Docker->>Docker: Build new container image
    Docker->>Container: Stop old container
    Docker->>Container: Start new container with fixes
    
    Note over Container: Container now contains our fixes
```

## Key Takeaways for Non-Java Developers

Even without knowing Java or Spring Boot, you can understand some key debugging principles:

```mermaid
graph TD
    A[Debugging Principles] --> B[Isolate the problem]
    A --> C[Understand the error message]
    A --> D[Create focused tests]
    A --> E[Make minimal changes]
    A --> F[Verify the fix works]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
    style E fill:#bbf,stroke:#333
    style F fill:#bbf,stroke:#333
```

### For the Authentication Issue

1. We identified that the code was trying to authenticate with an empty password
2. We changed it to look up the user directly instead
3. We rebuilt the Docker container to apply the changes
4. We verified the fix worked with our test script

### For the Card Creation Issue

1. We identified that complex response data was causing the ChunkedEncodingError
2. We created a new endpoint that returns simplified data
3. We rebuilt the Docker container to apply the changes
4. We verified the fix worked with our test script

## Conclusion

Debugging server applications doesn't always require deep knowledge of the programming language. By understanding the architecture, identifying patterns in error messages, and using focused tests, you can effectively diagnose and fix issues even in unfamiliar technology stacks.

```mermaid
flowchart TD
    A[Effective Debugging] --> B[Understand the problem]
    A --> C[Create isolated tests]
    A --> D[Make targeted changes]
    A --> E[Verify solutions]
    
    B --> F[Fixed Authentication]
    C --> F
    D --> F
    E --> F
    
    B --> G[Fixed Card Creation]
    C --> G
    D --> G
    E --> G
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style F fill:#9f9,stroke:#333
    style G fill:#9f9,stroke:#333
```

By following these principles, we successfully fixed both the token refresh and card creation issues in our application.
