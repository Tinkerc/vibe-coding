# Spring Boot 3.x + Spring Security 6.x Architecture Research for OAuth2/OIDC Auth Service

**Date:** 2026-03-02
**Status:** RESEARCH COMPLETE
**Project:** Auth Service MVP
**Duration:** 1-2 months

---

## 1. Spring Boot 3.x + Spring Security 6.x Best Practices

### 1.1 Project Structure for Auth Service

```
src/main/java/com/company/auth/
├── AuthApplication.java              # Main application class
├── config/                          # Configuration classes
│   ├── SecurityConfig.java           # Security filter chain
│   ├── OAuth2Config.java            # OAuth2/OIDC providers
│   ├── JwtConfig.java               # JWT configuration
│   └── RateLimitConfig.java         # Rate limiting configuration
├── controller/                      # REST controllers
│   ├── AuthController.java          # Login, refresh, logout
│   └── OAuth2Controller.java        # OAuth2 callback handling
├── service/                         # Business logic services
│   ├── AuthService.java             # Core auth logic
│   ├── UserService.java             # User management
│   ├── TokenService.java            # JWT & refresh token handling
│   └── OAuth2Service.java           # OAuth2 integration
├── repository/                      # Spring Data JPA repositories
│   ├── UserRepository.java          # User CRUD operations
│   ├── RefreshTokenRepository.java   # Refresh token management
│   └── TokenBlacklistRepository.java # JWT blacklist
├── model/                           # JPA entities
│   ├── User.java                   # User entity
│   ├── RefreshToken.java           # Refresh token entity
│   └── TokenBlacklist.java         # Blacklisted token entity
├── dto/                             # Data Transfer Objects
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   └── PasswordResetRequest.java
│   └── response/
│       ├── AuthResponse.java
│       └── TokenResponse.java
├── security/                        # Security utilities
│   ├── JwtTokenProvider.java        # JWT generation/verification
│   ├── CustomUserDetailsService.java # User details service
│   └── OAuth2AuthenticationSuccessHandler.java
├── exception/                       # Custom exceptions
│   ├── AuthException.java
│   ├── RateLimitExceededException.java
│   └── TokenExpiredException.java
├── filter/                          # Custom filters
│   ├── JwtAuthenticationFilter.java # JWT validation filter
│   └── RateLimitFilter.java         # Rate limiting filter
└── util/                            # Utility classes
    ├── PasswordEncoderUtil.java     # Password encoding utilities
    └── SecurityConstants.java       # Security constants
```

### 1.2 Modern Spring Security 6.x Configuration

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login/oauth2")
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler))
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .logoutSuccessUrl("/login?logout")
                .addLogoutHandler(new SecurityContextLogoutHandler()))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                           UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

### 1.3 JWT Configuration with RS256

```java
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${jwt.expiration:900}")
    private long jwtExpiration;

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Qualifier("userDetailsService") UserDetailsService userDetailsService) {

        // For RS256, use RSA key pair
        KeyPair keyPair = generateRsaKeyPair();

        return new JwtTokenProvider(
            keyPair.getPrivate(),
            keyPair.getPublic(),
            jwtExpiration,
            userDetailsService
        );
    }

    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new AuthException("Failed to generate RSA key pair");
        }
    }
}
```

---

## 2. OAuth2/OIDC Implementation Patterns

### 2.1 OAuth2 Client Configuration for Multiple Providers

```java
@Configuration
@RequiredArgsConstructor
public class OAuth2Config {

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            githubClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId(googleClientId)
            .clientSecret(googleClientSecret)
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    }

    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
            .clientId(githubClientId)
            .clientSecret(githubClientSecret)
            .scope("read:user", "user:email")
            .authorizationUri("https://github.com/login/oauth/authorize")
            .tokenUri("https://github.com/login/oauth/access_token")
            .userInfoUri("https://api.github.com/user")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    }
}
```

### 2.2 Custom OAuth2 Authentication Success Handler

```java
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String provider = getProviderFromAuthentication(authentication);

        // Find or create user
        User user = userService.findOrCreateOAuthUser(
            provider,
            oauth2User.getAttribute("sub"),
            oauth2User.getAttribute("email"),
            oauth2User.getAttribute("name")
        );

        // Generate tokens
        AuthResponse authResponse = tokenService.generateTokens(user);

        // Return as JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getWriter(), authResponse);
    }
}
```

---

## 3. PostgreSQL Schema Design for Auth

### 3.1 Entity Definitions

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_oauth", columnList = "oauth_provider, oauth_subject_id")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(unique = true, nullable = false, length = 255)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthType authType = AuthType.PASSWORD;

    @Column(length = 50)
    private String oauthProvider;

    @Column(length = 255)
    private String oauthSubjectId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_device", columnList = "device_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_blacklist_jti", columnList = "jti"),
    @Index(name = "idx_blacklist_user", columnList = "user_id")
})
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID blacklistId;

    @Column(name = "jti", nullable = false, length = 255, unique = true)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
```

### 3.2 Repository Interfaces

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndSubjectId(String provider, String subjectId);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.username = :username")
    Optional<User> findActiveUserByUsername(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = NOW() WHERE u.userId = :userId")
    void softDeleteById(@Param("userId") UUID userId);
}

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.tokenId = :tokenId")
    void revokeToken(@Param("tokenId") UUID tokenId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.userId = :userId")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
```

---

## 4. JWT Signing Key Management (Local Deployment)

### 4.1 Key Generation and Storage

```java
@Component
public class KeyManager {

    private final String KEY_STORAGE_PATH = "/opt/auth-service/keys";

    @PostConstruct
    public void init() {
        createKeyDirectoryIfNotExists();
        generateKeyPairIfNotExists();
    }

    private void generateKeyPairIfNotExists() {
        Path privateKeyPath = Paths.get(KEY_STORAGE_PATH, "private.key");
        Path publicKeyPath = Paths.get(KEY_STORAGE_PATH, "public.key");

        if (!Files.exists(privateKeyPath) || !Files.exists(publicKeyPath)) {
            try {
                KeyPair keyPair = generateRsaKeyPair();

                Files.write(privateKeyPath,
                    keyPair.getPrivate().getEncoded(),
                    StandardOpenOption.CREATE);

                Files.write(publicKeyPath,
                    keyPair.getPublic().getEncoded(),
                    StandardOpenOption.CREATE);

                // Set secure permissions
                privateKeyPath.toFile().setReadable(true, true);
                privateKeyPath.toFile().setWritable(false, false);

            } catch (IOException e) {
                throw new AuthException("Failed to generate and store key pair");
            }
        }
    }

    public KeyPair loadKeyPair() throws Exception {
        Path privateKeyPath = Paths.get(KEY_STORAGE_PATH, "private.key");
        Path publicKeyPath = Paths.get(KEY_STORAGE_PATH, "public.key");

        PrivateKey privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(privateKeyPath)));

        PublicKey publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(Files.readAllBytes(publicKeyPath)));

        return new KeyPair(publicKey, privateKey);
    }
}
```

### 4.2 Key Rotation Strategy

- **Rotation Period:** Every 90 days
- **Grace Period:** 15 minutes (access token expiry)
- **Strategy:** Support multiple active keys with `kid` header

---

## 5. Rate Limiting Strategies

### 5.1 Bucket4j Implementation

```java
@Service
public class RateLimiter {

    private final Cache<String, Bucket> cache;

    public boolean tryConsumeWithClient(String clientId, String endpoint) {
        String key = endpoint + ":" + clientId;
        Bucket bucket = cache.get(key, k -> createBucket(endpoint));

        return bucket.tryConsume(1);
    }

    private Bucket createBucket(String endpoint) {
        int limit = getLimitForEndpoint(endpoint);
        return Bucket.builder()
            .withCapacity(limit)
            .withLimitedRefill(Bandwidth.class)
            .withInitialTokens(limit)
            .withRefillGreedy(limit, TimeUnit.MINUTES)
            .build();
    }

    private int getLimitForEndpoint(String endpoint) {
        return switch (endpoint) {
            case "login" -> 5;
            case "refresh" -> 10;
            default -> 100;
        };
    }
}
```

---

## 6. Testing Strategies

### 6.1 TestContainers for PostgreSQL

```java
@Testcontainers
@SpringBootTest
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("auth_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## 7. API Documentation with SpringDoc

### 7.1 OpenAPI Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Auth Service API")
                .version("1.0.0")
                .description("Authentication and authorization service API"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

---

## 8. Common Pitfalls and Best Practices

### Security Best Practices

1. Always use HTTPS in production
2. Never store JWTs in local storage - use HTTP-only cookies or Authorization headers
3. Use RS256 instead of HS256 for JWTs
4. Implement token rotation for refresh tokens
5. Rate limiting on authentication endpoints

### Performance Best Practices

1. Keep JWT validation stateless - no database calls
2. Cache public keys for JWT validation
3. Use connection pooling for PostgreSQL
4. Implement proper indexes on OAuth lookup fields

---

## 9. Implementation Timeline (1-2 Months)

### Phase 1: Core Authentication (Weeks 1-2)
- [ ] Set up Spring Boot 3.x project structure
- [ ] Implement User entity and repositories
- [ ] Create JWT configuration with RS256
- [ ] Implement login/refresh/logout endpoints
- [ ] Add rate limiting
- [ ] Write unit and integration tests

### Phase 2: OAuth2 Integration (Weeks 3-4)
- [ ] Configure OAuth2 providers (Google, GitHub)
- [ ] Implement OAuth2 authentication flow
- [ ] Create user service for OAuth users
- [ ] Test OAuth2 flows
- [ ] Add API documentation

### Phase 3: Advanced Features (Weeks 5-6)
- [ ] Implement password reset flow
- [ ] Add token blacklist functionality
- [ ] Implement key rotation
- [ ] Add monitoring with Actuator

### Phase 4: Deployment and MVP (Weeks 7-8)
- [ ] Docker configuration
- [ ] Local deployment setup
- [ ] End-to-end testing
- [ ] Documentation updates

---

## 10. References

### Official Spring Documentation
- [Spring Security 6.x Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Boot 3.x Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/oauth2/index.html)

### JWT Standards
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [RFC 7518 - JSON Web Algorithms (JWA)](https://tools.ietf.org/html/rfc7518)

### OAuth2/OIDC
- [OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)

---

**Research Complete:** 2026-03-02
