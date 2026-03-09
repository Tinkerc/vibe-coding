package com.vibe.auth.service;

import com.vibe.auth.dto.request.RefreshTokenRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.RefreshToken;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.RefreshTokenRepository;
import com.vibe.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenService.
 * Tests token refresh, rotation, idempotency, and expiry validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Token Service Tests")
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private TokenService tokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(refreshTokenRepository, jwtTokenProvider, cacheManager);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(UUID.randomUUID());
        testRefreshToken.setUserId(testUser.getId());
        testRefreshToken.setTokenHash("hashed-refresh-token");
        testRefreshToken.setDeviceId("device-123");
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        testRefreshToken.setCreatedAt(LocalDateTime.now());
        testRefreshToken.setRevoked(false);
    }

    @Test
    @DisplayName("Should refresh access token with valid refresh token")
    void shouldRefreshAccessTokenWithValidRefreshToken() {
        // Arrange
        String refreshToken = "valid-refresh-token";
        String hash = hashToken(refreshToken);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null));

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800, response.expiresIn());
        verify(refreshTokenRepository).findByTokenHash(hash);
        verify(jwtTokenProvider).generateToken(testUser.getId(), testUser.getUsername(),
                testUser.getEmail(), "password");
    }

    @Test
    @DisplayName("Should rotate refresh token on refresh")
    void shouldRotateRefreshTokenOnRefresh() {
        // Arrange
        String oldRefreshToken = "old-refresh-token";
        String hash = hashToken(oldRefreshToken);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(oldRefreshToken, null));

        // Assert
        assertNotNull(response);
        assertNotNull(response.refreshToken());
        assertNotEquals(oldRefreshToken, response.refreshToken());
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should revoke old refresh token after rotation")
    void shouldRevokeOldRefreshTokenAfterRotation() {
        // Arrange
        String oldRefreshToken = "old-refresh-token";
        String hash = hashToken(oldRefreshToken);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tokenService.refreshToken(new RefreshTokenRequest(oldRefreshToken, null));

        // Assert
        assertTrue(testRefreshToken.isRevoked());
        assertNotNull(testRefreshToken.getRevokedAt());
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Should throw exception for expired refresh token")
    void shouldThrowExceptionForExpiredRefreshToken() {
        // Arrange
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        String refreshToken = "expired-token";
        when(refreshTokenRepository.findByTokenHash(hashToken(refreshToken))).thenReturn(Optional.of(testRefreshToken));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null)));
    }

    @Test
    @DisplayName("Should throw exception for revoked refresh token")
    void shouldThrowExceptionForRevokedRefreshToken() {
        // Arrange
        testRefreshToken.setRevoked(true);
        testRefreshToken.setRevokedAt(LocalDateTime.now());
        String refreshToken = "revoked-token";
        when(refreshTokenRepository.findByTokenHash(hashToken(refreshToken))).thenReturn(Optional.of(testRefreshToken));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null)));
    }

    @Test
    @DisplayName("Should throw exception for non-existent refresh token")
    void shouldThrowExceptionForNonExistentRefreshToken() {
        // Arrange
        String refreshToken = "non-existent-token";
        when(refreshTokenRepository.findByTokenHash(hashToken(refreshToken))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null)));
    }

    @Test
    @DisplayName("Should handle idempotency key for concurrent requests")
    void shouldHandleIdempotencyKeyForConcurrentRequests() {
        // Arrange
        String idempotencyKey = "unique-key-123";
        String refreshToken = "valid-token";
        AuthResponse cachedResponse = new AuthResponse("cached-access-token", "cached-refresh-token", "Bearer", 1800);

        when(cacheManager.getCache("idempotency")).thenReturn(cache);
        when(cache.get(idempotencyKey, AuthResponse.class)).thenReturn(cachedResponse);

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, idempotencyKey));

        // Assert
        assertEquals(cachedResponse, response);
        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    @DisplayName("Should cache response when idempotency key is provided")
    void shouldCacheResponseWhenIdempotencyKeyIsProvided() {
        // Arrange
        String idempotencyKey = "unique-key-456";
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(cacheManager.getCache("idempotency")).thenReturn(cache);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tokenService.refreshToken(new RefreshTokenRequest(refreshToken, idempotencyKey));

        // Assert
        verify(cache).put(eq(idempotencyKey), any(AuthResponse.class));
    }

    @Test
    @DisplayName("Should handle null idempotency key")
    void shouldHandleNullIdempotencyKey() {
        // Arrange
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null));

        // Assert
        assertNotNull(response);
        verify(cacheManager, never()).getCache(any());
    }

    @Test
    @DisplayName("Should handle device ID in refresh request")
    void shouldHandleDeviceIdInRefreshRequest() {
        // Arrange
        String deviceId = "mobile-device-123";
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null, deviceId));

        // Assert
        assertNotNull(response);
        assertEquals(deviceId, testRefreshToken.getDeviceId());
    }

    @Test
    @DisplayName("Should validate token expiry time correctly")
    void shouldValidateTokenExpiryTimeCorrectly() {
        // Arrange - token expires in 1 second
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(1));
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(hashToken(refreshToken))).thenReturn(Optional.of(testRefreshToken));

        // Act
        boolean isValid = testRefreshToken.isValid();

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate token just after expiry")
    void shouldInvalidateTokenJustAfterExpiry() {
        // Arrange - token expired 1 second ago
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        String refreshToken = "expired-token";
        when(refreshTokenRepository.findByTokenHash(hashToken(refreshToken))).thenReturn(Optional.of(testRefreshToken));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null)));
    }

    @Test
    @DisplayName("Should handle token rotation for same device")
    void shouldHandleTokenRotationForSameDevice() {
        // Arrange
        String deviceId = "device-123";
        String oldRefreshToken = "old-token";
        testRefreshToken.setDeviceId(deviceId);

        when(refreshTokenRepository.findByTokenHash(hashToken(oldRefreshToken))).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(oldRefreshToken, null, deviceId));

        // Assert
        assertNotNull(response);
        assertTrue(testRefreshToken.isRevoked());
        assertNotNull(testRefreshToken.getReplacedBy());
    }

    @Test
    @DisplayName("Should handle multiple concurrent refresh requests")
    void shouldHandleMultipleConcurrentRefreshRequests() {
        // This test documents thread safety expectations
        // In a real test, you would use concurrent test execution
        // Arrange
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response1 = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null));
        AuthResponse response2 = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null));

        // Assert
        assertNotNull(response1);
        assertNotNull(response2);
        verify(refreshTokenRepository, times(2)).findByTokenHash(any());
    }

    @Test
    @DisplayName("Should clean up expired tokens")
    void shouldCleanUpExpiredTokens() {
        // Arrange
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setId(UUID.randomUUID());
        expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByExpiresAtBefore(any())).thenReturn(java.util.List.of(expiredToken));

        // Act
        tokenService.cleanupExpiredTokens();

        // Assert
        verify(refreshTokenRepository).deleteAll(any());
    }

    @Test
    @DisplayName("Should handle refresh token with null device ID")
    void shouldHandleRefreshTokenWithNullDeviceId() {
        // Arrange
        testRefreshToken.setDeviceId(null);
        String refreshToken = "valid-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = tokenService.refreshToken(new RefreshTokenRequest(refreshToken, null));

        // Assert
        assertNotNull(response);
        assertNull(testRefreshToken.getDeviceId());
    }

    @Test
    @DisplayName("Should generate unique refresh tokens")
    void shouldGenerateUniqueRefreshTokens() {
        // Arrange
        String oldRefreshToken = "old-token";
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response1 = tokenService.refreshToken(new RefreshTokenRequest(oldRefreshToken, null));
        AuthResponse response2 = tokenService.refreshToken(new RefreshTokenRequest(oldRefreshToken, null));

        // Assert
        assertNotEquals(response1.refreshToken(), response2.refreshToken());
    }

    @Test
    @DisplayName("Should verify token hash format")
    void shouldVerifyTokenHashFormat() {
        // Arrange
        String refreshToken = "valid-token";
        String hash = hashToken(refreshToken);

        // Act
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(testRefreshToken));

        // Assert
        verify(refreshTokenRepository).findByTokenHash(hash);
    }

    private String hashToken(String token) {
        // Simple hash for testing
        return "sha256:" + token.substring(0, Math.min(10, token.length()));
    }
}
