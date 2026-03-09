# Auth Service API Documentation

Complete API reference for the Auth Service authentication endpoints.

## Base URL

```
Production: https://auth.example.com
Development: http://localhost:8080
```

## Authentication

Most endpoints require authentication using JWT access tokens:

```http
Authorization: Bearer <access_token>
```

## Content Type

All requests should use `Content-Type: application/json`.

---

## Endpoints

### Authentication Endpoints

#### POST /api/v1/auth/login

Authenticate user with username and password.

**Request:**

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "john_doe",
  "password": "SecurePassword123!"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| username | string | Yes | Username (3-255 characters) |
| password | string | Yes | Password (8-255 characters) |

**Success Response (200 OK):**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0yMDI2LTAzLTAyLXYxIn0...",
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
  "token_type": "Bearer",
  "expires_in": 1800
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| access_token | string | JWT access token (RS256) |
| refresh_token | string | Refresh token UUID |
| token_type | string | Token type (always "Bearer") |
| expires_in | number | Access token expiry in seconds (1800 = 30 minutes) |

**Error Responses:**

| Code | Description | Response |
|------|-------------|----------|
| 400 | Bad Request | `{"error": "Invalid request format"}` |
| 401 | Unauthorized | `{"error": "Invalid username or password"}` |
| 429 | Too Many Requests | `{"error": "Too many login attempts. Try again later."}` |
| 500 | Internal Server Error | `{"error": "Internal server error"}` |

**Rate Limiting:**

- 5 attempts per IP address per minute
- Rate limit headers included in response:

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
X-RateLimit-Reset: 1641234567
```

---

#### POST /api/v1/auth/refresh

Refresh access token using refresh token.

**Request:**

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
  "idempotency_key": "optional-uuid-for-concurrent-requests"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refresh_token | string | Yes | Valid refresh token UUID |
| idempotency_key | string | No | Unique key for concurrent request handling |

**Success Response (200 OK):**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0yMDI2LTAzLTAyLXYxIn0...",
  "refresh_token": "660e8400-e29b-41d4-a716-446655440001",
  "token_type": "Bearer",
  "expires_in": 1800
}
```

**Behavior:**

1. Validates refresh token against database
2. Issues new access token
3. **Rotates refresh token** (old token invalidated)
4. Returns new token pair

**Error Responses:**

| Code | Description | Response |
|------|-------------|----------|
| 400 | Bad Request | `{"error": "Invalid request format"}` |
| 401 | Unauthorized | `{"error": "Invalid or expired refresh token"}` |
| 429 | Too Many Requests | `{"error": "Too many refresh attempts. Try again later."}` |

**Rate Limiting:**

- 10 refresh attempts per user per minute

---

#### POST /api/v1/auth/logout

Logout user by invalidating refresh token.

**Request:**

```http
POST /api/v1/auth/logout
Content-Type: application/json
```

```json
{
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refresh_token | string | Yes | Refresh token to revoke |

**Success Response (200 OK):**

```json
{
  "message": "Logged out successfully"
}
```

**Behavior:**

1. Invalidates refresh token in database
2. Optionally adds access token JTI to blacklist
3. Access token remains valid until expiry (30 minutes)

**Error Responses:**

| Code | Description | Response |
|------|-------------|----------|
| 400 | Bad Request | `{"error": "Invalid request format"}` |
| 401 | Unauthorized | `{"error": "Invalid refresh token"}` |

---

### Password Reset Endpoints

#### POST /api/v1/auth/password-reset/request

Request password reset email.

**Request:**

```http
POST /api/v1/auth/password-reset/request
Content-Type: application/json
```

```json
{
  "email": "user@example.com"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | string | Yes | User email address |

**Success Response (200 OK):**

```json
{
  "message": "Password reset email sent if account exists"
}
```

**Behavior:**

1. Generates reset token (UUID, 1 hour expiry)
2. Sends email with reset link
3. Always returns success (prevents email enumeration)

**Error Responses:**

| Code | Description | Response |
|------|-------------|----------|
| 400 | Bad Request | `{"error": "Invalid email format"}` |
| 429 | Too Many Requests | `{"error": "Too many reset attempts. Try again later."}` |
| 500 | Internal Server Error | `{"error": "Failed to send reset email"}` |

**Rate Limiting:**

- 3 attempts per email per hour

---

#### POST /api/v1/auth/password-reset/confirm

Confirm password reset with token.

**Request:**

```http
POST /api/v1/auth/password-reset/confirm
Content-Type: application/json
```

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "new_password": "NewSecurePassword123!"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| token | string | Yes | Password reset token UUID |
| new_password | string | Yes | New password (8-255 characters) |

**Success Response (200 OK):**

```json
{
  "message": "Password reset successfully"
}
```

**Behavior:**

1. Validates reset token
2. Updates password hash
3. Invalidates all existing refresh tokens for user
4. Deletes reset token
5. Sends confirmation email

**Error Responses:**

| Code | Description | Response |
|------|-------------|----------|
| 400 | Bad Request | `{"error": "Invalid request format"}` |
| 401 | Unauthorized | `{"error": "Invalid or expired reset token"}` |
| 404 | Not Found | `{"error": "User not found"}` |

---

### JWKS Endpoint

#### GET /.well-known/jwks.json

Public keys for JWT validation (used by API Gateway).

**Request:**

```http
GET /.well-known/jwks.json
```

**Success Response (200 OK):**

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "key-2026-03-02-v1",
      "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDRQ...",
      "e": "AQAB",
      "alg": "RS256",
      "use": "sig"
    }
  ]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| keys | array | Array of public keys |
| kty | string | Key type (RSA) |
| kid | string | Key identifier |
| n | string | Modulus (base64url) |
| e | string | Exponent (base64url) |
| alg | string | Algorithm (RS256) |
| use | string | Public key use (sig) |

**Usage:**

API Gateway fetches this endpoint on startup and caches public keys for local JWT validation.

---

### OAuth2 Endpoints

#### GET /oauth2/authorization/{provider}

Initiate OAuth2 authorization flow.

**Request:**

```http
GET /oauth2/authorization/google
# or
GET /oauth2/authorization/github
```

**Response:**

```http
HTTP 302 Found
Location: https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&response_type=code&scope=...
```

**Supported Providers:**

- `google` - Google OAuth2
- `github` - GitHub OAuth2

---

#### GET /login/oauth2/code/{provider}

OAuth2 callback endpoint (internal, redirects to frontend).

**Request:**

```http
GET /login/oauth2/code/google?code=4/0AX4XfWh...&state=...
```

**Response:**

```http
HTTP 302 Found
Location: https://frontend.com/auth/callback?access_token=eyJh...&refresh_token=...&expires_in=1800
```

**Behavior:**

1. Receives OAuth2 authorization code
2. Exchanges code for user profile
3. Creates or links user account
4. Generates JWT tokens
5. Redirects to frontend with tokens in URL hash

**Frontend Handling:**

Frontend should extract tokens from URL hash and store securely:

```javascript
// Extract tokens from URL hash
const hash = window.location.hash.substring(1);
const params = new URLSearchParams(hash);
const accessToken = params.get('access_token');
const refreshToken = params.get('refresh_token');

// Store securely
localStorage.setItem('access_token', accessToken);
localStorage.setItem('refresh_token', refreshToken);

// Clear URL hash
window.history.replaceState({}, document.title, window.location.pathname);
```

---

### Health Check Endpoint

#### GET /actuator/health

Health check for load balancers/orchestrators.

**Request:**

```http
GET /actuator/health
```

**Success Response (200 OK):**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "path": "/app/.",
        "exists": true
      }
    },
    "keyStore": {
      "status": "UP",
      "details": {
        "keyId": "key-2026-03-02-v1"
      }
    }
  }
}
```

**Response Fields:**

| Component | Description |
|-----------|-------------|
| status | Overall health status (UP or DOWN) |
| components.db | Database connection status |
| components.diskSpace | Disk space status |
| components.keyStore | JWT key store status |

**Usage:**

```bash
# Check health
curl http://localhost:8080/actuator/health

# Expected output: {"status":"UP"}
```

---

## JWT Token Structure

### Access Token

Access tokens are JWTs signed with RS256.

**Header:**

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-2026-03-02-v1"
}
```

**Payload:**

```json
{
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "sub": "user-uuid",
  "username": "john_doe",
  "email": "john@example.com",
  "auth_type": "password",
  "iat": 1709376000,
  "exp": 1709377800,
  "iss": "auth-service",
  "aud": "api-gateway"
}
```

**Claims:**

| Claim | Description |
|-------|-------------|
| jti | JWT ID (unique identifier) |
| sub | Subject (user ID) |
| username | Username |
| email | User email |
| auth_type | Authentication type (password or oauth2) |
| iat | Issued at (Unix timestamp) |
| exp | Expiration (Unix timestamp) |
| iss | Issuer (auth-service) |
| aud | Audience (api-gateway) |

### Token Validation (API Gateway)

API Gateway validates access tokens locally:

```java
// 1. Fetch public keys from JWKS endpoint
Map<String, Object> jwks = fetchJwks("http://auth-service/.well-known/jwks.json");

// 2. Verify JWT signature
PublicKey publicKey = getPublicKeyFromJwks(jwks, jwt.getHeader().getKeyId());
Jws<Claims> claims = Jwts.parserBuilder()
    .setSigningKey(publicKey)
    .build()
    .parseClaimsJws(jwt);

// 3. Check expiry (with 30s clock skew tolerance)
Date now = new Date();
Date expiry = claims.getBody().getExpiration();
if (expiry.before(new Date(now.getTime() - 30000))) {
    throw new TokenExpiredException();
}

// 4. Extract user claims
String userId = claims.getBody().getSubject();
String username = claims.getBody().get("username", String.class);
```

---

## Error Codes

### Standard Error Response

All error responses follow this format:

```json
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "timestamp": "2026-03-02T10:30:00Z",
  "path": "/api/v1/auth/login"
}
```

### Error Codes Reference

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | Request format is invalid |
| `INVALID_CREDENTIALS` | 401 | Username or password is incorrect |
| `INVALID_TOKEN` | 401 | Token is invalid or expired |
| `TOKEN_EXPIRED` | 401 | Token has expired |
| `TOKEN_REVOKED` | 401 | Token has been revoked |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `USER_DISABLED` | 403 | User account is disabled |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |
| `SERVICE_UNAVAILABLE` | 503 | Service is temporarily unavailable |

---

## Rate Limiting

### Rate Limit Headers

All rate-limited endpoints include these headers:

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
X-RateLimit-Reset: 1641234567
X-RateLimit-Reset-InSeconds: 45
```

**Header Descriptions:**

| Header | Description |
|--------|-------------|
| X-RateLimit-Limit | Request limit per window |
| X-RateLimit-Remaining | Remaining requests in current window |
| X-RateLimit-Reset | Unix timestamp when limit resets |
| X-RateLimit-Reset-InSeconds | Seconds until limit resets |

### Rate Limit Rules

| Endpoint | Limit | Window | Key |
|----------|-------|--------|-----|
| POST /login | 5 | 1 minute | IP address |
| POST /refresh | 10 | 1 minute | User ID |
| POST /password-reset/request | 3 | 1 hour | Email |

### Handling Rate Limits

When rate limited, wait until the reset time before retrying:

```javascript
// Handle rate limit
try {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials)
  });

  if (response.status === 429) {
    const resetTime = response.headers.get('X-RateLimit-Reset');
    const waitSeconds = resetTime - Math.floor(Date.now() / 1000);
    console.log(`Rate limited. Retry in ${waitSeconds} seconds`);
    // Schedule retry
    setTimeout(() => login(), waitSeconds * 1000);
  }
} catch (error) {
  console.error('Login failed:', error);
}
```

---

## OpenAPI Specification

### Interactive Documentation

When the service is running, access interactive Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

### Raw OpenAPI Spec

```
http://localhost:8080/v3/api-docs
```

### Download OpenAPI Spec

```bash
# Download OpenAPI JSON
curl http://localhost:8080/v3/api-docs > openapi.json

# Download OpenAPI YAML
curl http://localhost:8080/v3/api-docs.yaml > openapi.yaml
```

---

## Code Examples

### cURL Examples

#### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123!"
  }'
```

#### Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

#### Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

#### Fetch JWKS

```bash
curl http://localhost:8080/.well-known/jwks.json
```

### JavaScript/TypeScript Examples

#### Login with Fetch

```typescript
interface LoginResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

async function login(username: string, password: string): Promise<LoginResponse> {
  const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    throw new Error(`Login failed: ${response.statusText}`);
  }

  return await response.json();
}

// Usage
const tokens = await login('john_doe', 'SecurePassword123!');
localStorage.setItem('access_token', tokens.access_token);
localStorage.setItem('refresh_token', tokens.refresh_token);
```

#### Token Refresh with Axios

```typescript
import axios from 'axios';

interface AuthResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

async function refreshToken(refreshToken: string): Promise<AuthResponse> {
  const response = await axios.post(
    'http://localhost:8080/api/v1/auth/refresh',
    { refresh_token: refreshToken }
  );
  return response.data;
}

// Usage with interceptor
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        try {
          const tokens = await refreshToken(refreshToken);
          localStorage.setItem('access_token', tokens.access_token);
          localStorage.setItem('refresh_token', tokens.refresh_token);
          // Retry original request
          error.config.headers.Authorization = `Bearer ${tokens.access_token}`;
          return axios.request(error.config);
        } catch (refreshError) {
          // Redirect to login
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

### Python Examples

#### Login with Requests

```python
import requests

def login(username: str, password: str) -> dict:
    response = requests.post(
        'http://localhost:8080/api/v1/auth/login',
        json={
            'username': username,
            'password': password
        }
    )
    response.raise_for_status()
    return response.json()

# Usage
tokens = login('john_doe', 'SecurePassword123!')
access_token = tokens['access_token']
refresh_token = tokens['refresh_token']

# Use access token
headers = {'Authorization': f'Bearer {access_token}'}
response = requests.get('http://api-gateway/resource', headers=headers)
```

---

## Testing

### Integration Tests

```bash
# Test login flow
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}' \
  | jq .

# Test refresh flow
REFRESH_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}' \
  | jq -r '.refresh_token')

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\":\"$REFRESH_TOKEN\"}" \
  | jq .

# Test JWKS endpoint
curl http://localhost:8080/.well-known/jwks.json | jq .

# Test health check
curl http://localhost:8080/actuator/health | jq .
```

### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 10 -T 'application/json' \
  -p login.json \
  http://localhost:8080/api/v1/auth/login

# Using wrk
wrk -t4 -c100 -d30s \
  -s login.lua \
  http://localhost:8080/api/v1/auth/login
```

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for API version history.
