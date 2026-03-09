package com.vibe.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RefreshToken entity.
 * Tests token lifecycle, validation, and revocation logic.
 */
@DisplayName("RefreshToken Entity Tests")
class RefreshTokenTest {

    private RefreshToken refreshToken;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        refreshToken = new RefreshToken();
        baseTime = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should create refresh token with all fields")
    void shouldCreateRefreshTokenWithAllFields() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String tokenHash = "hashed_token_value";
        String deviceId = "device_123";

        // Act
        refreshToken.setId(tokenId);
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setDeviceId(deviceId);
        refreshToken.setExpiresAt(baseTime.plusDays(7));
        refreshToken.setCreatedAt(baseTime);

        // Assert
        assertEquals(tokenId, refreshToken.getId());
        assertEquals(userId, refreshToken.getUserId());
        assertEquals(tokenHash, refreshToken.getTokenHash());
        assertEquals(deviceId, refreshToken.getDeviceId());
        assertFalse(refreshToken.isRevoked());
        assertNotNull(refreshToken.getExpiresAt());
        assertNotNull(refreshToken.getCreatedAt());
    }

    @Test
    @DisplayName("Should revoke token correctly")
    void shouldRevokeTokenCorrectly() {
        // Arrange
        LocalDateTime revokeTime = baseTime;

        // Act
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(revokeTime);

        // Assert
        assertTrue(refreshToken.isRevoked());
        assertEquals(revokeTime, refreshToken.getRevokedAt());
    }

    @Test
    @DisplayName("Should validate active token")
    void shouldValidateActiveToken() {
        // Arrange
        refreshToken.setExpiresAt(baseTime.plusDays(7));
        refreshToken.setRevoked(false);

        // Assert
        assertFalse(refreshToken.isRevoked());
        assertTrue(refreshToken.isValid());
    }

    @Test
    @DisplayName("Should invalidate expired token")
    void shouldInvalidateExpiredToken() {
        // Arrange
        refreshToken.setExpiresAt(baseTime.minusDays(1));
        refreshToken.setRevoked(false);

        // Assert
        assertFalse(refreshToken.isValid());
    }

    @Test
    @DisplayName("Should invalidate revoked token")
    void shouldInvalidateRevokedToken() {
        // Arrange
        refreshToken.setExpiresAt(baseTime.plusDays(7));
        refreshToken.setRevoked(true);

        // Assert
        assertFalse(refreshToken.isValid());
    }

    @Test
    @DisplayName("Should invalidate expired and revoked token")
    void shouldInvalidateExpiredAndRevokedToken() {
        // Arrange
        refreshToken.setExpiresAt(baseTime.minusDays(1));
        refreshToken.setRevoked(true);

        // Assert
        assertFalse(refreshToken.isValid());
    }

    @Test
    @DisplayName("Should set created timestamp on persist")
    void shouldSetCreatedTimestampOnPersist() {
        // Act
        refreshToken.onCreate();

        // Assert
        assertNotNull(refreshToken.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null device ID")
    void shouldHandleNullDeviceId() {
        // Act
        refreshToken.setDeviceId(null);

        // Assert
        assertNull(refreshToken.getDeviceId());
    }

    @Test
    @DisplayName("Should handle empty device ID")
    void shouldHandleEmptyDeviceId() {
        // Act
        refreshToken.setDeviceId("");

        // Assert
        assertEquals("", refreshToken.getDeviceId());
    }

    @Test
    @DisplayName("Should validate token hash is not null for valid token")
    void shouldValidateTokenHashIsNotNullForValidToken() {
        // Arrange
        String tokenHash = "secure_hashed_token_value";

        // Act
        refreshToken.setTokenHash(tokenHash);

        // Assert
        assertEquals(tokenHash, refreshToken.getTokenHash());
        assertNotNull(refreshToken.getTokenHash());
    }

    @Test
    @DisplayName("Should handle token rotation scenario")
    void shouldHandleTokenRotationScenario() {
        // Arrange - old token
        UUID oldTokenId = UUID.randomUUID();
        refreshToken.setId(oldTokenId);
        refreshToken.setTokenHash("old_token_hash");
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(baseTime);
        refreshToken.setReplacedBy(UUID.randomUUID());

        // Assert
        assertEquals(oldTokenId, refreshToken.getId());
        assertEquals("old_token_hash", refreshToken.getTokenHash());
        assertTrue(refreshToken.isRevoked());
        assertNotNull(refreshToken.getReplacedBy());
    }

    @Test
    @DisplayName("Should check token expiry status correctly")
    void shouldCheckTokenExpiryStatusCorrectly() {
        // Arrange - not expired
        refreshToken.setExpiresAt(baseTime.plusDays(7));

        // Assert
        assertFalse(refreshToken.isExpired());

        // Act - make it expired
        refreshToken.setExpiresAt(baseTime.minusMinutes(1));

        // Assert
        assertTrue(refreshToken.isExpired());
    }

    @Test
    @DisplayName("Should handle token with user ID")
    void shouldHandleTokenWithUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        refreshToken.setUserId(userId);

        // Assert
        assertEquals(userId, refreshToken.getUserId());
    }

    @Test
    @DisplayName("Should handle null user ID for new token")
    void shouldHandleNullUserIdForNewToken() {
        // Assert
        assertNull(refreshToken.getUserId());
    }

    @Test
    @DisplayName("Should validate token expiry time is in future for new tokens")
    void shouldValidateTokenExpiryTimeIsInFutureForNewTokens() {
        // Act
        refreshToken.setExpiresAt(baseTime.plusDays(7));

        // Assert
        assertTrue(refreshToken.getExpiresAt().isAfter(baseTime));
    }

    @Test
    @DisplayName("Should handle token revocation without replacement")
    void shouldHandleTokenRevocationWithoutReplacement() {
        // Act
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(baseTime);

        // Assert
        assertTrue(refreshToken.isRevoked());
        assertNotNull(refreshToken.getRevokedAt());
        assertNull(refreshToken.getReplacedBy());
    }

    @Test
    @DisplayName("Should track multiple tokens per user with different device IDs")
    void shouldTrackMultipleTokensPerUserWithDifferentDeviceIds() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String device1 = "mobile-device-123";
        String device2 = "desktop-device-456";

        // Act - token 1
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setDeviceId(device1);

        // Assert
        assertEquals(userId, refreshToken.getUserId());
        assertEquals(device1, refreshToken.getDeviceId());

        // Act - token 2 (simulated)
        RefreshToken token2 = new RefreshToken();
        token2.setId(UUID.randomUUID());
        token2.setUserId(userId);
        token2.setDeviceId(device2);

        // Assert
        assertEquals(userId, token2.getUserId());
        assertEquals(device2, token2.getDeviceId());
        assertNotEquals(refreshToken.getId(), token2.getId());
    }

    @Test
    @DisplayName("Should handle edge case of null expiry time")
    void shouldHandleEdgeCaseOfNullExpiryTime() {
        // Act
        refreshToken.setExpiresAt(null);

        // Assert
        assertNull(refreshToken.getExpiresAt());
    }

    @Test
    @DisplayName("Should handle token just before expiry")
    void shouldHandleTokenJustBeforeExpiry() {
        // Arrange - token expires in 1 second
        refreshToken.setExpiresAt(baseTime.plusSeconds(1));
        refreshToken.setRevoked(false);

        // Assert
        assertTrue(refreshToken.isValid());
    }

    @Test
    @DisplayName("Should handle token just after expiry")
    void shouldHandleTokenJustAfterExpiry() {
        // Arrange - token expired 1 second ago
        refreshToken.setExpiresAt(baseTime.minusSeconds(1));
        refreshToken.setRevoked(false);

        // Assert
        assertFalse(refreshToken.isValid());
    }
}
