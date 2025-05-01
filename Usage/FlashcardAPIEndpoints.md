# Flashcard Application API Documentation

## Overview

This document provides a comprehensive guide to the Flashcard Application REST API. It's designed for frontend developers to understand and integrate with the backend system without requiring Java knowledge.

## Base URL

All endpoints are relative to:

- Development: <http://localhost:3000>
- Production: Your deployed API URL

## Authentication

The application uses JWT (JSON Web Token) for authentication:

- Access tokens expire after 15 minutes by default
- Include tokens in the Authorization header: `Authorization: Bearer <token>`
- Protected routes require authentication

## Response Status Codes

- 200 OK: Request succeeded
- 201 Created: Resource created successfully
- 400 Bad Request: Invalid input data
- 401 Unauthorized: Authentication required or failed
- 403 Forbidden: Permission denied for the requested resource
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server error

## API Endpoints

### Health Check

#### Check API Status

- URL: `/health`
- Method: `GET`
- Auth Required: No
- Description: Confirms that the API is running properly
- Response Example:

```json
{
  "status": "UP",
  "version": "1.0.0"
}
```

### Authentication

#### Register a New User

- URL: `/api/auth/signup`
- Method: `POST`
- Auth Required: No
- Description: Creates a new user account and sends verification email
- Request Body:

```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

- Response (201 Created):

```json
{
  "userId": "1234567890",
  "message": "User registered successfully. Please verify your email."
}
```

- Possible Errors:
  - 400: Username already exists
  - 400: Email already in use

#### Verify Email

- URL: `/api/auth/verify-email`
- Method: `POST`
- Auth Required: No
- Description: Verifies a user's email with the token sent via email
- Request Body:

```json
{
  "token": "verification-token-from-email"
}
```

- Response (200 OK):

```json
{
  "message": "Email verified successfully. You can now log in."
}
```

- Possible Errors:
  - 400: Token is required
  - 400: Token expired
  - 500: Email verification failed

#### Resend Verification Email

- URL: `/api/auth/resend-verification`
- Method: `POST`
- Auth Required: No
- Description: Resends the verification email
- Request Body:

```json
{
  "email": "user@example.com"
}
```

- Response (200 OK):

```json
{
  "message": "Verification email has been resent."
}
```

- Possible Errors:
  - 400: Email is required
  - 400: Email is already verified
  - 500: Failed to resend verification code

#### Login

- URL: `/api/auth/login`
- Method: `POST`
- Auth Required: No
- Description: Authenticates user and returns access and refresh tokens
- Request Body:

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

- Response (200 OK):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- Possible Errors:
  - 401: Invalid credentials
  - 401: Email not verified

#### Refresh Token

- URL: `/api/auth/refresh`
- Method: `POST`
- Auth Required: No
- Description: Creates a new access token using a valid refresh token
- Request Body:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

- Response (200 OK):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- Possible Errors:
  - 400: Refresh token is required
  - 401: Invalid refresh token
  - 500: Token refresh failed

#### Logout

- URL: `/api/auth/logout`
- Method: `POST`
- Auth Required: No
- Description: Invalidates the current refresh token
- Request Body:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

- Response (200 OK):

```json
{
  "message": "Logged out successfully"
}
```

#### Logout from All Devices

- URL: `/api/auth/logout-all`
- Method: `POST`
- Auth Required: Yes
- Description: Invalidates all refresh tokens for the current user
- Response (200 OK):

```json
{
  "message": "Logged out from all devices successfully"
}
```

#### Request Password Reset

- URL: `/api/auth/forgot-password`
- Method: `POST`
- Auth Required: No
- Description: Sends a password reset email to the specified address
- Request Body:

```json
{
  "email": "user@example.com"
}
```

- Response (200 OK):

```json
{
  "message": "If the email exists, a password reset link has been sent."
}
```

- Security Note: For security purposes, this endpoint returns 200 OK even if the email is not found in the system

#### Reset Password

- URL: `/api/auth/reset-password`
- Method: `POST`
- Auth Required: No
- Description: Resets a user's password using the token sent via email
- Request Body:

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePassword123"
}
```

- Response (200 OK):

```json
{
  "message": "Password has been reset successfully."
}
```

- Possible Errors:
  - 400: Token and new password are required
  - 400: Token expired
  - 400: Password reset failed

### Deck Management

#### Get All Decks

- URL: `/api/decks`
- Method: `GET`
- Auth Required: Yes
- Description: Returns all flashcard decks belonging to the authenticated user
- Response (200 OK):

```json
{
  "decks": [
    {
      "id": "deck123",
      "name": "Spanish Vocabulary",
      "description": "Basic Spanish words and phrases",
      "cardCount": 42,
      "createdAt": "2023-05-10T14:30:00Z",
      "lastModified": "2023-05-15T09:45:00Z"
    },
    {
      "id": "deck456",
      "name": "JavaScript Concepts",
      "description": "Core JavaScript programming concepts",
      "cardCount": 35,
      "createdAt": "2023-04-20T11:15:00Z",
      "lastModified": "2023-05-14T16:20:00Z"
    }
  ]
}
```

#### Get Deck Details

- URL: `/api/decks/{deckId}`
- Method: `GET`
- Auth Required: Yes
- Description: Returns details of a specific deck
- Path Parameters:
  - deckId: The ID of the deck
- Response (200 OK):

```json
{
  "id": "deck123",
  "name": "Spanish Vocabulary",
  "description": "Basic Spanish words and phrases",
  "cardCount": 42,
  "createdAt": "2023-05-10T14:30:00Z",
  "lastModified": "2023-05-15T09:45:00Z",
  "lastStudied": "2023-05-14T10:20:00Z",
  "tags": ["language", "spanish", "beginner"]
}
```

- Possible Errors:
  - 404: Deck not found

#### Create New Deck

- URL: `/api/decks`
- Method: `POST`
- Auth Required: Yes
- Description: Creates a new flashcard deck
- Request Body:

```json
{
  "name": "French Basics",
  "description": "Essential French vocabulary and phrases",
  "tags": ["language", "french", "beginner"]
}
```

- Response (201 Created):

```json
{
  "id": "deck789",
  "name": "French Basics",
  "description": "Essential French vocabulary and phrases",
  "cardCount": 0,
  "createdAt": "2023-05-17T08:25:00Z",
  "lastModified": "2023-05-17T08:25:00Z",
  "tags": ["language", "french", "beginner"]
}
```

#### Update Deck

- URL: `/api/decks/{deckId}`
- Method: `PUT`
- Auth Required: Yes
- Description: Updates the details of an existing deck
- Path Parameters:
  - deckId: The ID of the deck
- Request Body:

```json
{
  "name": "French Vocabulary - Advanced",
  "description": "Advanced French vocabulary and expressions",
  "tags": ["language", "french", "advanced"]
}
```

- Response (200 OK):

```json
{
  "id": "deck789",
  "name": "French Vocabulary - Advanced",
  "description": "Advanced French vocabulary and expressions",
  "cardCount": 0,
  "createdAt": "2023-05-17T08:25:00Z",
  "lastModified": "2023-05-17T09:10:00Z",
  "tags": ["language", "french", "advanced"]
}
```

- Possible Errors:
  - 400: Deck not found or you don't have access to this deck

#### Delete Deck

- URL: `/api/decks/{deckId}`
- Method: `DELETE`
- Auth Required: Yes
- Description: Deletes a deck and all its cards
- Path Parameters:
  - deckId: The ID of the deck
- Response (200 OK):

```json
{
  "message": "Deck deleted successfully"
}
```

- Possible Errors:
  - 400: Deck not found or you don't have access to this deck

### Card Management

#### Get Cards in Deck

- URL: `/api/decks/{deckId}/cards`
- Method: `GET`
- Auth Required: Yes
- Description: Returns all cards in a specific deck
- Path Parameters:
  - deckId: The ID of the deck
- Query Parameters:
  - page: Page number (default: 0)
  - size: Items per page (default: 10)
  - sort: Field to sort by, followed by direction (default: "id,asc")
- Response (200 OK):

```json
{
  "content": [
    {
      "id": "card123",
      "front": "Hola",
      "back": "Hello",
      "notes": "Basic greeting",
      "tags": ["greeting", "basic"],
      "createdAt": "2023-05-10T15:00:00Z",
      "lastModified": "2023-05-10T15:00:00Z",
      "nextReviewAt": "2023-05-18T15:00:00Z",
      "repetitionCount": 3,
      "easeFactor": 2.5
    },
    {
      "id": "card124",
      "front": "Gracias",
      "back": "Thank you",
      "notes": "Basic courtesy",
      "tags": ["courtesy", "basic"],
      "createdAt": "2023-05-10T15:05:00Z",
      "lastModified": "2023-05-10T15:05:00Z",
      "nextReviewAt": "2023-05-19T15:05:00Z",
      "repetitionCount": 2,
      "easeFactor": 2.2
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": "id,asc"
  },
  "totalPages": 5,
  "totalElements": 42,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0
}
```

- Possible Errors:
  - 404: Deck not found or you don't have access to this deck

#### Get Card Details

- URL: `/api/decks/{deckId}/cards/{cardId}`
- Method: `GET`
- Auth Required: Yes
- Description: Returns details of a specific card
- Path Parameters:
  - deckId: The ID of the deck
  - cardId: The ID of the card
- Response (200 OK):

```json
{
  "id": "card123",
  "front": "Hola",
  "back": "Hello",
  "notes": "Basic greeting",
  "tags": ["greeting", "basic"],
  "createdAt": "2023-05-10T15:00:00Z",
  "lastModified": "2023-05-10T15:00:00Z",
  "nextReviewAt": "2023-05-18T15:00:00Z",
  "repetitionCount": 3,
  "easeFactor": 2.5,
  "reviewHistory": [
    {
      "reviewedAt": "2023-05-10T20:00:00Z",
      "result": 4
    },
    {
      "reviewedAt": "2023-05-12T18:30:00Z",
      "result": 5
    },
    {
      "reviewedAt": "2023-05-15T16:45:00Z",
      "result": 3
    }
  ]
}
```

- Possible Errors:
  - 404: Card not found

#### Get Cards Due for Review

- URL: `/api/decks/{deckId}/review-cards`
- Method: `GET`
- Auth Required: Yes
- Description: Returns cards due for review based on the spaced repetition algorithm
- Path Parameters:
  - deckId: The ID of the deck
- Query Parameters:
  - limit: Maximum number of cards to return (default: 10)
- Response (200 OK):

```json
{
  "cards": [
    {
      "id": "card123",
      "front": "Hola",
      "back": "Hello",
      "notes": "Basic greeting",
      "tags": ["greeting", "basic"],
      "repetitionCount": 3,
      "easeFactor": 2.5
    },
    {
      "id": "card125",
      "front": "Adiós",
      "back": "Goodbye",
      "notes": "Basic farewell",
      "tags": ["farewell", "basic"],
      "repetitionCount": 1,
      "easeFactor": 2.0
    }
  ],
  "totalDue": 8
}
```

- Possible Errors:
  - 404: Deck not found or you don't have access to this deck

#### Create Card

- URL: `/api/decks/{deckId}/cards`
- Method: `POST`
- Auth Required: Yes
- Description: Creates a new flashcard in the specified deck
- Path Parameters:
  - deckId: The ID of the deck
- Request Body:

```json
{
  "front": "Buenos días",
  "back": "Good morning",
  "notes": "Morning greeting",
  "tags": ["greeting", "morning"]
}
```

- Response (201 Created):

```json
{
  "id": "card126",
  "front": "Buenos días",
  "back": "Good morning",
  "notes": "Morning greeting",
  "tags": ["greeting", "morning"],
  "createdAt": "2023-05-17T10:00:00Z",
  "lastModified": "2023-05-17T10:00:00Z",
  "nextReviewAt": "2023-05-18T10:00:00Z",
  "repetitionCount": 0,
  "easeFactor": 2.5
}
```

- Possible Errors:
  - 404: Deck not found or you don't have access to this deck

#### Update Card

- URL: `/api/decks/{deckId}/cards/{cardId}`
- Method: `PUT`
- Auth Required: Yes
- Description: Updates an existing flashcard
- Path Parameters:
  - deckId: The ID of the deck
  - cardId: The ID of the card
- Request Body:

```json
{
  "front": "Buenos días",
  "back": "Good morning / Good day",
  "notes": "Morning or daytime greeting",
  "tags": ["greeting", "morning", "daytime"]
}
```

- Response (200 OK):

```json
{
  "id": "card126",
  "front": "Buenos días",
  "back": "Good morning / Good day",
  "notes": "Morning or daytime greeting",
  "tags": ["greeting", "morning", "daytime"],
  "createdAt": "2023-05-17T10:00:00Z",
  "lastModified": "2023-05-17T10:15:00Z",
  "nextReviewAt": "2023-05-18T10:00:00Z",
  "repetitionCount": 0,
  "easeFactor": 2.5
}
```

- Possible Errors:
  - 404: Card not found
  - 404: Deck not found or you don't have access to this deck

#### Delete Card

- URL: `/api/decks/{deckId}/cards/{cardId}`
- Method: `DELETE`
- Auth Required: Yes
- Description: Deletes a specific card
- Path Parameters:
  - deckId: The ID of the deck
  - cardId: The ID of the card
- Response (200 OK):

```json
{
  "message": "Card deleted successfully"
}
```

- Possible Errors:
  - 404: Card not found
  - 404: Deck not found or you don't have access to this deck

### Study Session Management

#### Get All Study Sessions

- URL: `/api/study-sessions`
- Method: `GET`
- Auth Required: Yes
- Description: Returns all study sessions for the authenticated user
- Query Parameters:
  - page: Page number (default: 0)
  - size: Items per page (default: 10)
  - sort: Field to sort by, followed by direction (default: "startedAt,desc")
- Response (200 OK):

```json
{
  "content": [
    {
      "id": "session123",
      "deckId": "deck123",
      "deckName": "Spanish Vocabulary",
      "startedAt": "2023-05-15T19:30:00Z",
      "completedAt": "2023-05-15T19:45:00Z",
      "cardsStudied": 15,
      "cardsCorrect": 12,
      "timeSpentMs": 900000
    },
    {
      "id": "session122",
      "deckId": "deck123",
      "deckName": "Spanish Vocabulary",
      "startedAt": "2023-05-14T18:00:00Z",
      "completedAt": "2023-05-14T18:20:00Z",
      "cardsStudied": 20,
      "cardsCorrect": 15,
      "timeSpentMs": 1200000
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": "startedAt,desc"
  },
  "totalPages": 3,
  "totalElements": 22,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0
}
```

#### Get Study Session Details

- URL: `/api/study-sessions/{sessionId}`
- Method: `GET`
- Auth Required: Yes
- Description: Returns details of a specific study session
- Path Parameters:
  - sessionId: The unique session ID
- Response (200 OK):

```json
{
  "id": "session123",
  "deckId": "deck123",
  "deckName": "Spanish Vocabulary",
  "startedAt": "2023-05-15T19:30:00Z",
  "completedAt": "2023-05-15T19:45:00Z",
  "cardsStudied": 15,
  "cardsCorrect": 12,
  "timeSpentMs": 900000,
  "reviews": [
    {
      "cardId": "card123",
      "front": "Hola",
      "back": "Hello",
      "result": 5,
      "reviewedAt": "2023-05-15T19:31:00Z",
      "timeSpentMs": 3000
    },
    {
      "cardId": "card124",
      "front": "Gracias",
      "back": "Thank you",
      "result": 4,
      "reviewedAt": "2023-05-15T19:32:00Z",
      "timeSpentMs": 5000
    }
  ]
}
```

- Possible Errors:
  - 403: You don't have access to this study session
  - 404: Study session not found

#### Start Study Session

- URL: `/api/decks/{deckId}/study-sessions`
- Method: `POST`
- Auth Required: Yes
- Description: Creates a new study session for the specified deck
- Path Parameters:
  - deckId: The ID of the deck
- Response (201 Created):

```json
{
  "sessionId": "session124",
  "deckId": "deck123",
  "deckName": "Spanish Vocabulary",
  "startedAt": "2023-05-17T14:00:00Z",
  "cardsAvailable": 8
}
```

- Possible Errors:
  - 404: Deck not found or you don't have access to this deck

#### Complete Study Session

- URL: `/api/study-sessions/{sessionId}/complete`
- Method: `PUT`
- Auth Required: Yes
- Description: Marks a study session as complete and records statistics
- Path Parameters:
  - sessionId: The unique session ID
- Request Body:

```json
{
  "timeSpentMs": 840000
}
```

- Response (200 OK):

```json
{
  "id": "session124",
  "deckId": "deck123",
  "deckName": "Spanish Vocabulary",
  "startedAt": "2023-05-17T14:00:00Z",
  "completedAt": "2023-05-17T14:14:00Z",
  "cardsStudied": 10,
  "cardsCorrect": 8,
  "timeSpentMs": 840000
}
```

- Possible Errors:
  - 403: You don't have access to this study session
  - 404: Study session not found

### Card Review Management

#### Submit Card Review

- URL: `/api/study-sessions/{sessionId}/reviews`
- Method: `POST`
- Auth Required: Yes
- Description: Records the result of a card review and updates its spaced repetition data
- Path Parameters:
  - sessionId: The unique session ID
- Request Body:

```json
{
  "cardId": "card123",
  "result": 4,
  "timeSpentMs": 5000
}
```

- The result parameter uses a 0-5 scale:
  - 0: Incorrect answer
  - 1-5: Correct answer with varying difficulty (1 = hardest, 5 = easiest)
- Response (201 Created):

```json
{
  "id": "review123",
  "cardId": "card123",
  "sessionId": "session124",
  "result": 4,
  "timeSpentMs": 5000,
  "reviewedAt": "2023-05-17T14:02:00Z",
  "nextReviewAt": "2023-05-20T14:02:00Z"
}
```

- Possible Errors:
  - 400: Card does not belong to the deck being studied
  - 403: You don't have access to this study session
  - 404: Card not found
  - 404: Study session not found

#### Get Card Review History

- URL: `/api/cards/{cardId}/reviews`
- Method: `GET`
- Auth Required: Yes
- Description: Returns the review history for a specific card
- Path Parameters:
  - cardId: The ID of the card
- Response (200 OK):

```json
{
  "reviews": [
    {
      "id": "review123",
      "sessionId": "session124",
      "result": 4,
      "timeSpentMs": 5000,
      "reviewedAt": "2023-05-17T14:02:00Z"
    },
    {
      "id": "review120",
      "sessionId": "session123",
      "result": 3,
      "timeSpentMs": 8000,
      "reviewedAt": "2023-05-15T19:35:00Z"
    },
    {
      "id": "review115",
      "sessionId": "session122",
      "result": 2,
      "timeSpentMs": 10000,
      "reviewedAt": "2023-05-14T18:05:00Z"
    }
  ],
  "totalReviews": 3,
  "averageResult": 3.0,
  "averageTimeMs": 7666
}
```

- Possible Errors:
  - 403: You don't have access to this card
  - 404: Card not found

### Statistics

#### Get Study Activity

- URL: `/api/stats/study-activity`
- Method: `GET`
- Auth Required: Yes
- Description: Returns study activity statistics for the authenticated user
- Query Parameters:
  - days: Period in days to analyze (default: 7)
- Response (200 OK):

```json
{
  "activityByDay": [
    {
      "date": "2023-05-17",
      "cardsStudied": 10,
      "timeSpentMinutes": 14,
      "correctPercentage": 80
    },
    {
      "date": "2023-05-16",
      "cardsStudied": 0,
      "timeSpentMinutes": 0,
      "correctPercentage": 0
    },
    {
      "date": "2023-05-15",
      "cardsStudied": 15,
      "timeSpentMinutes": 15,
      "correctPercentage": 80
    }
  ],
  "totalCardsStudied": 25,
  "totalTimeSpentMinutes": 29,
  "averageCorrectPercentage": 80,
  "currentStreak": 1,
  "longestStreak": 2
}
```

## Best Practices for Frontend Integration

### Authentication Flow

- Registration: Call the signup endpoint and store the userId
- Email Verification: Prompt users to check their email and verify their account
- Login: Authenticate and store both access and refresh tokens securely

### Token Management

- Include the access token in the Authorization header for all requests
- When requests fail with 401, try refreshing the access token
- If refresh fails, redirect to login

### Security Considerations

- Store tokens securely:
  - Use secure HTTP-only cookies when possible
  - Or store in memory with a fallback to secure storage
- Always validate user input before sending to the API
- Implement CSRF protection for cookie-based authentication
- Use HTTPS for all API communications

### Error Handling

- Implement global error handling for API responses
- Handle 401 errors by refreshing tokens or redirecting to login
- Display user-friendly messages for common errors
- Implement retry logic for network issues

### Performance Tips

- Cache frequently accessed data like deck lists
- Use pagination for large collections of cards
- Consider implementing optimistic UI updates for better user experience

### Example API Usage (JavaScript)

#### Authentication Example

```javascript
async function login(email, password) {
  try {
    const response = await fetch('http://localhost:3000/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, password })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Login failed');
    }
    
    const { accessToken, refreshToken } = await response.json();
    
    // Store tokens securely
    localStorage.setItem('refreshToken', refreshToken);
    sessionStorage.setItem('accessToken', accessToken);
    
    return true;
  } catch (error) {
    console.error('Login error:', error);
    return false;
  }
}
```

This documentation provides an overview of all endpoints available in the Flashcard Application API, along with example usage for frontend developers. For more detailed implementation guidelines, please refer to the full technical specification.
