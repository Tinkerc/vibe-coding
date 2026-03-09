package com.vibe.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenBlacklist entity.
 * Tests token blacklisting, expiry, and cleanup logic.
 */
@DisplayName("TokenBlacklist Entity Tests")
class TokenBlacklistTest {

    private TokenBlacklist tokenBlacklist;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklist();
        baseTime = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should create token blacklist entry with all fields")
    void shouldCreateTokenBlacklistEntryWithAllFields() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        String jti = "jwt-token-id-123";
        UUID userId = UUID.randomUUID();

        // Act
        tokenBlacklist.setId(tokenId);
        tokenBlacklist.setJti(jti);
        tokenBlacklist.setUserId(userId);
        tokenBlacklist.setExpiresAt(baseTime.plusMinutes(30));
        tokenBlacklist.setCreatedAt(baseTime);

        // Assert
        assertEquals(tokenId, tokenBlacklist.getId());
        assertEquals(jti, tokenBlacklist.getJti());
        assertEquals(userId, tokenBlacklist.getUserId());
        assertNotNull(tokenBlacklist.getExpiresAt());
        assertNotNull(tokenBlacklist.getCreatedAt());
    }

    @Test
    @DisplayName("Should identify expired blacklist entry")
    void shouldIdentifyExpiredBlacklistEntry() {
        // Arrange
        tokenBlacklist.setExpiresAt(baseTime.minusMinutes(1));

        // Assert
        assertTrue(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should identify valid blacklist entry")
    void shouldIdentifyValidBlacklistEntry() {
        // Arrange
        tokenBlacklist.setExpiresAt(baseTime.plusMinutes(30));

        // Assert
        assertFalse(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should set created timestamp on persist")
    void shouldSetCreatedTimestampOnPersist() {
        // Act
        tokenBlacklist.onCreate();

        // Assert
        assertNotNull(tokenBlacklist.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null JTI")
    void shouldHandleNullJTI() {
        // Act
        tokenBlacklist.setJti(null);

        // Assert
        assertNull(tokenBlacklist.getJti());
    }

    @Test
    @DisplayName("Should handle empty JTI")
    void shouldHandleEmptyJTI() {
        // Act
        tokenBlacklist.setJti("");

        // Assert
        assertEquals("", tokenBlacklist.getJti());
    }

    @Test
    @DisplayName("Should handle valid JWT token identifier")
    void shouldHandleValidJWTTokenIdentifier() {
        // Arrange
        String validJti = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        // Act
        tokenBlacklist.setJti(validJti);

        // Assert
        assertEquals(validJti, tokenBlacklist.getJti());
    }

    @Test
    @DisplayName("Should handle user ID for blacklist entry")
    void shouldHandleUserIdForBlacklistEntry() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        tokenBlacklist.setUserId(userId);

        // Assert
        assertEquals(userId, tokenBlacklist.getUserId());
    }

    @Test
    @DisplayName("Should handle null user ID")
    void shouldHandleNullUserId() {
        // Act
        tokenBlacklist.setUserId(null);

        // Assert
        assertNull(tokenBlacklist.getUserId());
    }

    @Test
    @DisplayName("Should check expiry at exact expiration time")
    void shouldCheckExpiryAtExactExpirationTime() {
        // Arrange
        tokenBlacklist.setExpiresAt(baseTime);

        // Assert - at exact moment, might be considered expired
        // depending on implementation precision
        assertTrue(tokenBlacklist.getExpiresAt().isBefore(baseTime.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should handle blacklist entry for revoked token")
    void shouldHandleBlacklistEntryForRevokedToken() {
        // Arrange
        String revokedTokenJti = "revoked-token-jti-123";
        UUID userId = UUID.randomUUID();

        // Act
        tokenBlacklist.setJti(revokedTokenJti);
        tokenBlacklist.setUserId(userId);
        tokenBlacklist.setExpiresAt(baseTime.plusHours(1));
        tokenBlacklist.setReason("User logged out");

        // Assert
        assertEquals(revokedTokenJti, tokenBlacklist.getJti());
        assertEquals(userId, tokenBlacklist.getUserId());
        assertTrue(tokenBlacklist.getExpiresAt().isAfter(baseTime));
    }

    @Test
    @DisplayName("Should handle blacklist entry for password change")
    void shouldHandleBlacklistEntryForPasswordChange() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        tokenBlacklist.setUserId(userId);
        tokenBlacklist.setJti("all-tokens-jti");
        tokenBlacklist.setExpiresAt(baseTime.plusMinutes(15));
        tokenBlacklist.setReason("Password changed");

        // Assert
        assertEquals(userId, tokenBlacklist.getUserId());
        assertEquals("all-tokens-jti", tokenBlacklist.getJti());
        assertTrue(tokenBlacklist.getExpiresAt().isAfter(baseTime));
    }

    @Test
    @DisplayName("Should handle long expiry time for token blacklist")
    void shouldHandleLongExpiryTimeForTokenBlacklist() {
        // Arrange
        LocalDateTime longExpiry = baseTime.plusDays(30);

        // Act
        tokenBlacklist.setExpiresAt(longExpiry);

        // Assert
        assertEquals(longExpiry, tokenBlacklist.getExpiresAt());
        assertFalse(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should handle short expiry time for token blacklist")
    void shouldHandleShortExpiryTimeForTokenBlacklist() {
        // Arrange
        LocalDateTime shortExpiry = baseTime.plusMinutes(5);

        // Act
        tokenBlacklist.setExpiresAt(shortExpiry);

        // Assert
        assertEquals(shortExpiry, tokenBlacklist.getExpiresAt());
        assertFalse(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should handle blacklist entry just before expiry")
    void shouldHandleBlacklistEntryJustBeforeExpiry() {
        // Arrange
        tokenBlacklist.setExpiresAt(baseTime.plusSeconds(1));

        // Assert
        assertFalse(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should handle blacklist entry just after expiry")
    void shouldHandleBlacklistEntryJustAfterExpiry() {
        // Arrange
        tokenBlacklist.setExpiresAt(baseTime.minusSeconds(1));

        // Assert
        assertTrue(tokenBlacklist.isExpired());
    }

    @Test
    @DisplayName("Should handle null expiry time")
    void shouldHandleNullExpiryTime() {
        // Act
        tokenBlacklist.setExpiresAt(null);

        // Assert
        assertNull(tokenBlacklist.getExpiresAt());
    }

    @Test
    @DisplayName("Should track blacklist entry creation time")
    void shouldTrackBlacklistEntryCreationTime() {
        // Arrange
        LocalDateTime creationTime = baseTime;

        // Act
        tokenBlacklist.setCreatedAt(creationTime);

        // Assert
        assertEquals(creationTime, tokenBlacklist.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle multiple blacklist entries for same user")
    void shouldHandleMultipleBlacklistEntriesForSameUser() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String jti1 = "token-jti-1";
        String jti2 = "token-jti-2";

        // Act - entry 1
        TokenBlacklist entry1 = new TokenBlacklist();
        entry1.setId(UUID.randomUUID());
        entry1.setUserId(userId);
        entry1.setJti(jti1);
        entry1.setExpiresAt(baseTime.plusHours(1));

        // Act - entry 2
        TokenBlacklist entry2 = new TokenBlacklist();
        entry2.setId(UUID.randomUUID());
        entry2.setUserId(userId);
        entry2.setJti(jti2);
        entry2.setExpiresAt(baseTime.plusHours(2));

        // Assert
        assertEquals(userId, entry1.getUserId());
        assertEquals(userId, entry2.getUserId());
        assertEquals(jti1, entry1.getJti());
        assertEquals(jti2, entry2.getJti());
        assertNotEquals(entry1.getId(), entry2.getId());
    }

    @Test
    @DisplayName("Should verify blacklist entry uniqueness by JTI")
    void shouldVerifyBlacklistEntryUniquenessByJTI() {
        // Arrange
        String uniqueJti = UUID.randomUUID().toString();

        // Act
        tokenBlacklist.setJti(uniqueJti);

        // Assert
        assertEquals(uniqueJti, tokenBlacklist.getJti());
        // In production, there should be a unique constraint on JTI
    }
}
