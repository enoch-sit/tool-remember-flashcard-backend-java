# Authentication Guide

This guide explains the authentication system used in the Flashcard Application.

## Overview

The Flashcard Application uses JSON Web Tokens (JWT) for authentication and authorization. This stateless approach eliminates the need for server-side session storage and enables efficient scaling.

## Authentication Flow

1. **User Registration**: Creates a new user account and sends a verification email
2. **Email Verification**: User verifies their email with a token received by email
3. **User Login**: Authenticates user credentials and returns JWT tokens
4. **Accessing Protected Resources**: JWT tokens are included in API requests
5. **Token Refresh**: When access tokens expire, refresh tokens are used to get new ones
6. **Logout**: Invalidates refresh tokens to prevent further use

## Email Verification with MailHog

The development environment uses MailHog to handle email verification. MailHog is a testing tool that captures emails sent by the application without actually sending them to real email addresses.

### Accessing MailHog

When running the application with Docker, MailHog is available at:

- Web UI: <http://localhost:8025>
- SMTP server: mailhog:1025 (internal) or localhost:1025 (from host)

### How Email Verification Works

1. When a user registers, the application sends a verification email containing a token
2. In development, this email is captured by MailHog and can be viewed in its web interface
3. Access the verification link from the email to verify the account
4. After verification, the user can log in and access all application features

## JWT Token Structure

Each JWT token consists of three parts separated by dots:

- Header: Contains token type and algorithm
- Payload: Contains claims (user information and token metadata)
- Signature: Verifies token authenticity

### Access Tokens vs. Refresh Tokens

**Access Tokens**:

- Short-lived (15 minutes by default)
- Used to authenticate API requests
- Included in Authorization header with Bearer scheme

**Refresh Tokens**:

- Long-lived (7 days by default)
- Used to get new access tokens
- Should be stored securely

## API Endpoints

### User Registration

```
POST /api/auth/signup
```

Request body:

```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "securepassword"
}
```

Response:

```json
{
  "message": "User registered successfully. Verification email has been sent.",
  "userId": "123e4567-e89b-12d3-a456-426614174000"
}
```

### Email Verification

```
POST /api/auth/verify-email
```

Request body:

```json
{
  "token": "verification-token-from-email"
}
```

### Login

```
POST /api/auth/login
```

Request body:

```json
{
  "username": "user123",
  "password": "securepassword"
}
```

Response:

```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "username": "user123",
    "email": "user@example.com",
    "isVerified": true,
    "role": "ROLE_USER"
  }
}
```

### Token Refresh

```
POST /api/auth/refresh
```

Request body:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Response:

```json
{
  "message": "Token refreshed successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Logout

```
POST /api/auth/logout
```

Request body:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

## Using JWT Tokens in API Requests

To access protected resources, include the access token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Example using curl:

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." http://localhost:3000/api/profile
```

## Security Best Practices

### For Frontend Applications

1. **Store tokens securely**:
   - Store access tokens in memory (variables)
   - Store refresh tokens in secure HttpOnly cookies or use refresh token rotation
   - Never store tokens in localStorage or sessionStorage

2. **Token Validation**:
   - Validate token expiration
   - Handle refresh token flow when access tokens expire

3. **HTTPS Only**:
   - Use HTTPS in all environments

4. **Token Expiration**:
   - Handle token expiration by redirecting to login

### For Backend Configuration

1. **JWT Secret Key**:
   - Use a strong random secret key (at least 256 bits)
   - Base64 encode your secret key
   - Keep the secret key secure (use environment variables)

   The current Docker configuration uses a Base64 encoded secret:

   ```
   JWT_SECRET=VGhpcyBpcyBhIHNlY3VyZSBKV1Qgc2VjcmV0IGtleSBmb3IgZG9ja2VyIGVudmlyb25tZW50
   ```

2. **Token Lifetimes**:
   - Short-lived access tokens (15 minutes)
   - Longer-lived refresh tokens (7 days)

## Roles and Permissions

The application supports role-based access control with three roles:

- `ROLE_USER`: Standard user permissions
- `ROLE_SUPERVISOR`: Additional moderation permissions
- `ROLE_ADMIN`: Full administrative permissions

## Troubleshooting

### Invalid Credentials

- Ensure username and password are correct
- Check if the account has been verified

### Token Expired

- If access token is expired, use the refresh token to get a new one
- If refresh token is expired, you must log in again

### JWT Malformed

- Ensure the token is correctly formatted
- Ensure the token is being sent in the correct header format
