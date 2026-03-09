# Auth Service Development Guide

Complete guide for contributing to and developing the Auth Service.

## Table of Contents

- [Project Structure](#project-structure)
- [Development Environment](#development-environment)
- [Coding Conventions](#coding-conventions)
- [Testing](#testing)
- [Debugging](#debugging)
- [Adding New Features](#adding-new-features)
- [Code Review Checklist](#code-review-checklist)
- [Git Workflow](#git-workflow)

---

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/vibe/auth/
│   │   │   ├── AuthServiceApplication.java          # Main application class
│   │   │   ├── config/                              # Configuration classes
│   │   │   │   ├── SecurityConfig.java              # Security configuration
│   │   │   │   ├── JwtConfig.java                   # JWT configuration
│   │   │   │   ├── OAuth2Config.java                # OAuth2 configuration
│   │   │   │   └── FlywayConfig.java                # Database migration config
│   │   │   ├── controller/                          # REST controllers
│   │   │   │   ├── AuthController.java              # Auth endpoints
│   │   │   │   ├── JwksController.java              # JWKS endpoint
│   │   │   │   └── GlobalExceptionHandler.java      # Error handling
│   │   │   ├── dto/                                 # Data Transfer Objects
│   │   │   │   ├── request/                         # Request DTOs
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   └── PasswordResetRequest.java
│   │   │   │   └── response/                        # Response DTOs
│   │   │   │       ├── AuthResponse.java
│   │   │   │       └── ErrorResponse.java
│   │   │   ├── model/                               # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── TokenBlacklist.java
│   │   │   ├── repository/                          # Data repositories
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── TokenBlacklistRepository.java
│   │   │   ├── service/                             # Business logic
│   │   │   │   ├── AuthService.java                 # Authentication logic
│   │   │   │   ├── TokenService.java                # Token management
│   │   │   │   ├── JwtTokenProvider.java            # JWT generation/validation
│   │   │   │   ├── UserService.java                 # User management
│   │   │   │   ├── EmailService.java                # Email sending
│   │   │   │   └── OAuth2Service.java               # OAuth2 integration
│   │   │   ├── security/                            # Security components
│   │   │   │   ├── JwtAuthenticationFilter.java     # JWT filter
│   │   │   │   ├── RateLimitFilter.java             # Rate limiting
│   │   │   │   └── PasswordEncoder.java             # Password encoding
│   │   │   └── exception/                           # Custom exceptions
│   │   │       ├── AuthenticationException.java
│   │   │       ├── TokenExpiredException.java
│   │   │       └── RateLimitExceededException.java
│   │   └── resources/
│   │       ├── application.yml                      # Main configuration
│   │       ├── application-dev.yml                  # Dev configuration
│   │       ├── application-prod.yml                 # Prod configuration
│   │       └── db/migration/                        # Flyway migrations
│   │           ├── V1__Create_user_table.sql
│   │           ├── V2__Create_refresh_tokens_table.sql
│   │           └── V3__Create_token_blacklist_table.sql
│   └── test/
│       ├── java/com/vibe/auth/
│       │   ├── controller/                          # Controller tests
│       │   │   ├── AuthControllerTest.java
│       │   │   └── JwksControllerTest.java
│       │   ├── service/                             # Service tests
│       │   │   ├── AuthServiceTest.java
│       │   │   ├── TokenServiceTest.java
│       │   │   └── JwtTokenProviderTest.java
│       │   ├── repository/                          # Repository tests
│       │   │   ├── UserRepositoryTest.java
│       │   │   └── RefreshTokenRepositoryTest.java
│       │   └── integration/                         # Integration tests
│       │       ├── AuthFlowTest.java
│       │       └── OAuth2FlowTest.java
│       └── resources/
│           ├── application-test.yml                 # Test configuration
│           └── db/migration/                        # Test migrations
├── docker/
│   ├── Dockerfile                                   # Production image
│   └── docker-compose.yml                           # Local development
├── scripts/
│   ├── init-db.sh                                   # Database initialization
│   └── generate-keys.sh                             # Key generation
├── pom.xml                                          # Maven configuration
└── README.md                                        # This file
```

---

## Development Environment

### Prerequisites

Install required software:

```bash
# macOS
brew install openjdk@21 maven postgresql docker

# Ubuntu/Debian
sudo apt install openjdk-21-jdk maven postgresql docker.io

# Verify installations
java -version    # OpenJDK 21
mvn -version     # Maven 3.9+
docker --version # Docker 20.10+
```

### IDE Setup

**IntelliJ IDEA (Recommended):**

1. Import project as Maven project
2. Enable Lombok plugin: Settings → Plugins → Lombok
3. Enable annotation processing: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing
4. Set up code style: Settings → Editor → Code Style → Java → Import → Import from URL → `https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-style.xml`

**VS Code:**

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support
2. Configure settings:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.format.enabled": true,
     "java.saveActions.organizeImports": true
   }
   ```

### Local Database Setup

```bash
# Start PostgreSQL using Docker
docker run -d \
  --name auth-postgres-dev \
  -e POSTGRES_DB=authdb_dev \
  -e POSTGRES_USER=authuser \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v authdb-dev-data:/var/lib/postgresql/data \
  postgres:15-alpine

# Verify connection
docker exec -it auth-postgres-dev psql -U authuser -d authdb_dev -c "SELECT 1"
```

### Environment Configuration

Create `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb_dev
    username: authuser
    password: postgres
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate

  # Disable email in local development
email:
  enabled: false

# OAuth2 (use test credentials)
spring.security.oauth2.client:
  registration:
    google:
      client-id: ${GOOGLE_CLIENT_ID:test-client-id}
      client-secret: ${GOOGLE_CLIENT_SECRET:test-client-secret}
    github:
      client-id: ${GITHUB_CLIENT_ID:test-client-id}
      client-secret: ${GITHUB_CLIENT_SECRET:test-client-secret}

# Logging
logging:
  level:
    root: INFO
    com.vibe.auth: DEBUG
    org.springframework.security: DEBUG
```

### Running the Application

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Using IDE
# Run AuthServiceApplication.main()

# Using Java after building
mvn clean package
java -jar target/auth-service-1.0.0.jar --spring.profiles.active=local
```

Verify it's running:

```bash
curl http://localhost:8080/actuator/health
```

---

## Coding Conventions

### Java Code Style

Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html):

**Indentation:**
- Use 4 spaces (no tabs)
- Maximum line length: 120 characters

**Naming Conventions:**
```java
// Classes: PascalCase
public class AuthService {}

// Methods: camelCase
public void generateToken() {}

// Constants: UPPER_SNAKE_CASE
private static final int MAX_TOKENS_PER_USER = 5;

// Variables: camelCase
private String refreshToken;

// Packages: lowercase
package com.vibe.auth.service;
```

**Order of Declarations:**
```java
public class Example {
    // 1. Constants
    private static final int CONSTANT = 1;

    // 2. Static fields
    private static String staticField;

    // 3. Instance fields
    private String instanceField;

    // 4. Constructors
    public Example() {}

    // 5. Public methods
    public void publicMethod() {}

    // 6. Protected methods
    protected void protectedMethod() {}

    // 7. Private methods
    private void privateMethod() {}
}
```

### Javadoc Guidelines

Add Javadoc to all public APIs:

```java
/**
 * Service for handling authentication operations.
 *
 * <p>This service provides methods for user authentication, token generation,
 * and token validation.
 *
 * @author Your Name
 * @since 1.0.0
 */
@Service
public class AuthService {

    /**
     * Authenticates a user with username and password.
     *
     * @param request the login request containing credentials
     * @return authentication response containing tokens
     * @throws AuthenticationException if authentication fails
     * @throws RateLimitExceededException if rate limit exceeded
     */
    public AuthResponse login(LoginRequest request) {
        // Implementation
    }
}
```

### Exception Handling

**Create specific exceptions:**

```java
public class AuthenticationException extends RuntimeException {
    private final String errorCode;

    public AuthenticationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

**Handle exceptions in controller advice:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .error(ex.getMessage())
            .code(ex.getErrorCode())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

### Logging

Use SLF4J with proper logging levels:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.getUsername());
        try {
            // Business logic
            log.info("User {} logged in successfully", request.getUsername());
            return response;
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for user {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }
}
```

**Logging Levels:**
- `ERROR`: Application errors requiring immediate attention
- `WARN`: Unexpected situations that don't stop execution
- `INFO`: Important business events (login, logout, etc.)
- `DEBUG**: Detailed information for debugging
- `TRACE`: Very detailed debugging information

### Dependency Injection

Use constructor injection:

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    // No need to write constructor
}
```

### Configuration Properties

Use type-safe configuration:

```java
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    @NotBlank String keyPath,
    @Min(60) @Max(3600) int accessTokenExpiry,
    @Min(86400) @Max(2592000) int refreshTokenTokenExpiry
) {
}
```

---

## Testing

### Testing Strategy

**Test Pyramid:**
```
        /\
       /E2E\      (Few, slow, expensive)
      /------\
     /  Integration \ (Some, medium speed)
    /----------------\
   /    Unit Tests    \ (Many, fast, cheap)
  /--------------------\
```

### Unit Tests

**Test Structure:**

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void login_withValidCredentials_shouldReturnTokens() {
        // Given
        LoginRequest request = new LoginRequest("john_doe", "password123");
        User user = createUser();
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(tokenService.generateTokens(user.getUserId())).thenReturn(mockAuthResponse());

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        verify(tokenService).generateTokens(user.getUserId());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void login_withInvalidUsername_shouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("unknown", "password");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid username or password");
    }
}
```

**Best Practices:**
- Use `@DisplayName` for descriptive test names
- Follow Given-When-Then pattern
- Mock external dependencies
- Test both success and failure paths
- Use assertions from AssertJ

### Integration Tests

**TestContainers Setup:**

```java
@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:15-alpine"
    ).withDatabaseName("testdb")
     .withUsername("test")
     .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuthService authService;

    @Test
    @Transactional
    void shouldAuthenticateUserInDatabase() {
        // Test with real database
    }
}
```

### Test Configuration

`src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-client-id
            client-secret: test-client-secret

jwt:
  key-path: src/test/resources/keys
```

### Running Tests

```bash
# Unit tests only (fast)
mvn test

# Integration tests only
mvn verify -P integration

# All tests
mvn verify

# Specific test class
mvn test -Dtest=AuthServiceTest

# Specific test method
mvn test -Dtest=AuthServiceTest#login_withValidCredentials_shouldReturnTokens

# Generate coverage report
mvn clean test jacoco:report
```

### Test Coverage

Target: 80%+ coverage for service layer.

View coverage report:

```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

---

## Debugging

### Remote Debugging

**Start application with debug enabled:**

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

**Configure IDE debugger:**
- Host: localhost
- Port: 5005

### Debug Logging

Enable debug logging for specific packages:

```yaml
# application-dev.yml
logging:
  level:
    com.vibe.auth: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

### Common Debugging Scenarios

**Debug authentication flow:**

```java
// Add breakpoints in:
// 1. SecurityConfig.configure()
// 2. JwtAuthenticationFilter.doFilterInternal()
// 3. AuthService.login()
```

**Debug token validation:**

```java
// Add breakpoints in:
// 1. JwtTokenProvider.validateToken()
// 2. JwtAuthenticationFilter.doFilterInternal()
// 3. TokenService.refreshToken()
```

**Debug OAuth2 flow:**

```java
// Add breakpoints in:
// 1. OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess()
// 2. OAuth2Service.findOrCreateUser()
```

### HTTP Debugging

**Use cURL with verbose:**

```bash
curl -v http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

**Use httpie for better formatting:**

```bash
http POST http://localhost:8080/api/v1/auth/login \
  username=test password=test
```

---

## Adding New Features

### Feature Development Workflow

1. **Create feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Write tests first (TDD):**
   ```bash
   # Create test class
   touch src/test/java/com/vibe/auth/service/NewFeatureTest.java
   ```

3. **Implement feature:**
   ```bash
   # Create implementation
   touch src/main/java/com/vibe/auth/service/NewFeatureService.java
   ```

4. **Run tests:**
   ```bash
   mvn test
   ```

5. **Commit with conventional commit:**
   ```bash
   git add .
   git commit -m "feat(service): add new feature"
   ```

6. **Push and create PR:**
   ```bash
   git push origin feature/your-feature-name
   ```

### Example: Adding 2FA Support

**Step 1: Write tests**

```java
@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    @Test
    @DisplayName("Should generate TOTP secret for user")
    void generateSecret_shouldReturnBase32EncodedSecret() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        String secret = twoFactorAuthService.generateSecret(userId);

        // Then
        assertThat(secret).isNotNull();
        assertThat(secret).hasSize(32);
    }
}
```

**Step 2: Implement feature**

```java
@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private final UserRepository userRepository;

    public String generateSecret(UUID userId) {
        String secret = generateRandomSecret();
        userRepository.saveTwoFactorSecret(userId, secret);
        return secret;
    }

    private String generateRandomSecret() {
        // Generate random Base32-encoded secret
        return Base32.random().toString();
    }
}
```

**Step 3: Add controller endpoint**

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/enable")
    public ResponseEntity<TwoFactorResponse> enable2fa() {
        UUID userId = SecurityContext.getCurrentUserId();
        String secret = twoFactorAuthService.generateSecret(userId);
        return ResponseEntity.ok(new TwoFactorResponse(secret));
    }
}
```

**Step 4: Add migration**

```sql
-- V4__Add_2fa_support.sql
ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(255);
CREATE INDEX idx_users_2fa ON users(two_factor_enabled) WHERE two_factor_enabled = TRUE;
```

**Step 5: Update documentation**

Update API.md with new endpoints.

---

## Code Review Checklist

### Before Submitting PR

- [ ] All tests pass (`mvn verify`)
- [ ] Code coverage maintained or improved
- [ ] No SonarQube warnings
- [ ] Javadoc added for public APIs
- [ ] Error handling complete
- [ ] Logging added appropriately
- [ ] Configuration documented
- [ ] Migration scripts added (if needed)
- [ ] API documentation updated
- [ ] Commit messages follow conventions

### Code Review Guidelines

**Functional Correctness:**
- Does the code implement the requirements?
- Are edge cases handled?
- Is error handling appropriate?

**Code Quality:**
- Is the code readable and maintainable?
- Are naming conventions followed?
- Is there unnecessary complexity?
- Are there code smells or duplications?

**Testing:**
- Are tests comprehensive?
- Are test cases meaningful?
- Are mocks used appropriately?

**Security:**
- Are inputs validated?
- Are secrets properly managed?
- Are security best practices followed?

**Performance:**
- Are database queries optimized?
- Is caching used appropriately?
- Are there potential performance bottlenecks?

---

## Git Workflow

### Branch Strategy

```
main (protected)
  └── develop (integration)
      └── feature/* (per feature)
      └── bugfix/* (per bug fix)
      └── hotfix/* (for production fixes)
```

### Commit Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes

**Examples:**

```bash
git commit -m "feat(auth): add OAuth2 support for Google"

git commit -m "fix(jwt): handle clock skew in token validation"

git commit -m "docs(api): update authentication endpoint documentation"

git commit -m "test(service): add unit tests for TokenService"
```

### Pull Request Process

1. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **Make changes and commit**
   ```bash
   git add .
   git commit -m "feat(scope): description"
   ```

3. **Push to remote**
   ```bash
   git push origin feature/your-feature
   ```

4. **Create Pull Request**
   - Use descriptive title
   - Reference related issues
   - Add description of changes
   - Include screenshots if applicable

5. **Address review feedback**
   - Make requested changes
   - Add commits to branch
   - Request review again

6. **Merge after approval**
   - Squash commits if needed
   - Delete branch after merge

### Release Process

```bash
# Update version in pom.xml
mvn versions:set -DnewVersion=1.0.0

# Commit version change
git commit -m "chore: bump version to 1.0.0"

# Create tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push tag
git push origin v1.0.0
```

---

## Performance Tips

### Database Optimization

```java
// Use indexes in queries
@Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
Optional<User> findActiveByEmail(@Param("email") String email);

// Batch operations
@Transactional
public void revokeAllUserTokens(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId);
}
```

### Caching

```java
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Cacheable(value = "publicKeys", key = "#kid")
    public PublicKey getPublicKey(String kid) {
        // Load and cache public key
    }
}
```

### Async Processing

```java
@Service
@EnableAsync
public class EmailService {

    @Async
    public void sendPasswordResetEmail(String email, String token) {
        // Send email asynchronously
    }
}
```

---

## Resources

### Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/current/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Tools
- [TestContainers](https://www.testcontainers.org/)
- [JJWT](https://github.com/jwtk/jjwt)
- [Flyway](https://flywaydb.org/documentation/)

### Style Guides
- [Google Java Style](https://google.github.io/styleguide/javaguide.html)
- [Spring Boot Conventions](https://github.com/spring-projects/spring-boot/wiki/Customizing-Conventions)

---

## Getting Help

- Internal documentation: [docs/](docs/)
- Stack Overflow: Tag questions with `spring-boot` and `spring-security`
- GitHub Issues: [Report bugs](https://github.com/Tinkerc/vibe-coding/issues)
