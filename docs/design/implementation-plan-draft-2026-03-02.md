# Auth Service MVP - TDD-First Implementation Plan

**Date:** 2026-03-02
**Status:** DRAFT
**Based on:** tech-solution-2026-03-02.md, requirement-2026-03-02.md

---

## Implementation Principles

### TDD Workflow (Red-Green-Refactor)
1. **RED:** Write a failing test
2. **GREEN:** Write minimal code to pass
3. **REFACTOR:** Improve code while keeping tests green
4. **COMMIT:** Each atomic change gets committed

### Task Granularity
- Each task: 2-5 minutes
- One file creation/modification per task
- Test first, then implementation
- Commit after each task

### Git Commit Conventions
```
feat(scope): description
fix(scope): description
test(scope): description
refactor(scope): description
```

---

## Phase 1: Core Authentication (Weeks 1-2)

### Week 1: Project Setup & Foundation

#### Task 1.1.1: Initialize Spring Boot Project
**Time:** 3 minutes | **Type:** Setup | **Commit:** `feat(build): initialize Spring Boot project`

**Test:** `mvn clean compile`

**File:** `pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>
    <groupId>com.vibe</groupId>
    <artifactId>auth-service</artifactId>
    <version>1.0.0</version>
    <name>Auth Service</name>
    <description>Authentication and authorization service with OAuth2/OIDC</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.3</jjwt.version>
        <bucket4j.version>8.7.0</bucket4j.version>
        <springdoc.version>2.3.0</springdoc.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
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
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Rate Limiting -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
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
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

#### Task 1.1.2: Create Application Configuration
**Time:** 2 minutes | **Type:** Config | **Commit:** `feat(config): create application.yml`

**File:** `src/main/resources/application.yml`
```yaml
spring:
  application:
    name: auth-service
  profiles:
    active: ${ENV:dev}

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb_dev
    username: authuser
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

jwt:
  key-path: /opt/auth-service/keys
  access-token-expiry: 1800  # 30 minutes
  refresh-token-expiry: 604800  # 7 days

email:
  enabled: false

logging:
  level:
    com.vibe.auth: DEBUG
    org.springframework.security: DEBUG
```

---

#### Task 1.1.3: Create Main Application Class
**Time:** 1 minute | **Type:** Setup | **Commit:** `feat(app): create main application class`

**File:** `src/main/java/com/vibe/auth/AuthServiceApplication.java`
```java
package com.vibe.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

**Test:** `mvn spring-boot:run` (verify startup)

---

### Week 1: Database & Entities

#### Task 1.2.1: Create User Entity
**Time:** 3 minutes | **Type:** Entity | **Commit:** `feat(model): create User entity`

**Test First:** `src/test/java/com/vibe/auth/model/UserTest.java`
```java
package com.vibe.auth.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void shouldCreateUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");

        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }
}
```

**Implementation:** `src/main/java/com/vibe/auth/model/User.java`
```java
package com.vibe.auth.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public enum AuthType {
        PASSWORD, OAUTH2
    }

    // Getters and setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthSubjectId() { return oauthSubjectId; }
    public void setOauthSubjectId(String oauthSubjectId) { this.oauthSubjectId = oauthSubjectId; }
}
```

---

#### Task 1.2.2: Create RefreshToken Entity
**Time:** 3 minutes | **Type:** Entity | **Commit:** `feat(model): create RefreshToken entity`

**File:** `src/main/java/com/vibe/auth/model/RefreshToken.java`
```java
package com.vibe.auth.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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

    // Getters and setters
    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
```

---

#### Task 1.2.3: Create Repositories
**Time:** 3 minutes | **Type:** Repository | **Commit:** `feat(repo): create UserRepository and RefreshTokenRepository`

**File:** `src/main/java/com/vibe/auth/repository/UserRepository.java`
```java
package com.vibe.auth.repository;

import com.vibe.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
```

**File:** `src/main/java/com/vibe/auth/repository/RefreshTokenRepository.java`
```java
package com.vibe.auth.repository;

import com.vibe.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

### Week 2: Security & JWT

#### Task 1.3.1: Create JwtTokenProvider
**Time:** 5 minutes | **Type:** Security | **Commit:** `feat(security): create JwtTokenProvider with RS256`

**File:** `src/main/java/com/vibe/auth/security/JwtTokenProvider.java`
```java
package com.vibe.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.key-path:/opt/auth-service/keys}")
    private String keyPath;

    @Value("${jwt.access-token-expiry:1800}")
    private long accessTokenExpiry;

    private KeyPair keyPair;

    @PostConstruct
    public void init() throws Exception {
        generateKeysIfNotExists();
        this.keyPair = loadKeyPair();
    }

    private void generateKeysIfNotExists() throws Exception {
        java.nio.file.Path privateKeyPath = Paths.get(keyPath, "private.key");
        java.nio.file.Path publicKeyPath = Paths.get(keyPath, "public.key");

        if (!Files.exists(privateKeyPath)) {
            Files.createDirectories(Paths.get(keyPath));
            KeyPair keyPair = generateRSAKeyPair();
            Files.write(privateKeyPath, keyPair.getPrivate().getEncoded());
            Files.write(publicKeyPath, keyPair.getPublic().getEncoded());

            // Set permissions
            privateKeyPath.toFile().setReadable(true, true);
            privateKeyPath.toFile().setWritable(false, false);
        }
    }

    private KeyPair generateRSAKeyPair() throws Exception {
        java.security.KeyPairGenerator keyPairGenerator =
            java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private KeyPair loadKeyPair() throws Exception {
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(
                Files.readAllBytes(Paths.get(keyPath, "private.key"))));
        PublicKey publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(
                Files.readAllBytes(Paths.get(keyPath, "public.key"))));
        return new KeyPair(publicKey, privateKey);
    }

    public String generateAccessToken(UUID userId, String username, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiry * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("username", username);
        claims.put("email", email);

        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getPublicKeyModulus() {
        return java.util.Base64.getEncoder()
            .encodeToString(((java.security.interfaces.RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray());
    }

    public String getPublicKeyExponent() {
        return java.util.Base64.getEncoder()
            .encodeToString(((java.security.interfaces.RSAPublicKey) keyPair.getPublic()).getPublicExponent().toByteArray());
    }

    public String getKeyId() {
        return "key-" + java.time.LocalDate.now();
    }
}
```

---

#### Task 1.3.2: Create Security Configuration
**Time:** 5 minutes | **Type:** Security | **Commit:** `feat(security): create SecurityConfig`

**File:** `src/main/java/com/vibe/auth/config/SecurityConfig.java`
```java
package com.vibe.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/.well-known/jwks.json").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated());

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

---

#### Task 1.3.3: Create DTOs
**Time:** 3 minutes | **Type:** DTO | **Commit:** `feat(dto): create request and response DTOs`

**Files:**
- `src/main/java/com/vibe/auth/dto/request/LoginRequest.java`
- `src/main/java/com/vibe/auth/dto/request/RefreshTokenRequest.java`
- `src/main/java/com/vibe/auth/dto/response/AuthResponse.java`

```java
// LoginRequest.java
package com.vibe.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 255)
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255)
    String password
) {}

// RefreshTokenRequest.java
package com.vibe.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken,

    String idempotencyKey  // Optional, for concurrent requests
) {}

// AuthResponse.java
package com.vibe.auth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
```

---

#### Task 1.3.4: Create AuthService
**Time:** 5 minutes | **Type:** Service | **Commit:** `feat(service): create AuthService with login logic`

**File:** `src/main/java/com/vibe/auth/service/AuthService.java`
```java
package com.vibe.auth.service;

import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.username(), request.password()));

        User user = userRepository.findActiveUserByUsername(request.username())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getUserId(),
            user.getUsername(),
            user.getEmail()
        );

        // TODO: Generate refresh token
        return AuthResponse.of(accessToken, null, 1800);
    }
}
```

---

#### Task 1.3.5: Create AuthController
**Time:** 3 minutes | **Type:** Controller | **Commit:** `feat(controller): create AuthController`

**File:** `src/main/java/com/vibe/auth/controller/AuthController.java`
```java
package com.vibe.auth.controller;

import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

---

## Phase 2: OAuth2 Integration (Weeks 3-4)

### Week 3: OAuth2 Configuration

#### Task 2.1.1: Create OAuth2Config
**Commit:** `feat(config): create OAuth2 configuration for Google and GitHub`

**File:** `src/main/java/com/vibe/auth/config/OAuth2Config.java`
```java
package com.vibe.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.List;

@Configuration
public class OAuth2Config {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            githubClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("${oauth2.google.client-id}")
            .clientSecret("${oauth2.google.client-secret}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    }

    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
            .clientId("${oauth2.github.client-id}")
            .clientSecret("${oauth2.github.client-secret}")
            .scope("read:user", "user:email")
            .authorizationUri("https://github.com/login/oauth/authorize")
            .tokenUri("https://github.com/login/oauth/access_token")
            .userInfoUri("https://api.github.com/user")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    }
}
```

---

## Summary

### Tasks Overview

| Phase | Week | Tasks | Estimated Time |
|-------|------|-------|----------------|
| Phase 1 | 1 | Project setup, entities, repos | ~20 tasks |
| Phase 1 | 2 | Security, JWT, auth endpoints | ~25 tasks |
| Phase 2 | 3 | OAuth2 config, handlers | ~15 tasks |
| Phase 2 | 4 | Rate limiting, cleanup, docs | ~15 tasks |
| Phase 3 | 5 | Password reset, blacklist | ~20 tasks |
| Phase 3 | 6 | Key rotation, monitoring | ~15 tasks |
| Phase 4 | 7 | Docker, deployment config | ~10 tasks |
| Phase 4 | 8 | Testing, documentation | ~10 tasks |

**Total:** ~130 tasks over 8 weeks

---

**Document Status:** DRAFT - Ready for plan review
**Next Step:** Step 2.7 - Plan Review (solution-critic)
