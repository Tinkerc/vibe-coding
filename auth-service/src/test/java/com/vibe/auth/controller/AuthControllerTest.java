package com.vibe.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.dto.request.RefreshTokenRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.AuthType;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import com.vibe.auth.service.AuthService;
import com.vibe.auth.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 * Tests login, refresh, logout endpoints with various scenarios.
 */
@WebMvcTest(AuthController.class)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        testUser.setAuthType(AuthType.PASSWORD);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should return 200 on successful login")
    void shouldReturn200OnSuccessfulLogin() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        AuthResponse authResponse = new AuthResponse("jwt-token", "refresh-token", "Bearer", 1800);

        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt-token");
        when(tokenService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("Should return 401 on invalid credentials")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when user not found")
    void shouldReturn401WhenUserNotFound() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("User not found"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 on missing username")
    void shouldReturn400OnMissingUsername() throws Exception {
        // Arrange
        String invalidRequest = "{\"password\":\"password123\"}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 on missing password")
    void shouldReturn400OnMissingPassword() throws Exception {
        // Arrange
        String invalidRequest = "{\"username\":\"testuser\"}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 on empty request body")
    void shouldReturn400OnEmptyRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 on malformed JSON")
    void shouldReturn400OnMalformedJson() throws Exception {
        // Arrange
        String malformedJson = "{username:testuser,password:password123}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 200 on successful token refresh")
    void shouldReturn200OnSuccessfulTokenRefresh() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token", null);
        AuthResponse authResponse = new AuthResponse("new-access-token", "new-refresh-token", "Bearer", 1800);

        when(tokenService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("Should return 401 on invalid refresh token")
    void shouldReturn401OnInvalidRefreshToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token", null);
        when(tokenService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 on expired refresh token")
    void shouldReturn401OnExpiredRefreshToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token", null);
        when(tokenService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new IllegalArgumentException("Refresh token expired"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 204 on successful logout")
    void shouldReturn204OnSuccessfulLogout() throws Exception {
        // Arrange
        String accessToken = "valid-access-token";
        when(jwtTokenProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(accessToken)).thenReturn(testUser.getId());

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 on logout without authentication")
    void shouldReturn401OnLogoutWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 on logout with invalid token")
    void shouldReturn401OnLogoutWithInvalidToken() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle refresh with idempotency key")
    void shouldHandleRefreshWithIdempotencyKey() throws Exception {
        // Arrange
        String idempotencyKey = "unique-key-123";
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token", idempotencyKey);
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "Bearer", 1800);

        when(tokenService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Should handle refresh with device ID")
    void shouldHandleRefreshWithDeviceId() throws Exception {
        // Arrange
        String deviceId = "mobile-device-123";
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token", null, deviceId);
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "Bearer", 1800);

        when(tokenService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Should return 415 on unsupported media type")
    void shouldReturn415OnUnsupportedMediaType() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("username=testuser&password=password123"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should handle concurrent login requests")
    void shouldHandleConcurrentLoginRequests() throws Exception {
        // This test documents the expected behavior for concurrent requests
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt-token-1", "jwt-token-2");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return JSON content type")
    void shouldReturnJsonContentType() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle very long username")
    void shouldHandleVeryLongUsername() throws Exception {
        // Arrange
        String longUsername = "a".repeat(1000);
        LoginRequest request = new LoginRequest(longUsername, "password123");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void shouldHandleSpecialCharactersInUsername() throws Exception {
        // Arrange
        String specialUsername = "user+test@example.com";
        LoginRequest request = new LoginRequest(specialUsername, "password123");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle null values in request")
    void shouldHandleNullValuesInRequest() throws Exception {
        // Arrange
        String requestWithNulls = "{\"username\":null,\"password\":null}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithNulls))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate request body structure")
    void shouldValidateRequestBodyStructure() throws Exception {
        // Arrange
        String invalidStructure = "{\"invalid\":\"field\"}";

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStructure))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty username")
    void shouldHandleEmptyUsername() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty password")
    void shouldHandleEmptyPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle timeout scenarios")
    void shouldHandleTimeoutScenarios() throws Exception {
        // This test documents the expected behavior for slow responses
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt-token");

        // Act & Assert
        long startTime = System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        long endTime = System.currentTimeMillis();

        // Verify response is reasonable (within 5 seconds)
        assertTrue(endTime - startTime < 5000);
    }
}
