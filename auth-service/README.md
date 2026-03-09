# Auth Service

Authentication and Authorization Service with JWT and OAuth2 support.

## Security Considerations

### Phase 1 Limitations

**IMPORTANT:** This is Phase 1 MVP. The following security features are **NOT YET IMPLEMENTED**:

- **Rate Limiting:** API endpoints are not rate-limited by the application itself.
  - **Workaround:** Deploy behind a rate-limiting proxy (Nginx, API Gateway, or cloud provider level)
  - **Planned:** Built-in rate limiting using Bucket4j in Phase 2

- **Clock Skew Tolerance:** JWT validation does not account for clock skew.
  - **Workaround:** Ensure all services use NTP for time synchronization
  - **Planned:** ±30 second tolerance in Phase 2

### Security Features Implemented ✓

- **Password Hashing:** BCrypt with work factor 12
- **JWT Signing:** RS256 with 2048-bit RSA keys
- **Token Rotation:** Refresh tokens rotate on every refresh
- **Token Revocation:** Logout properly invalidates refresh tokens
- **JWKS Endpoint:** Public key endpoint for local JWT validation

### Deployment Recommendations

For production deployment, ensure:
1. **HTTPS/TLS** is enabled for all endpoints
2. **Rate limiting** is configured at infrastructure level until Phase 2
3. **NTP synchronization** is configured across all services
4. **Secret management** uses proper key storage (not file-based for production)
5. **API Gateway** is configured with rate limiting rules

## Features

- JWT-based stateless authentication (RS256)
- Refresh token rotation
- OAuth2 login (Google, GitHub) - *Phase 2*
- Token blacklist for immediate revocation
- Rate limiting - *Phase 2*
- Health checks

## Tech Stack

- Java 21 LTS
- Spring Boot 3.2+
- Spring Security 6.2+
- PostgreSQL 15+
- Maven

## Quick Start

### Prerequisites

- Java 21 JDK
- Maven 3.9+
- PostgreSQL 15+
- Docker Desktop (for TestContainers)

### Database Setup

```sql
CREATE DATABASE authdb_dev;
CREATE USER authuser WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE authdb_dev TO authuser;
```

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

## API Endpoints

### Authentication

#### Login
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "uuid-token-here",
  "token_type": "Bearer",
  "expires_in": 1800
}
```

#### Refresh Token
```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refresh_token": "your-refresh-token",
  "idempotency_key": "optional-key-for-concurrent-requests"
}
```

#### Logout
```bash
POST /api/v1/auth/logout
Refresh-Token: your-refresh-token
```

### JWKS Endpoint

Get public key for JWT validation:
```bash
GET /.well-known/jwks.json
```

### Health Check
```bash
GET /actuator/health
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration
```

### All Tests
```bash
mvn verify
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_KEY_PATH` | RSA key storage path | `/opt/auth-service/keys` |
| `JWT_ACCESS_EXPIRY` | Access token expiry (seconds) | `1800` |
| `JWT_REFRESH_EXPIRY` | Refresh token expiry (seconds) | `604800` |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | - |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 secret | - |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 client ID | - |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 secret | - |

### Profiles

- `dev` - Development with HikariCP connection pooling
- `test` - Testing with H2 in-memory database
- `prod` - Production configuration

## Project Structure

```
auth-service/
├── src/main/java/com/vibe/auth/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── dto/             # Request/Response objects
│   ├── model/           # JPA entities
│   ├── repository/      # Data repositories
│   ├── security/        # Security components
│   ├── service/         # Business logic
│   └── exception/       # Exception handling
└── src/main/resources/
    └── application.yml  # Configuration
```

## Development

### API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

OpenAPI spec at: `http://localhost:8080/v3/api-docs`

### Key Management

RSA keys are auto-generated on first startup if they don't exist.
For production, ensure the key path is persisted:

```yaml
jwt:
  key-path: /opt/auth-service/keys
```

The service will:
1. Generate 2048-bit RSA key pair
2. Store in `{key-path}/private.key` and `{key-path}/public.key`
3. Track version in `{key-path}/key-version.txt`

## License

MIT License
