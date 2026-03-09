# Auth Service - Expanded Requirements

**Date:** 2026-03-02
**Status:** Expanded
**Input:** auth-flow.md

## Overview

Authentication and authorization service providing token-based authentication for API Gateway and downstream services.

## Functional Requirements

### FR1: User Login

**Endpoint:** `POST /login`

**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Success Response (200 OK):**
```json
{
  "access_token": "string (JWT)",
  "refresh_token": "string",
  "expires_in": "number (seconds)"
}
```

**Behavior:**
- Validate user credentials against User Database
- Verify password hash
- Generate JWT Access Token
- Generate Refresh Token
- Return both tokens with expiry time

### FR2: Token Validation

**Endpoint:** Internal (called by API Gateway)

**Request:**
```json
{
  "access_token": "string (JWT)"
}
```

**Success Response (200 OK):**
```json
{
  "valid": true,
  "user_id": "string",
  "exp": "number (timestamp)"
}
```

**Behavior:**
- Verify JWT signature
- Check token expiry
- Return token validity and user context

### FR3: Token Refresh

**Endpoint:** `POST /refresh`

**Request:**
```json
{
  "refresh_token": "string"
}
```

**Success Response (200 OK):**
```json
{
  "access_token": "string (JWT)",
  "expires_in": "number (seconds)"
}
```

**Behavior:**
- Validate refresh token against User Database
- Generate new Access Token
- Return new access token with expiry

### FR4: User Logout

**Endpoint:** `POST /logout`

**Request:**
```json
{
  "refresh_token": "string"
}
```

**Success Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Behavior:**
- Delete or blacklist refresh token in User Database
- Invalidate refresh token

## Non-Functional Requirements

### NFR1: Security

- Passwords stored as hashes (bcrypt/scrypt/argon2)
- JWT signed with RS256 or HS256
- Access token expiry: 15 minutes
- Refresh token expiry: 7 days
- HTTPS required for all endpoints

### NFR2: Performance

- Login latency: < 500ms
- Token validation latency: < 50ms
- Token refresh latency: < 200ms
- Support 1000 concurrent requests

### NFR3: Availability

- Uptime: 99.9%
- Graceful degradation if database is unavailable
- Token validation should work with local cache if DB unavailable

### NFR4: Scalability

- Stateless token validation (JWT)
- Horizontal scaling capability
- Database connection pooling

## Architecture Components

### API Gateway

- Routes client requests
- Validates JWT tokens on protected routes
- Forwards valid requests to downstream services
- Handles token refresh flow

### Auth Service

- Core authentication logic
- Token generation and validation
- Password verification
- Refresh token management

### User Database

- Stores user records
- Stores password hashes
- Stores refresh tokens
- Token blacklist for logout

### Resource Service

- Downstream service that requires authentication
- Receives validated requests from API Gateway

## Data Models

### User Record

```json
{
  "user_id": "string (UUID)",
  "username": "string (unique)",
  "password_hash": "string",
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

### Refresh Token

```json
{
  "token_id": "string (UUID)",
  "user_id": "string (UUID)",
  "token_hash": "string",
  "expires_at": "timestamp",
  "revoked": "boolean"
}
```

### Token Blacklist

```json
{
  "token_jti": "string (JWT ID)",
  "revoked_at": "timestamp",
  "expires_at": "timestamp"
}
```

## API Contracts

### Login API

```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "username": "string",
  "password": "string"
}

Response 200:
{
  "access_token": "string",
  "refresh_token": "string",
  "token_type": "Bearer",
  "expires_in": 900
}

Response 401:
{
  "error": "Invalid credentials"
}

Response 500:
{
  "error": "Internal server error"
}
```

### Token Validation API (Internal)

```
POST /api/v1/auth/validate
Authorization: Bearer {access_token}

Response 200:
{
  "valid": true,
  "user_id": "string",
  "username": "string",
  "exp": 1234567890
}

Response 401:
{
  "valid": false,
  "error": "Invalid or expired token"
}
```

### Refresh Token API

```
POST /api/v1/auth/refresh
Content-Type: application/json

Request:
{
  "refresh_token": "string"
}

Response 200:
{
  "access_token": "string",
  "token_type": "Bearer",
  "expires_in": 900
}

Response 401:
{
  "error": "Invalid refresh token"
}
```

### Logout API

```
POST /api/v1/auth/logout
Content-Type: application/json

Request:
{
  "refresh_token": "string"
}

Response 200:
{
  "message": "Logged out successfully"
}
```

## Flows

### Login Flow

1. Client submits username/password to API Gateway
2. API Gateway forwards to Auth Service
3. Auth Service queries User Database for user record
4. Auth Service verifies password hash
5. Auth Service generates JWT Access Token (15 min expiry)
6. Auth Service generates Refresh Token (7 day expiry)
7. Auth Service stores refresh token in database
8. Auth Service returns both tokens to API Gateway
9. API Gateway returns tokens to Client
10. Client stores tokens securely

### Authenticated Request Flow

1. Client includes Access Token in Authorization header
2. API Gateway extracts token
3. API Gateway calls Auth Service to validate token
4. Auth Service verifies signature and expiry
5. Auth Service returns token validity and user context
6. API Gateway forwards request to Resource Service
7. Resource Service returns data
8. API Gateway returns data to Client

### Token Refresh Flow

1. Client sends refresh token to API Gateway
2. API Gateway forwards to Auth Service
3. Auth Service validates refresh token against database
4. Auth Service generates new Access Token
5. Auth Service returns new Access Token to API Gateway
6. API Gateway returns new Access Token to Client
7. Client replaces stored Access Token

### Logout Flow

1. Client sends refresh token to API Gateway
2. API Gateway forwards to Auth Service
3. Auth Service deletes or blacklists refresh token in database
4. Auth Service confirms logout
5. API Gateway confirms logout to Client
6. Client removes stored tokens

## Error Handling

| Error Code | Description | HTTP Status |
|------------|-------------|-------------|
| AUTH_001 | Invalid credentials | 401 |
| AUTH_002 | Invalid token | 401 |
| AUTH_003 | Expired token | 401 |
| AUTH_004 | Invalid refresh token | 401 |
| AUTH_005 | Token already revoked | 401 |
| AUTH_006 | User not found | 404 |
| AUTH_007 | Database error | 500 |
| AUTH_008 | Token generation failed | 500 |

## Open Questions

1. Should we support multiple active refresh tokens per user?
2. Should we implement token rotation on refresh?
3. Should we support social login (OAuth2, SAML)?
4. Should we implement rate limiting?
5. Should we support MFA (Multi-Factor Authentication)?
6. Database technology selection?
7. JWT signing algorithm (RS256 vs HS256)?
8. Secret management approach?
