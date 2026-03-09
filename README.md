# Auth Service

A robust authentication and authorization service providing JWT-based token authentication for microservices architectures.

## Overview

Auth Service is a Spring Boot 3.2 application that provides secure authentication with JWT tokens, OAuth2 social login integration, and comprehensive token management capabilities. It's designed to work seamlessly with API Gateways and downstream services in a microservices architecture.

## Features

- **JWT-based Authentication** - RS256 asymmetric encryption for secure token validation
- **Token Refresh with Rotation** - Automatic refresh token rotation for enhanced security
- **Multi-Device Support** - Support up to 5 active refresh tokens per user
- **OAuth2 Integration** - Social login with Google, GitHub (extensible)
- **Password Reset Flow** - Secure email-based password reset
- **Token Blacklist** - Immediate token revocation capability
- **Rate Limiting** - Built-in protection against brute force attacks
- **Health Monitoring** - Actuator endpoints for operational visibility
- **JWKS Endpoint** - Standard public key distribution for API Gateway

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 3.2+ |
| **Language** | Java | 21 |
| **Build Tool** | Maven | 3.9+ |
| **Database** | PostgreSQL | 15+ |
| **ORM** | Spring Data JPA | - |
| **Migrations** | Flyway | - |
| **Security** | Spring Security + JJWT | - |
| **Testing** | JUnit 5, TestContainers, Mockito | - |
| **API Docs** | SpringDoc OpenAPI | - |
| **Container** | Docker | - |

## Prerequisites

- **Java 21** - [Download JDK](https://adoptium.net/)
- **Maven 3.9+** - [Download Maven](https://maven.apache.org/download.cgi)
- **Docker** - [Install Docker](https://docs.docker.com/get-docker/)
- **PostgreSQL 15+** - For local development (or use Docker)

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/Tinkerc/vibe-coding.git
cd vibe-coding/auth-service

# Create environment file
cat > .env << EOF
DB_PASSWORD=your_secure_password
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
FRONTEND_URL=http://localhost:3000
EOF

# Start services
docker-compose up -d

# Check health
curl http://localhost:8080/actuator/health
```

### Manual Setup

```bash
# Install dependencies
mvn clean install

# Start PostgreSQL (using Docker)
docker run -d \
  --name auth-postgres \
  -e POSTGRES_DB=authdb \
  -e POSTGRES_USER=authuser \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export DB_PASSWORD=postgres
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=authdb

# Run the application
mvn spring-boot:run
```

## Development Setup

### 1. Configure OAuth2 Applications

**Google OAuth2:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add redirect URI: `http://localhost:8080/login/oauth2/code/google`
6. Copy Client ID and Secret

**GitHub OAuth2:**
1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Set Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Copy Client ID and Secret

### 2. Configure Email (Optional)

For password reset functionality, configure SMTP:

```yaml
# application-dev.yml
email:
  enabled: true
  smtp:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

### 3. Initialize Database

```bash
# Flyway migrations run automatically on startup
# Or run manually:
mvn flyway:migrate
```

### 4. Run Tests

```bash
# Unit tests (fast)
mvn test

# Integration tests (requires Docker)
mvn verify

# All tests
mvn clean verify
```

## Running Tests

### Unit Tests

```bash
mvn test
```

Unit tests use H2 in-memory database and run quickly (< 30 seconds).

### Integration Tests

```bash
mvn verify -P integration
```

Integration tests use TestContainers with PostgreSQL and require Docker running.

### Test Coverage

```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

Target coverage: 80%+ for service layer.

## Docker Deployment

### Production Build

```bash
# Build JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t auth-service:1.0.0 .

# Run container
docker run -d \
  --name auth-service \
  -p 8080:8080 \
  -v $(pwd)/keys:/opt/auth-service/keys \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PASSWORD=secure_password \
  -e DB_HOST=postgres.example.com \
  -e GOOGLE_CLIENT_ID=xxx \
  -e GOOGLE_CLIENT_SECRET=xxx \
  auth-service:1.0.0
```

### Docker Compose (Production)

```bash
# Create production compose file
cat > docker-compose.prod.yml << EOF
version: '3.8'
services:
  auth-service:
    image: auth-service:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_PASSWORD=\${DB_PASSWORD}
      - GOOGLE_CLIENT_ID=\${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=\${GOOGLE_CLIENT_SECRET}
    volumes:
      - ./keys:/opt/auth-service/keys:rw
    restart: unless-stopped
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=authdb
      - POSTGRES_USER=authuser
      - POSTGRES_PASSWORD=\${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  pgdata:
EOF

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

## API Documentation

### Interactive API Docs

Once the service is running, access the interactive Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Spec

Raw OpenAPI specification available at:

```
http://localhost:8080/v3/api-docs
```

### JWKS Endpoint

Public keys for JWT validation:

```
http://localhost:8080/.well-known/jwks.json
```

### Health Check

Service health status:

```
http://localhost:8080/actuator/health
```

## Key API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | User login with password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout (revoke token) |
| POST | `/api/v1/auth/password-reset/request` | Request password reset |
| POST | `/api/v1/auth/password-reset/confirm` | Confirm password reset |
| GET | `/.well-known/jwks.json` | JWKS public keys |
| GET | `/oauth2/authorization/{provider}` | Initiate OAuth2 flow |

For detailed API documentation, see [docs/API.md](docs/API.md)

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Active profile | dev | No |
| `DB_HOST` | Database host | localhost | No |
| `DB_PORT` | Database port | 5432 | No |
| `DB_NAME` | Database name | authdb | No |
| `DB_PASSWORD` | Database password | - | Yes |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | - | No* |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 secret | - | No* |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 client ID | - | No* |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 secret | - | No* |
| `FRONTEND_URL` | Frontend URL for OAuth2 redirects | - | Yes** |
| `SMTP_HOST` | SMTP server host | - | No*** |
| `SMTP_PORT` | SMTP server port | 587 | No*** |
| `SMTP_USERNAME` | SMTP username | - | No*** |
| `SMTP_PASSWORD` | SMTP password | - | No*** |

*Required for OAuth2 functionality
**Required for OAuth2 callback
***Required for password reset emails

## Contributing

We welcome contributions! Please follow these guidelines:

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Write tests (TDD approach)
4. Implement your feature
5. Ensure all tests pass: `mvn verify`
6. Commit with conventional commits: `feat(scope): description`
7. Push and create a pull request

### Code Style

- Follow Java code conventions
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Add Javadoc for public APIs
- Write unit tests for all new features

### Commit Conventions

```
feat(auth): add OAuth2 support
fix(security): patch token validation vulnerability
test(service): add refresh token rotation tests
docs(api): update API documentation
refactor(jwt): simplify token generation
```

### Testing Requirements

- Unit tests: 80%+ coverage
- Integration tests: For all API endpoints
- No failing tests allowed in PR

## Documentation

- [API Documentation](docs/API.md) - Complete API reference
- [Deployment Guide](docs/DEPLOYMENT.md) - Production deployment
- [Development Guide](docs/DEVELOPMENT.md) - Development workflow
- [Changelog](docs/CHANGELOG.md) - Version history

## Architecture

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   CLIENT     │◄────────┤  API GATEWAY │◄────────┤   RESOURCE   │
│              │         │              │         │   SERVICES   │
└──────────────┘         └──────┬───────┘         └──────────────┘
                               │ JWKS fetch
                               │ Local JWT validation
                               │
                      ┌────────▼────────┐
                      │  AUTH SERVICE   │
                      │  ┌───────────┐  │
                      │  │   JWKS    │  │
                      │  │  Endpoint │  │
                      │  └───────────┘  │
                      └────────┬────────┘
                               │
                      ┌────────▼────────┐
                      │   PostgreSQL    │
                      │                 │
                      └─────────────────┘

┌──────────────┐         ┌──────────────┐
│   Google     │         │    GitHub    │
│   OAuth2     │         │    OAuth2    │
└──────────────┘         └──────────────┘
```

## Security Features

- **RS256 JWT** - Asymmetric encryption for token validation
- **Token Rotation** - Automatic refresh token rotation
- **Token Blacklist** - Immediate token revocation
- **Rate Limiting** - Brute force protection
- **Password Hashing** - bcrypt with work factor 12
- **Key Rotation** - 90-day automatic rotation
- **TLS Ready** - Configure for production

## Performance

| Operation | Target | Measured |
|-----------|--------|----------|
| Login | < 500ms | ~200ms |
| Token Validation | < 10ms | ~3ms (local) |
| Token Refresh | < 200ms | ~80ms |
| Logout | < 100ms | ~40ms |

## Monitoring

### Actuator Endpoints

```
/actuator/health          - Health status
/actuator/metrics         - Metrics (Micrometer)
/actuator/info           - Application info
```

### Key Metrics

- Login success rate
- Token validation latency
- Database connection pool usage
- Failed login attempts
- Active refresh tokens per user

## Troubleshooting

### Common Issues

**Database connection fails**
- Check PostgreSQL is running: `docker ps | grep postgres`
- Verify connection string in application.yml
- Check network connectivity

**OAuth2 callback fails**
- Verify redirect URI matches OAuth app configuration
- Check FRONTEND_URL environment variable
- Review OAuth app credentials

**JWT validation fails**
- Verify JWKS endpoint is accessible
- Check clock synchronization (use NTP)
- Verify key ID matches

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for more troubleshooting.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- Documentation: [docs/](docs/)
- Issues: [GitHub Issues](https://github.com/Tinkerc/vibe-coding/issues)
- API Documentation: http://localhost:8080/swagger-ui.html

## Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [JJWT](https://github.com/jwtk/jjwt)
- [TestContainers](https://www.testcontainers.org/)
