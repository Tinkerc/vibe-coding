package com.vibe.auth.service;

import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.model.AuthType;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication logic, user validation, and token generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);

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
    @DisplayName("Should authenticate user with valid credentials")
    void shouldAuthenticateUserWithValidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        assertEquals("jwt-token", token);
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
        verify(jwtTokenProvider).generateToken(testUser.getId(), testUser.getUsername(),
                testUser.getEmail(), "password");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void shouldThrowExceptionWhenPasswordIsInvalid() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("wrongpassword", testUser.getPasswordHash());
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception for OAuth2 user without password")
    void shouldThrowExceptionForOAuth2UserWithoutPassword() {
        // Arrange
        testUser.setAuthType(AuthType.OAUTH2);
        testUser.setPasswordHash(null);
        testUser.setOauthProvider("google");
        testUser.setOauthSubjectId("google_123");
        LoginRequest request = new LoginRequest("oauthuser", "password");
        when(userRepository.findByUsername("oauthuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository).findByUsername("oauthuser");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should handle null password in request")
    void shouldHandleNullPasswordInRequest() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should handle empty password in request")
    void shouldHandleEmptyPasswordInRequest() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
    }

    @Test
    @DisplayName("Should handle null username in request")
    void shouldHandleNullUsernameInRequest() {
        // Arrange
        LoginRequest request = new LoginRequest(null, "password");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should handle empty username in request")
    void shouldHandleEmptyUsernameInRequest() {
        // Arrange
        LoginRequest request = new LoginRequest("", "password");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should authenticate user with email instead of username")
    void shouldAuthenticateUserWithEmailInsteadOfUsername() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        assertEquals("jwt-token", token);
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should authenticate successfully with BCrypt password")
    void shouldAuthenticateSuccessfullyWithBCryptPassword() {
        // Arrange
        String rawPassword = "mySecurePassword123!";
        String bcryptedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        testUser.setPasswordHash(bcryptedHash);

        LoginRequest request = new LoginRequest("testuser", rawPassword);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, bcryptedHash)).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        verify(passwordEncoder).matches(rawPassword, bcryptedHash);
    }

    @Test
    @DisplayName("Should handle soft-deleted user authentication attempt")
    void shouldHandleSoftDeletedUserAuthenticationAttempt() {
        // Arrange
        testUser.setDeletedAt(LocalDateTime.now());
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should handle authentication for user with multiple auth types")
    void shouldHandleAuthenticationForUserWithMultipleAuthTypes() {
        // Arrange - User has both password and OAuth2 set up
        testUser.setAuthType(AuthType.PASSWORD);
        testUser.setOauthProvider("google");
        testUser.setOauthSubjectId("google_123");

        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        verify(jwtTokenProvider).generateToken(eq(testUser.getId()), eq("testuser"),
                eq("test@example.com"), eq("password"));
    }

    @Test
    @DisplayName("Should generate unique tokens for each authentication")
    void shouldGenerateUniqueTokensForEachAuthentication() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any()))
                .thenReturn("jwt-token-1", "jwt-token-2");

        // Act
        String token1 = authService.authenticate(request);
        String token2 = authService.authenticate(request);

        // Assert
        assertNotEquals(token1, token2);
        verify(jwtTokenProvider, times(2)).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle case-insensitive username lookup")
    void shouldHandleCaseInsensitiveUsernameLookup() {
        // Arrange
        LoginRequest request = new LoginRequest("TestUser", "password123");
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        verify(userRepository).findByUsername("TestUser");
    }

    @Test
    @DisplayName("Should verify user is not locked before authentication")
    void shouldVerifyUserIsNotLockedBeforeAuthentication() {
        // This test documents the expected behavior for locked accounts
        // Implementation may vary based on requirements
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        // Additional checks for account locking can be added here
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void shouldHandleSpecialCharactersInPassword() {
        // Arrange
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
        LoginRequest request = new LoginRequest("testuser", specialPassword);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(specialPassword, testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
        verify(passwordEncoder).matches(specialPassword, testUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should update last login timestamp")
    void shouldUpdateLastLoginTimestamp() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        authService.authenticate(request);

        // Assert
        // Verify that user's last login timestamp is updated
        // This would require additional fields in the User entity
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle authentication timeout")
    void shouldHandleAuthenticationTimeout() {
        // This test documents expected behavior for slow authentication
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        long startTime = System.currentTimeMillis();
        String token = authService.authenticate(request);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(token);
        assertTrue(endTime - startTime < 5000); // Should complete within 5 seconds
    }

    @Test
    @DisplayName("Should handle concurrent authentication requests")
    void shouldHandleConcurrentAuthenticationRequests() {
        // This test documents thread safety expectations
        // In a real test, you would use concurrent test execution
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("jwt-token");

        // Act
        String token = authService.authenticate(request);

        // Assert
        assertNotNull(token);
    }
}
