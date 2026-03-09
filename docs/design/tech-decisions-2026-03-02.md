# Technical Decisions - Auth Service

**Date:** 2026-03-02
**Status:** CONFIRMED
**Project:** Auth Service MVP

---

## Final Technology Stack

| Category | Choice | Version | Rationale |
|----------|--------|---------|-----------|
| **Language** | Java | 21 LTS | Latest LTS, virtual threads, pattern matching |
| **Framework** | Spring Boot | 3.2+ | Modern Spring stack, native support |
| **Security** | Spring Security | 6.2+ | OAuth2/OIDC built-in support |
| **Database** | PostgreSQL | 15+ | Chosen in requirements |
| **ORM** | Spring Data JPA | 3.2+ | Hibernate integration |
| **Build Tool** | Maven | 3.9+ | User selected |
| **Testing** | JUnit 5 + TestContainers | - | Integration testing with real DB |
| **API Docs** | SpringDoc OpenAPI | 2.3+ | OpenAPI 3 for Spring Boot 3 |
| **Rate Limiting** | Bucket4j | 8.7+ | Token bucket algorithm |
| **Deployment** | Local | - | Embedded Tomcat |

---

## Architecture Decisions

### 1. Token Validation Strategy

**Decision:** Local JWT validation at API Gateway

**Rationale:**
- JWT is stateless by design
- Eliminates inter-service call on every request
- Target latency < 10ms achievable locally

**Trade-off:**
- Access tokens cannot be immediately revoked
- Use short expiry (15 min) + token blacklist for critical cases

### 2. Refresh Token Management

**Decision:** Database-backed with rotation

**Rationale:**
- Enables revocation and abuse detection
- Rotation prevents replay attacks
- Supports multi-device (max 5 tokens per user)

**Storage:**
- Hashed (SHA-256) in PostgreSQL
- Device fingerprinting for detection

### 3. OAuth2/OIDC Integration

**Decision:** Spring Security OAuth2 Client

**Providers:**
| Provider | Priority | Type |
|----------|----------|------|
| Google | P0 | OIDC |
| GitHub | P0 | OAuth2 |

**Flow:**
- Authorization Code flow with PKCE
- Custom success handler returns JWT tokens
- Auto-provision user accounts on first login

### 4. JWT Signing

**Decision:** RS256 with RSA key pair

**Key Management:**
- 2048-bit RSA keys
- File-based storage (`/opt/auth-service/keys/`)
- Rotation every 90 days
- 15-minute grace period for old key

### 5. Rate Limiting

**Decision:** Bucket4j with in-memory cache

**Limits:**
- Login: 5 attempts/IP/minute
- Refresh: 10 attempts/user/minute
- Default: 100 requests/minute

**Implementation:**
- Caffeine cache for bucket storage
- Filter-based for all endpoints
- IP-based for login, user-based for refresh

### 6. Database Schema

**Design:**
- UUID primary keys
- Soft delete support (`deleted_at`)
- OAuth provider + subject_id composite index
- Token expiry cleanup via scheduled task

### 7. Security Configuration

**Stateless Sessions:**
- `SessionCreationPolicy.STATELESS`
- No HTTP session storage
- JWT in Authorization header only

**CORS:**
- Configured for frontend origin
- Credentials not supported (JWT pattern)

---

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/vibe/auth/
│   │   │   ├── AuthApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── OAuth2Config.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── RateLimitConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   └── HealthController.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── TokenService.java
│   │   │   │   └── OAuth2Service.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── TokenBlacklistRepository.java
│   │   │   ├── model/
│   │   │   │   ├── User.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── TokenBlacklist.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   └── PasswordResetRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── AuthResponse.java
│   │   │   │       └── TokenResponse.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── OAuth2AuthenticationSuccessHandler.java
│   │   │   ├── exception/
│   │   │   │   ├── AuthException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── filter/
│   │   │       ├── JwtAuthenticationFilter.java
│   │   │       └── RateLimitFilter.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/vibe/auth/
│           ├── service/
│           ├── controller/
│           └── integration/
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

---

## Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot 3.2 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
    </dependency>

    <!-- Rate Limiting -->
    <dependency>
        <groupId>com.bucket4j</groupId>
        <artifactId>bucket4j-core</artifactId>
        <version>8.7.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Configuration

### application.yml

```yaml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: authuser
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8080

jwt:
  expiration: 900  # 15 minutes
  refresh-expiration: 604800  # 7 days
  key-path: /opt/auth-service/keys

oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}

rate-limit:
  login: 5
  refresh: 10
  window-minutes: 1

logging:
  level:
    com.vibe.auth: DEBUG
    org.springframework.security: DEBUG
```

---

## Open Questions (Resolved)

| Question | Decision |
|----------|----------|
| Java Version | 21 LTS |
| Build Tool | Maven |
| Rate Limiting | Bucket4j |
| Key Storage | File-based |
| OAuth2 Providers | Google, GitHub |

---

**Next Step:** Step 2.3 - Solution Design
