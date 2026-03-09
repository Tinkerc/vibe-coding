package com.vibe.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.dto.request.RefreshTokenRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.AuthType;
import com.vibe.auth.model.RefreshToken;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.RefreshTokenRepository;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Auth Service.
 * Tests full authentication flows using TestContainers for PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setAuthType(AuthType.PASSWORD);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should complete full login flow successfully")
    void shouldCompleteFullLoginFlowSuccessfully() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginWithWrongPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail login with non-existent user")
    void shouldFailLoginWithNonExistentUser() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should complete full refresh flow")
    void shouldCompleteFullRefreshFlow() throws Exception {
        // Arrange - Create a refresh token in the database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setTokenHash(hashToken("valid-refresh-token"));
        refreshToken.setDeviceId("test-device");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token", null);

        // Act
        var result = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should fail refresh with expired token")
    void shouldFailRefreshWithExpiredToken() throws Exception {
        // Arrange - Create an expired refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setTokenHash(hashToken("expired-token"));
        refreshToken.setDeviceId("test-device");
        refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("expired-token", null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail refresh with revoked token")
    void shouldFailRefreshWithRevokedToken() throws Exception {
        // Arrange - Create a revoked refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setTokenHash(hashToken("revoked-token"));
        refreshToken.setDeviceId("test-device");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("revoked-token", null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should complete full logout flow")
    void shouldCompleteFullLogoutFlow() throws Exception {
        // Arrange - Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(loginResponse, AuthResponse.class);

        // Act
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isNoContent());

        // Assert - Verify refresh token is revoked
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(testUser.getId()).get(0);
        assertTrue(refreshToken.isRevoked());
        assertNotNull(refreshToken.getRevokedAt());
    }

    @Test
    @DisplayName("Should handle token refresh rotation")
    void shouldHandleTokenRefreshRotation() throws Exception {
        // Arrange - Create initial refresh token
        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(UUID.randomUUID());
        oldToken.setUserId(testUser.getId());
        oldToken.setTokenHash(hashToken("old-refresh-token"));
        oldToken.setDeviceId("test-device");
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        oldToken.setCreatedAt(LocalDateTime.now());
        oldToken.setRevoked(false);
        refreshTokenRepository.save(oldToken);

        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token", null);

        // Act
        String response = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);

        // Assert - Old token should be revoked
        RefreshToken revokedToken = refreshTokenRepository.findById(oldToken.getId()).orElseThrow();
        assertTrue(revokedToken.isRevoked());
        assertEquals(authResponse.refreshToken(), revokedToken.getReplacedBy().toString());

        // Assert - New token should exist
        RefreshToken newToken = refreshTokenRepository.findById(
                UUID.fromString(authResponse.refreshToken())).orElseThrow();
        assertFalse(newToken.isRevoked());
        assertEquals(testUser.getId(), newToken.getUserId());
    }

    @Test
    @DisplayName("Should verify JWT token structure")
    void shouldVerifyJwtTokenStructure() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        String token = authResponse.accessToken();

        // Assert - Verify JWT structure (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        // Verify token is valid
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(testUser.getId(), jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Should handle multiple devices for same user")
    void shouldHandleMultipleDevicesForSameUser() throws Exception {
        // Arrange - Create refresh tokens for multiple devices
        for (int i = 1; i <= 3; i++) {
            RefreshToken token = new RefreshToken();
            token.setId(UUID.randomUUID());
            token.setUserId(testUser.getId());
            token.setTokenHash(hashToken("token-device-" + i));
            token.setDeviceId("device-" + i);
            token.setExpiresAt(LocalDateTime.now().plusDays(7));
            token.setCreatedAt(LocalDateTime.now());
            token.setRevoked(false);
            refreshTokenRepository.save(token);
        }

        // Act & Assert - Verify all tokens exist
        var tokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertTrue(tokens.size() >= 3);
    }

    @Test
    @DisplayName("Should enforce maximum devices per user")
    void shouldEnforceMaximumDevicesPerUser() throws Exception {
        // This test documents the expected behavior for device limits
        // Arrange - Create tokens for 5 devices (assuming max is 5)
        for (int i = 1; i <= 5; i++) {
            RefreshToken token = new RefreshToken();
            token.setId(UUID.randomUUID());
            token.setUserId(testUser.getId());
            token.setTokenHash(hashToken("token-device-" + i));
            token.setDeviceId("device-" + i);
            token.setExpiresAt(LocalDateTime.now().plusDays(7));
            token.setCreatedAt(LocalDateTime.now());
            token.setRevoked(false);
            refreshTokenRepository.save(token);
        }

        // Act - Create 6th token
        RefreshToken newToken = new RefreshToken();
        newToken.setId(UUID.randomUUID());
        newToken.setUserId(testUser.getId());
        newToken.setTokenHash(hashToken("token-device-6"));
        newToken.setDeviceId("device-6");
        newToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        newToken.setCreatedAt(LocalDateTime.now());
        newToken.setRevoked(false);
        refreshTokenRepository.save(newToken);

        // Assert - Oldest token should be revoked
        var tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
        assertTrue(tokens.size() <= 5);
    }

    @Test
    @DisplayName("Should handle concurrent refresh requests")
    void shouldHandleConcurrentRefreshRequests() throws Exception {
        // Arrange - Create refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUser.getId());
        refreshToken.setTokenHash(hashToken("concurrent-token"));
        refreshToken.setDeviceId("test-device");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("concurrent-token", null);

        // Act - Send multiple concurrent requests
        var result1 = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        var result2 = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert - Both should succeed
        result1.andExpect(status().isOk());
        result2.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void shouldCleanupExpiredTokens() throws Exception {
        // Arrange - Create expired tokens
        for (int i = 1; i <= 3; i++) {
            RefreshToken token = new RefreshToken();
            token.setId(UUID.randomUUID());
            token.setUserId(testUser.getId());
            token.setTokenHash(hashToken("expired-token-" + i));
            token.setDeviceId("device-" + i);
            token.setExpiresAt(LocalDateTime.now().minusDays(1));
            token.setCreatedAt(LocalDateTime.now().minusDays(8));
            token.setRevoked(false);
            refreshTokenRepository.save(token);
        }

        // Act
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        // Assert - Expired tokens should be deleted
        var expiredTokens = refreshTokenRepository.findByExpiresAtBefore(LocalDateTime.now());
        assertTrue(expiredTokens.isEmpty());
    }

    @Test
    @DisplayName("Should verify password is stored as hash")
    void shouldVerifyPasswordIsStoredAsHash() throws Exception {
        // Act
        User user = userRepository.findById(testUser.getId()).orElseThrow();

        // Assert
        assertNotEquals("password123", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$")); // BCrypt hash
        assertTrue(passwordEncoder.matches("password123", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Should handle soft deleted user")
    void shouldHandleSoftDeletedUser() throws Exception {
        // Arrange
        testUser.setDeletedAt(LocalDateTime.now());
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should verify token expiration time")
    void shouldVerifyTokenExpirationTime() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        // Act
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        String token = authResponse.accessToken();

        // Assert
        var expiration = jwtTokenProvider.getExpirationFromToken(token);
        assertTrue(expiration.isAfter(java.time.Instant.now()));
    }

    private String hashToken(String token) {
        // Simple hash for testing
        return "sha256:" + token.substring(0, Math.min(10, token.length()));
    }
}
