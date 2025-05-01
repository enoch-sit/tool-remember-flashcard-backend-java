# Flashcard Remembering App API Documentation

This document provides comprehensive technical documentation for a flashcard remembering application backend API, based on the authentication endpoints infrastructure described in the provided document. This API is built with Java and shares the same JWT authentication mechanism.

## API Overview

The flashcard remembering application uses a RESTful API architecture to manage user accounts, authentication, and flashcard data. The system leverages JWT tokens for secure authentication across all protected endpoints.

## Authentication System

The application shares the same JWT authentication system as described in the AuthAPIEndpoint.md document. This includes:

- JWT token-based authentication with access and refresh tokens
- Access tokens expire after 15 minutes
- Refresh tokens for obtaining new access tokens
- User registration, verification, and password management
- Role-based permissions (admin, supervisor, enduser)

## Base URL

All endpoints are relative to:

- Development: `http://localhost:3000`
- Production: Your deployed API URL

## Flashcard API Endpoints

### Deck Management

#### Get All Decks

```
GET /api/decks
```

**Description**: Returns all flashcard decks belonging to the authenticated user.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "decks": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "cardCount": number,
      "createdAt": "date",
      "updatedAt": "date",
      "lastStudied": "date"
    }
  ]
}
```

#### Create New Deck

```
POST /api/decks
```

**Description**: Creates a new flashcard deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "name": "string",
  "description": "string"
}
```

**Response (201 Created)**:

```json
{
  "message": "Deck created successfully",
  "deck": {
    "id": "string",
    "name": "string",
    "description": "string",
    "cardCount": 0,
    "createdAt": "date",
    "updatedAt": "date"
  }
}
```

#### Get Deck Details

```
GET /api/decks/{deckId}
```

**Description**: Returns details of a specific deck including cards.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "deck": {
    "id": "string",
    "name": "string",
    "description": "string",
    "createdAt": "date",
    "updatedAt": "date",
    "lastStudied": "date",
    "cards": [
      {
        "id": "string",
        "front": "string",
        "back": "string",
        "notes": "string",
        "difficulty": number,
        "nextReviewDate": "date",
        "reviewCount": number,
        "createdAt": "date",
        "updatedAt": "date"
      }
    ]
  }
}
```

#### Update Deck

```
PUT /api/decks/{deckId}
```

**Description**: Updates the details of an existing deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "name": "string",
  "description": "string"
}
```

**Response (200 OK)**:

```json
{
  "message": "Deck updated successfully",
  "deck": {
    "id": "string",
    "name": "string",
    "description": "string",
    "cardCount": number,
    "createdAt": "date",
    "updatedAt": "date"
  }
}
```

#### Delete Deck

```
DELETE /api/decks/{deckId}
```

**Description**: Deletes a deck and all its cards.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "message": "Deck deleted successfully"
}
```

### Card Management

#### Get Cards in Deck

```
GET /api/decks/{deckId}/cards
```

**Description**: Returns all cards in a specific deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Query Parameters**:

- `sort` (optional): Field to sort by (default: "nextReviewDate")
- `order` (optional): "asc" or "desc" (default: "asc")
- `page` (optional): Page number (default: 1)
- `limit` (optional): Items per page (default: 20)

**Response (200 OK)**:

```json
{
  "cards": [
    {
      "id": "string",
      "front": "string",
      "back": "string",
      "notes": "string",
      "difficulty": number,
      "nextReviewDate": "date",
      "reviewCount": number,
      "createdAt": "date",
      "updatedAt": "date"
    }
  ],
  "pagination": {
    "total": number,
    "page": number,
    "limit": number,
    "pages": number
  }
}
```

#### Create Card

```
POST /api/decks/{deckId}/cards
```

**Description**: Creates a new flashcard in the specified deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "front": "string",
  "back": "string",
  "notes": "string"
}
```

**Response (201 Created)**:

```json
{
  "message": "Card created successfully",
  "card": {
    "id": "string",
    "front": "string",
    "back": "string",
    "notes": "string",
    "difficulty": 0,
    "nextReviewDate": "date",
    "reviewCount": 0,
    "createdAt": "date",
    "updatedAt": "date"
  }
}
```

#### Create Multiple Cards

```
POST /api/decks/{deckId}/cards/batch
```

**Description**: Creates multiple cards at once in the specified deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "cards": [
    {
      "front": "string",
      "back": "string",
      "notes": "string"
    },
    {
      "front": "string",
      "back": "string",
      "notes": "string"
    }
  ]
}
```

**Response (201 Created)**:

```json
{
  "message": "Cards created successfully",
  "cardCount": number
}
```

#### Get Card Details

```
GET /api/cards/{cardId}
```

**Description**: Returns details of a specific card.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "card": {
    "id": "string",
    "deckId": "string",
    "front": "string",
    "back": "string",
    "notes": "string",
    "difficulty": number,
    "nextReviewDate": "date",
    "reviewCount": number,
    "createdAt": "date",
    "updatedAt": "date"
  }
}
```

#### Update Card

```
PUT /api/cards/{cardId}
```

**Description**: Updates an existing flashcard.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "front": "string",
  "back": "string",
  "notes": "string"
}
```

**Response (200 OK)**:

```json
{
  "message": "Card updated successfully",
  "card": {
    "id": "string",
    "front": "string",
    "back": "string",
    "notes": "string",
    "difficulty": number,
    "nextReviewDate": "date",
    "reviewCount": number,
    "createdAt": "date",
    "updatedAt": "date"
  }
}
```

#### Delete Card

```
DELETE /api/cards/{cardId}
```

**Description**: Deletes a specific card.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "message": "Card deleted successfully"
}
```

### Study Session Management

#### Get Cards Due for Review

```
GET /api/decks/{deckId}/review
```

**Description**: Returns cards due for review based on spaced repetition algorithm.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Query Parameters**:

- `limit` (optional): Maximum number of cards to return (default: 20)

**Response (200 OK)**:

```json
{
  "cards": [
    {
      "id": "string",
      "front": "string",
      "back": "string",
      "notes": "string",
      "difficulty": number,
      "nextReviewDate": "date",
      "reviewCount": number
    }
  ],
  "sessionId": "string",
  "totalDue": number
}
```

#### Record Review Result

```
POST /api/review
```

**Description**: Records the result of a card review and updates its spaced repetition data.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "cardId": "string",
  "sessionId": "string",
  "result": number, // 0-5 scale where 0 = failed, 5 = perfect recall
  "timeSpent": number // seconds spent reviewing
}
```

**Response (200 OK)**:

```json
{
  "message": "Review recorded",
  "nextReviewDate": "date",
  "newDifficulty": number
}
```

#### Complete Study Session

```
POST /api/review/complete
```

**Description**: Marks a study session as complete and records statistics.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Request Body**:

```json
{
  "sessionId": "string",
  "cardsReviewed": number,
  "totalTime": number // seconds
}
```

**Response (200 OK)**:

```json
{
  "message": "Session completed",
  "stats": {
    "cardsReviewed": number,
    "correctResponses": number,
    "incorrectResponses": number,
    "averageTime": number,
    "streak": number
  }
}
```

### Statistics and Progress

#### Get User Statistics

```
GET /api/stats
```

**Description**: Returns overall study statistics for the user.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "stats": {
    "totalCards": number,
    "totalDecks": number,
    "cardsReviewed": number,
    "studyTime": number, // total minutes
    "currentStreak": number,
    "longestStreak": number,
    "masteredCards": number
  }
}
```

#### Get Deck Statistics

```
GET /api/stats/deck/{deckId}
```

**Description**: Returns study statistics for a specific deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response (200 OK)**:

```json
{
  "stats": {
    "totalCards": number,
    "cardsReviewed": number,
    "averageDifficulty": number,
    "masteredCards": number,
    "cardsToReview": number,
    "lastStudied": "date"
  }
}
```

#### Get Study History

```
GET /api/stats/history
```

**Description**: Returns the user's study history.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Query Parameters**:

- `period` (optional): "week", "month", "year" (default: "month")
- `page` (optional): Page number (default: 1)
- `limit` (optional): Items per page (default: 30)

**Response (200 OK)**:

```json
{
  "history": [
    {
      "date": "date",
      "cardsReviewed": number,
      "timeSpent": number, // minutes
      "performance": number // 0-100 performance score
    }
  ],
  "pagination": {
    "total": number,
    "page": number,
    "limit": number,
    "pages": number
  }
}
```

### Import/Export Features

#### Import Cards from CSV

```
POST /api/decks/{deckId}/import/csv
```

**Description**: Imports cards from a CSV file into the specified deck.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Form Data**:

- `file`: CSV file with columns "front", "back", "notes"

**Response (200 OK)**:

```json
{
  "message": "Import successful",
  "imported": number,
  "failed": number,
  "errors": [
    {
      "line": number,
      "error": "string"
    }
  ]
}
```

#### Export Deck to CSV

```
GET /api/decks/{deckId}/export/csv
```

**Description**: Exports all cards in a deck to a CSV file.

**Access Level**: Authenticated

**Headers**:

```
Authorization: Bearer <access_token>
```

**Response**: CSV file download

## Response Status Codes

- `200 OK`: The request succeeded
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid input data
- `401 Unauthorized`: Authentication required or failed
- `403 Forbidden`: Permission denied for the requested resource
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

## Developer Guide for Non-Java Programmers

This section helps non-Java developers understand the architecture and implementation of the flashcard application backend.

### System Architecture

The application follows a standard Java backend architecture:

1. **Controller Layer**: Handles HTTP requests and responses
2. **Service Layer**: Contains business logic
3. **Repository Layer**: Interfaces with the database
4. **Model/Entity Layer**: Data structures and objects

### Authentication Flow

1. **Registration**: Users register with email, username, and password
2. **Email Verification**: Users verify their email with a token sent via email
3. **Login**: Users receive JWT access and refresh tokens
4. **Access Protected Resources**: Users include access token in Authorization header
5. **Token Refresh**: When access token expires, the refresh token is used to get a new one

### Spaced Repetition Algorithm

The flashcard system uses a spaced repetition algorithm based on the SM-2 algorithm:

1. Each card has a difficulty level (0-5)
2. Review results affect the card's difficulty
3. Next review date is calculated based on:
   - Current difficulty
   - How well the user recalled the answer
   - Previous intervals

### Database Structure

The application uses a relational database with the following main tables:

1. **Users**: User accounts and authentication data
2. **Decks**: Flashcard decks owned by users
3. **Cards**: Individual flashcards belonging to decks
4. **Reviews**: History of card reviews and user performance
5. **Sessions**: Study session data

### Integration Guide

To integrate with this API from a non-Java client:

1. **Authentication**:
   - Register or login to obtain JWT tokens
   - Store both access and refresh tokens securely
   - Include access token in the Authorization header for all protected requests
   - Handle 401 errors by refreshing the access token

2. **Basic Operations**:
   - Create decks to organize flashcards
   - Add cards to decks with front (question) and back (answer) content
   - Use the review endpoints to implement spaced repetition studying

3. **Error Handling**:
   - Check HTTP status codes for errors
   - Handle 400-level errors by checking the error message in the response
   - For 500-level errors, implement retry logic or user-friendly error messages

4. **Performance Considerations**:
   - Cache frequently accessed data like deck lists
   - Use pagination for large collections of cards
   - Batch operations for creating multiple cards at once

### Security Best Practices

1. Never store access tokens in localStorage (use secure HTTP-only cookies or memory)
2. Implement CSRF protection for cookie-based authentication
3. Use HTTPS for all API communications
4. Validate all user inputs before sending to the API
5. Implement rate limiting on the client side
