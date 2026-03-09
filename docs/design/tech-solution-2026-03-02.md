# Auth Service - Technical Solution (APPROVED)

**Date:** 2026-03-02
**Status:** READY
**Version:** 1.0
**Based on:** requirement-2026-03-02.md, tech-decisions-2026-03-02.md, design-review-2026-03-02.md

---

## 1. System Architecture

### 1.1 Component Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SYSTEM ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────────┐        │
│  │   CLIENT     │◄────────┤  API GATEWAY │◄────────┤   RESOURCE   │        │
│  │              │         │              │         │   SERVICES   │        │
│  └──────────────┘         └──────┬───────┘         └──────────────┘        │
│                                   │ JWKS fetch on startup                  │
│                                   │ Local JWT validation                   │
│                                   │                                         │
│                          ┌────────▼────────┐                                │
│                          │  AUTH SERVICE   │                                │
│                          │  ┌───────────┐  │                                │
│                          │  │   JWKS    │  │ ◄── Public key endpoint      │
│                          │  │  Endpoint │  │                                │
│                          │  └───────────┘  │                                │
│                          └────────┬────────┘                                │
│                                   │                                         │
│                          ┌────────▼────────┐                                │
│                          │   PostgreSQL    │                                │
│                          │   + Volume Mount│                                │
│                          └─────────────────┘                                │
│                                                                             │
│  ┌──────────────┐         ┌──────────────┐                                 │
│  │   Google     │         │    GitHub    │                                 │
│  │   OAuth2     │         │    OAuth2    │                                 │
│  └──────────────┘         └──────────────┘                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Improvements from Review

| Issue | Solution |
|-------|----------|
| Public key distribution | Added JWKS endpoint at `/.well-known/jwks.json` |
| Key persistence | Volume mount for `/opt/auth-service/keys` |
| OAuth2 callback | Redirect to frontend with tokens in URL hash |
| Database cleanup | Added `@EnableScheduling` with `@Scheduled` |
| Clock skew | Added NTP configuration to Docker Compose |
| Concurrent refresh | Added idempotency key to refresh request |

---

## 2. API Design

### 2.1 REST Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | /api/v1/auth/login | Password login | No |
| POST | /api/v1/auth/refresh | Refresh access token | No |
| POST | /api/v1/auth/logout | Logout (revoke refresh token) | No |
| POST | /api/v1/auth/password-reset/request | Request password reset | No |
| POST | /api/v1/auth/password-reset/confirm | Confirm password reset | No |
| **GET** | **/.well-known/jwks.json** | **JWKS endpoint for public key** | **No** |
| GET | /oauth2/authorization/{provider} | Initiate OAuth2 flow | No |
| GET | /login/oauth2/code/{provider} | OAuth2 callback → Frontend redirect | No |
| GET | /actuator/health | Health check | No |

### 2.2 Request/Response Schemas

#### 2.2.1 Login Request

```json
{
  "username": "string (3-255 chars)",
  "password": "string (8-255 chars)"
}
```

#### 2.2.2 Auth Response

```json
{
  "access_token": "string (JWT)",
  "refresh_token": "string",
  "token_type": "Bearer",
  "expires_in": 900
}
```

#### 2.2.3 Refresh Token Request (IMPROVED)

```json
{
  "refresh_token": "string",
  "idempotency_key": "string (optional, for concurrent requests)"
}
```

#### 2.2.4 OAuth2 Callback (IMPROVED)

OAuth2 callback now redirects to frontend:

```
HTTP 302 Redirect
Location: https://frontend.com/auth/callback?
  access_token=jwt&
  refresh_token=token&
  expires_in=900
```

---

## 3. Database Schema (UNCHANGED)

See DDL in tech-solution-draft. Added indexes for performance.

---

## 4. Security Design

### 4.1 JWKS Endpoint (NEW)

```java
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of(
            "keys", List.of(
                Map.of(
                    "kty", "RSA",
                    "kid", jwtTokenProvider.getKeyId(),
                    "n", jwtTokenProvider.getModulus(),
                    "e", jwtTokenProvider.getExponent(),
                    "alg", "RS256",
                    "use", "sig"
                )
            )
        );
    }
}
```

### 4.2 JWT Token Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-2026-03-02-v1"
  },
  "payload": {
    "jti": "550e8400-e29b-41d4-a716-446655440000",
    "sub": "user-uuid",
    "username": "john_doe",
    "email": "john@example.com",
    "auth_type": "password",
    "iat": 1709376000,
    "exp": 1709376900,
    "iss": "auth-service",
    "aud": "api-gateway"
  }
}
```

### 4.3 Token Expiry Strategy (IMPROVED)

| Token Type | Expiry | Rationale |
|------------|--------|-----------|
| Access Token | 30 minutes (was 15) | Better UX, blacklist for logout |
| Refresh Token | 7 days | Balance security and convenience |
| Password Reset Token | 1 hour | Standard security practice |

### 4.4 Rate Limiting (CLARIFIED)

**Implementation:** In-memory with Caffeine (suitable for single-instance deployment)

**Limitations:**
- Does not scale across multiple instances
- For distributed deployment, use Redis + Bucket4j

**Configuration:**
- Login: 5 attempts/IP/minute
- Refresh: 10 attempts/user/minute
- Password reset: 3 attempts/email/hour

---

## 5. Component Design

### 5.1 Scheduled Tasks (NEW)

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enables @Scheduled annotations
}

@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        tokenBlacklistRepository.deleteExpiredEntries(LocalDateTime.now());
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
```

### 5.2 Refresh Token with Idempotency (IMPROVED)

```java
@Service
@RequiredArgsConstructor
public class TokenService {

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Handle idempotency for concurrent requests
        String idempotencyKey = request.getIdempotencyKey();

        if (idempotencyKey != null) {
            // Check if this request was already processed
            Optional<AuthResponse> cached = cache.get(idempotencyKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        // Process refresh...
        AuthResponse response = processRefresh(request.getRefreshToken());

        // Cache for idempotency (5 minutes)
        if (idempotencyKey != null) {
            cache.put(idempotencyKey, response, 5, TimeUnit.MINUTES);
        }

        return response;
    }
}
```

### 5.3 Email Service (NEW)

```java
@Service
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class EmailService {

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private int smtpPort;

    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    public void sendPasswordResetEmail(String email, String resetToken) {
        JavaMailSender mailSender = createMailSender();
        MimeMessage message = createPasswordResetMessage(email, resetToken);
        mailSender.send(message);
    }
}
```

---

## 6. Configuration Management

### 6.1 Application Configuration

```yaml
# application.yml
spring:
  application:
    name: auth-service
  profiles:
    active: ${ENV:dev}
  task:
    scheduling:
      pool:
        size: 2  # For scheduled tasks

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb_dev
  jpa:
    show-sql: true

email:
  enabled: false  # Disable email in dev

jwt:
  key-path: /opt/auth-service/keys
  access-token-expiry: 1800  # 30 minutes
  refresh-token-expiry: 604800  # 7 days

---
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
  jpa:
    show-sql: false

email:
  enabled: true
  smtp:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}

jwt:
  key-path: /opt/auth-service/keys
```

### 6.2 Environment Variables

```
┌─────────────────────────────────────────────────────────────┐
│              ENVIRONMENT VARIABLES                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Database:                                                  │
│    DB_PASSWORD                                             │
│    DB_HOST                                                 │
│    DB_PORT                                                 │
│    DB_NAME                                                 │
│                                                             │
│  OAuth2:                                                    │
│    GOOGLE_CLIENT_ID                                        │
│    GOOGLE_CLIENT_SECRET                                    │
│    GITHUB_CLIENT_ID                                        │
│    GITHUB_CLIENT_SECRET                                    │
│                                                             │
│  Email (NEW):                                               │
│    SMTP_HOST                                               │
│    SMTP_PORT                                               │
│    SMTP_USERNAME                                           │
│    SMTP_PASSWORD                                           │
│                                                             │
│  Frontend (NEW for OAuth2 redirect):                        │
│    FRONTEND_URL                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 Key Management (IMPROVED)

```bash
# Directory structure with permissions
/opt/auth-service/
├── keys/                    # Volume mounted
│   ├── private.key          # 600 permissions (read/write owner only)
│   ├── public.key           # 644 permissions (readable by all)
│   └── key-version.txt      # Key version identifier
└── config/
    └── application-prod.yml

# Key generation on startup (only if keys don't exist)
@PostConstruct
public void generateKeysIfNotExists() {
    Path privateKeyPath = Paths.get(keyPath, "private.key");
    if (!Files.exists(privateKeyPath)) {
        KeyPair keyPair = generateRsaKeyPair();
        Files.write(privateKeyPath, keyPair.getPrivate().getEncoded());
        Files.write(publicKeyPath, keyPair.getPublic().getEncoded());

        // Set secure permissions
        privateKeyPath.toFile().setReadable(true, true);
        privateKeyPath.toFile().setWritable(false, false);

        // Write key version
        Files.writeString(Paths.get(keyPath, "key-version.txt"),
            "key-" + LocalDate.now() + "-v1");
    }
}
```

---

## 7. Deployment Architecture

### 7.1 Docker Configuration (IMPROVED)

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar
COPY target/auth-service-*.jar app.jar

# Create key storage directory with permissions
RUN mkdir -p /opt/auth-service/keys && \
    chmod 700 /opt/auth-service/keys

# Create non-root user
RUN addgroup -S auth && adduser -S auth -G auth
USER auth

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app.jar"]
```

### 7.2 Docker Compose (IMPROVED)

```yaml
# docker-compose.yml
version: '3.8'

services:
  auth-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_PASSWORD=postgres
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - FRONTEND_URL=http://localhost:3000
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./keys:/opt/auth-service/keys:rw  # Persist keys
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    # NTP sync for clock skew mitigation
    dns:
      - 1.1.1.1
      - 8.8.8.8

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=authdb
      - POSTGRES_USER=authuser
      - POSTGRES_PASSWORD=postgres
    volumes:
      - ./pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U authuser -d authdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Optional: Redis for distributed rate limiting (future)
  # redis:
  #   image: redis:7-alpine
  #   ports:
  #     - "6379:6379"
  #   volumes:
  #     - ./redis-data:/data

volumes:
  keys:
  pgdata:
```

### 7.3 NTP Configuration (NEW)

For clock skew mitigation, ensure containers have synchronized time:

```yaml
# In docker-compose.yml or K8s config
services:
  auth-service:
    # Use host's time sync
    cap_add:
      - SYS_TIME
    # Or install chrony in container
    command: >
      sh -c "
        apk add --no-cache chrony &&
        chronyd -d &&
        java -jar /app.jar
      "
```

---

## 8. Edge Cases & Failure Scenarios (IMPROVED)

### 8.1 Password Change

```java
@Service
public class UserService {

    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        // Update password
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException());

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Invalidate ALL refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(userId);

        userRepository.save(user);
    }
}
```

### 8.2 Multi-Device Overflow

```java
@Service
public class TokenService {

    public AuthResponse generateTokens(UUID userId, String deviceId) {
        List<RefreshToken> activeTokens = refreshTokenRepository
            .findByUserIdAndRevokedFalse(userId);

        if (activeTokens.size() >= MAX_TOKENS_PER_USER) {
            // Revoke oldest token
            RefreshToken oldest = activeTokens.stream()
                .min(Comparator.comparing(RefreshToken::getCreatedAt))
                .orElseThrow();
            refreshTokenRepository.revokeToken(oldest.getTokenId());
        }

        // Create new refresh token...
    }
}
```

### 8.3 Account Deletion

```java
@Service
public class UserService {

    @Transactional
    public void deleteAccount(UUID userId) {
        // Add all active JWTs to blacklist
        // Note: This requires tracking active JTIs
        tokenBlacklistRepository.blacklistAllUserTokens(userId);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(userId);

        // Soft delete user
        userRepository.softDeleteById(userId);
    }
}
```

### 8.4 OAuth2 Email Change

```java
@Service
public class OAuth2Service {

    public User findOrCreateOAuthUser(String provider, String subjectId,
                                     String email, String name) {
        return userRepository.findByProviderAndSubjectId(provider, subjectId)
            .orElseGet(() -> {
                // Check if email already exists with different provider
                return userRepository.findByEmail(email)
                    .map(existingUser -> {
                        // Link OAuth account to existing user
                        existingUser.setOauthProvider(provider);
                        existingUser.setOauthSubjectId(subjectId);
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> createNewOAuthUser(provider, subjectId, email, name));
            });
    }
}
```

---

## 9. Implementation Phases

### 9.1 Phase 1: Core Authentication (Weeks 1-2)

```
Week 1:
    [ ] Project setup (Spring Boot 3.2, Java 21, Maven)
    [ ] Database schema with migration scripts
    [ ] User, RefreshToken entities
    [ ] UserRepository, RefreshTokenRepository
    [ ] SecurityConfig (filter chain)
    [ ] Key generation with volume mount

Week 2:
    [ ] JwtTokenProvider (RS256) with JWKS endpoint
    [ ] AuthService (login, validate)
    [ ] TokenService (generate, refresh with idempotency)
    [ ] AuthController (endpoints)
    [ ] Unit tests + integration tests
```

### 9.2 Phase 2: OAuth2 Integration (Weeks 3-4)

```
Week 3:
    [ ] OAuth2Config (Google, GitHub)
    [ ] OAuth2AuthenticationSuccessHandler (with redirect)
    [ ] UserService (findOrCreate with email merge)
    [ ] Frontend OAuth2 callback handling
    [ ] OAuth2 flow integration testing

Week 4:
    [ ] RateLimitFilter (Bucket4j)
    [ ] Scheduled cleanup tasks (@EnableScheduling)
    [ ] GlobalExceptionHandler
    [ ] OpenAPI documentation
    [ ] End-to-end testing
```

### 9.3 Phase 3: Advanced Features (Weeks 5-6)

```
Week 5:
    [ ] Password reset flow with EmailService
    [ ] TokenBlacklist with audit considerations
    [ ] Password change token invalidation
    [ ] Multi-device overflow handling
    [ ] Actuator configuration

Week 6:
    [ ] Key rotation service (90 days)
    [ ] Metrics and monitoring (Micrometer)
    [ ] Performance testing
    [ ] Security audit
    [ ] NTP configuration
```

### 9.4 Phase 4: Deployment & MVP (Weeks 7-8)

```
Week 7:
    [ ] Dockerfile with health checks
    [ ] docker-compose.yml with volumes
    [ ] Database migration scripts
    [ ] Environment configuration
    [ ] Key persistence testing

Week 8:
    [ ] Deployment testing
    [ ] Load testing (parallel refresh)
    [ ] Clock skew testing
    [ ] Documentation finalization
    [ ] MVP release
```

---

## 10. Risk Mitigation (UPDATED)

| Risk | Impact | Mitigation |
|------|--------|------------|
| Key loss on restart | Critical | Volume mount `/opt/auth-service/keys` |
| Database connection exhaustion | High | HikariCP config, circuit breaker |
| JWT key compromise | Critical | 90-day rotation, key version tracking |
| OAuth2 provider outage | Medium | Multiple providers, fallback to password |
| Concurrent refresh race condition | Medium | Idempotency key in refresh request |
| Clock skew | Medium | NTP sync, ±30s tolerance |
| Rate limiting bypass (distributed) | Medium | Document limitation, recommend Redis |
| Password change token validity | High | Invalidate all refresh tokens |

---

## 11. Testing Strategy

### 11.1 Unit Tests

- Service layer tests with mocked repositories
- JWT generation/validation tests
- Password encoding tests
- Rate limiting tests

### 11.2 Integration Tests

- TestContainers for PostgreSQL
- OAuth2 flow with mock providers
- End-to-end API tests
- Concurrent refresh tests

### 11.3 Edge Case Tests

- Password change during active session
- Multi-device overflow
- Account deletion with active tokens
- Clock skew simulation

---

**Document Status:** READY FOR IMPLEMENTATION
**Next Step:** Step 2.6 - Implementation Planning
