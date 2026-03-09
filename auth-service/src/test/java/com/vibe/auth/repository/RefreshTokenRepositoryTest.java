package com.vibe.auth.repository;

import com.vibe.auth.model.RefreshToken;
import com.vibe.auth.model.User;
import com.vibe.auth.model.AuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RefreshTokenRepository.
 * Uses H2 in-memory database for fast execution.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository Tests")
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken testToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$encodedPassword");
        testUser.setAuthType(AuthType.PASSWORD);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(testUser);

        // Create test refresh token
        testToken = new RefreshToken();
        testToken.setId(UUID.randomUUID());
        testToken.setUserId(testUser.getId());
        testToken.setTokenHash("hashed_token_value");
        testToken.setDeviceId("device_123");
        testToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        testToken.setCreatedAt(LocalDateTime.now());
        testToken.setRevoked(false);
    }

    @Test
    @DisplayName("Should save refresh token successfully")
    void shouldSaveRefreshTokenSuccessfully() {
        // Act
        RefreshToken savedToken = refreshTokenRepository.save(testToken);

        // Assert
        assertNotNull(savedToken);
        assertEquals(testToken.getId(), savedToken.getId());
        assertEquals(testToken.getUserId(), savedToken.getUserId());
        assertEquals(testToken.getTokenHash(), savedToken.getTokenHash());
    }

    @Test
    @DisplayName("Should find token by ID")
    void shouldFindTokenById() {
        // Arrange
        RefreshToken savedToken = entityManager.persistAndFlush(testToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findById(savedToken.getId());

        // Assert
        assertTrue(foundToken.isPresent());
        assertEquals(savedToken.getId(), foundToken.get().getId());
        assertEquals(savedToken.getTokenHash(), foundToken.get().getTokenHash());
    }

    @Test
    @DisplayName("Should not find token by non-existent ID")
    void shouldNotFindTokenByNonExistentId() {
        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findById(UUID.randomUUID());

        // Assert
        assertFalse(foundToken.isPresent());
    }

    @Test
    @DisplayName("Should find valid tokens by user ID")
    void shouldFindValidTokensByUserId() {
        // Arrange
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(
                testUser.getId());

        // Assert
        assertFalse(userTokens.isEmpty());
        assertEquals(testUser.getId(), userTokens.get(0).getUserId());
        assertFalse(userTokens.get(0).isRevoked());
    }

    @Test
    @DisplayName("Should not find revoked tokens when searching for valid tokens")
    void shouldNotFindRevokedTokensWhenSearchingForValidTokens() {
        // Arrange
        testToken.setRevoked(true);
        testToken.setRevokedAt(LocalDateTime.now());
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(
                testUser.getId());

        // Assert
        assertTrue(userTokens.isEmpty());
    }

    @Test
    @DisplayName("Should find token by token hash")
    void shouldFindTokenByTokenHash() {
        // Arrange
        entityManager.persistAndFlush(testToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash(
                "hashed_token_value");

        // Assert
        assertTrue(foundToken.isPresent());
        assertEquals("hashed_token_value", foundToken.get().getTokenHash());
    }

    @Test
    @DisplayName("Should not find token by non-existent token hash")
    void shouldNotFindTokenByNonExistentTokenHash() {
        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash(
                "nonexistent_hash");

        // Assert
        assertFalse(foundToken.isPresent());
    }

    @Test
    @DisplayName("Should find token by user ID and device ID")
    void shouldFindTokenByUserIdAndDeviceId() {
        // Arrange
        entityManager.persistAndFlush(testToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserIdAndDeviceId(
                testUser.getId(), "device_123");

        // Assert
        assertTrue(foundToken.isPresent());
        assertEquals(testUser.getId(), foundToken.get().getUserId());
        assertEquals("device_123", foundToken.get().getDeviceId());
    }

    @Test
    @DisplayName("Should not find token by non-existent user ID and device ID")
    void shouldNotFindTokenByNonExistentUserIdAndDeviceId() {
        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserIdAndDeviceId(
                UUID.randomUUID(), "device_123");

        // Assert
        assertFalse(foundToken.isPresent());
    }

    @Test
    @DisplayName("Should find all tokens by user ID")
    void shouldFindAllTokensByUserId() {
        // Arrange
        RefreshToken token1 = new RefreshToken();
        token1.setId(UUID.randomUUID());
        token1.setUserId(testUser.getId());
        token1.setTokenHash("hash1");
        token1.setDeviceId("device1");
        token1.setExpiresAt(LocalDateTime.now().plusDays(7));
        token1.setCreatedAt(LocalDateTime.now());
        token1.setRevoked(false);

        RefreshToken token2 = new RefreshToken();
        token2.setId(UUID.randomUUID());
        token2.setUserId(testUser.getId());
        token2.setTokenHash("hash2");
        token2.setDeviceId("device2");
        token2.setExpiresAt(LocalDateTime.now().plusDays(7));
        token2.setCreatedAt(LocalDateTime.now());
        token2.setRevoked(false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);

        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findAllByUserId(
                testUser.getId());

        // Assert
        assertTrue(userTokens.size() >= 2);
    }

    @Test
    @DisplayName("Should find expired tokens")
    void shouldFindExpiredTokens() {
        // Arrange
        testToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> expiredTokens = refreshTokenRepository.findByExpiresAtBefore(
                LocalDateTime.now());

        // Assert
        assertFalse(expiredTokens.isEmpty());
        assertTrue(expiredTokens.get(0).getExpiresAt().isBefore(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should not find non-expired tokens when searching for expired")
    void shouldNotFindNonExpiredTokensWhenSearchingForExpired() {
        // Arrange
        testToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> expiredTokens = refreshTokenRepository.findByExpiresAtBefore(
                LocalDateTime.now());

        // Assert
        assertTrue(expiredTokens.stream()
                .noneMatch(t -> t.getId().equals(testToken.getId())));
    }

    @Test
    @DisplayName("Should find revoked tokens")
    void shouldFindRevokedTokens() {
        // Arrange
        testToken.setRevoked(true);
        testToken.setRevokedAt(LocalDateTime.now());
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> revokedTokens = refreshTokenRepository.findByRevokedTrue();

        // Assert
        assertFalse(revokedTokens.isEmpty());
        assertTrue(revokedTokens.get(0).isRevoked());
    }

    @Test
    @DisplayName("Should not find non-revoked tokens when searching for revoked")
    void shouldNotFindNonRevokedTokensWhenSearchingForRevoked() {
        // Arrange
        testToken.setRevoked(false);
        entityManager.persistAndFlush(testToken);

        // Act
        List<RefreshToken> revokedTokens = refreshTokenRepository.findByRevokedTrue();

        // Assert
        assertTrue(revokedTokens.stream()
                .noneMatch(t -> t.getId().equals(testToken.getId())));
    }

    @Test
    @DisplayName("Should delete token by ID")
    void shouldDeleteTokenById() {
        // Arrange
        RefreshToken savedToken = entityManager.persistAndFlush(testToken);

        // Act
        refreshTokenRepository.deleteById(savedToken.getId());

        // Assert
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findById(savedToken.getId());
        assertFalse(deletedToken.isPresent());
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeTokenSuccessfully() {
        // Arrange
        RefreshToken savedToken = entityManager.persistAndFlush(testToken);
        savedToken.setRevoked(true);
        savedToken.setRevokedAt(LocalDateTime.now());

        // Act
        RefreshToken revokedToken = refreshTokenRepository.save(savedToken);

        // Assert
        assertTrue(revokedToken.isRevoked());
        assertNotNull(revokedToken.getRevokedAt());
    }

    @Test
    @DisplayName("Should handle token rotation")
    void shouldHandleTokenRotation() {
        // Arrange
        RefreshToken oldToken = entityManager.persistAndFlush(testToken);
        UUID newTokenId = UUID.randomUUID();

        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        oldToken.setReplacedBy(newTokenId);
        entityManager.persistAndFlush(oldToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findById(oldToken.getId());

        // Assert
        assertTrue(foundToken.isPresent());
        assertTrue(foundToken.get().isRevoked());
        assertEquals(newTokenId, foundToken.get().getReplacedBy());
    }

    @Test
    @DisplayName("Should count tokens by user ID")
    void shouldCountTokensByUserId() {
        // Arrange
        RefreshToken token1 = new RefreshToken();
        token1.setId(UUID.randomUUID());
        token1.setUserId(testUser.getId());
        token1.setTokenHash("hash1");
        token1.setDeviceId("device1");
        token1.setExpiresAt(LocalDateTime.now().plusDays(7));
        token1.setCreatedAt(LocalDateTime.now());
        token1.setRevoked(false);

        RefreshToken token2 = new RefreshToken();
        token2.setId(UUID.randomUUID());
        token2.setUserId(testUser.getId());
        token2.setTokenHash("hash2");
        token2.setDeviceId("device2");
        token2.setExpiresAt(LocalDateTime.now().plusDays(7));
        token2.setCreatedAt(LocalDateTime.now());
        token2.setRevoked(false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);

        // Act
        long count = refreshTokenRepository.countByUserId(testUser.getId());

        // Assert
        assertTrue(count >= 2);
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void shouldDeleteExpiredTokens() {
        // Arrange
        testToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        RefreshToken expiredToken = entityManager.persistAndFlush(testToken);

        // Act
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        entityManager.flush();

        // Assert
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findById(expiredToken.getId());
        assertFalse(deletedToken.isPresent());
    }

    @Test
    @DisplayName("Should handle null device ID")
    void shouldHandleNullDeviceId() {
        // Arrange
        testToken.setDeviceId(null);
        RefreshToken savedToken = entityManager.persistAndFlush(testToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findById(savedToken.getId());

        // Assert
        assertTrue(foundToken.isPresent());
        assertNull(foundToken.get().getDeviceId());
    }

    @Test
    @DisplayName("Should find tokens without replacement")
    void shouldFindTokensWithoutReplacement() {
        // Arrange
        testToken.setReplacedBy(null);
        entityManager.persistAndFlush(testToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findById(testToken.getId());

        // Assert
        assertTrue(foundToken.isPresent());
        assertNull(foundToken.get().getReplacedBy());
    }
}
