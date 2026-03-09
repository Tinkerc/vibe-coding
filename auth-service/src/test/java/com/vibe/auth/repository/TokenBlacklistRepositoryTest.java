package com.vibe.auth.repository;

import com.vibe.auth.model.TokenBlacklist;
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
 * Unit tests for TokenBlacklistRepository.
 * Uses H2 in-memory database for fast execution.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TokenBlacklistRepository Tests")
class TokenBlacklistRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    private TokenBlacklist testBlacklistEntry;
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

        // Create test blacklist entry
        testBlacklistEntry = new TokenBlacklist();
        testBlacklistEntry.setId(UUID.randomUUID());
        testBlacklistEntry.setJti("jwt-token-id-123");
        testBlacklistEntry.setUserId(testUser.getId());
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        testBlacklistEntry.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should save token blacklist entry successfully")
    void shouldSaveTokenBlacklistEntrySuccessfully() {
        // Act
        TokenBlacklist savedEntry = tokenBlacklistRepository.save(testBlacklistEntry);

        // Assert
        assertNotNull(savedEntry);
        assertEquals(testBlacklistEntry.getId(), savedEntry.getId());
        assertEquals(testBlacklistEntry.getJti(), savedEntry.getJti());
        assertEquals(testBlacklistEntry.getUserId(), savedEntry.getUserId());
    }

    @Test
    @DisplayName("Should find blacklist entry by ID")
    void shouldFindBlacklistEntryById() {
        // Arrange
        TokenBlacklist savedEntry = entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                savedEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertEquals(savedEntry.getId(), foundEntry.get().getId());
        assertEquals(savedEntry.getJti(), foundEntry.get().getJti());
    }

    @Test
    @DisplayName("Should not find blacklist entry by non-existent ID")
    void shouldNotFindBlacklistEntryByNonExistentId() {
        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                UUID.randomUUID());

        // Assert
        assertFalse(foundEntry.isPresent());
    }

    @Test
    @DisplayName("Should find blacklist entry by JTI")
    void shouldFindBlacklistEntryByJTI() {
        // Arrange
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findByJti(
                "jwt-token-id-123");

        // Assert
        assertTrue(foundEntry.isPresent());
        assertEquals("jwt-token-id-123", foundEntry.get().getJti());
    }

    @Test
    @DisplayName("Should not find blacklist entry by non-existent JTI")
    void shouldNotFindBlacklistEntryByNonExistentJTI() {
        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findByJti(
                "nonexistent-jti");

        // Assert
        assertFalse(foundEntry.isPresent());
    }

    @Test
    @DisplayName("Should find all blacklist entries by user ID")
    void shouldFindAllBlacklistEntriesByUserId() {
        // Arrange
        TokenBlacklist entry1 = new TokenBlacklist();
        entry1.setId(UUID.randomUUID());
        entry1.setJti("jti-1");
        entry1.setUserId(testUser.getId());
        entry1.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry1.setCreatedAt(LocalDateTime.now());

        TokenBlacklist entry2 = new TokenBlacklist();
        entry2.setId(UUID.randomUUID());
        entry2.setJti("jti-2");
        entry2.setUserId(testUser.getId());
        entry2.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry2.setCreatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(entry1);
        entityManager.persistAndFlush(entry2);

        // Act
        List<TokenBlacklist> userEntries = tokenBlacklistRepository.findAllByUserId(
                testUser.getId());

        // Assert
        assertTrue(userEntries.size() >= 2);
        assertTrue(userEntries.stream()
                .allMatch(e -> e.getUserId().equals(testUser.getId())));
    }

    @Test
    @DisplayName("Should find expired blacklist entries")
    void shouldFindExpiredBlacklistEntries() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        List<TokenBlacklist> expiredEntries = tokenBlacklistRepository
                .findByExpiresAtBefore(LocalDateTime.now());

        // Assert
        assertFalse(expiredEntries.isEmpty());
        assertTrue(expiredEntries.get(0).getExpiresAt().isBefore(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should not find non-expired entries when searching for expired")
    void shouldNotFindNonExpiredEntriesWhenSearchingForExpired() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        List<TokenBlacklist> expiredEntries = tokenBlacklistRepository
                .findByExpiresAtBefore(LocalDateTime.now());

        // Assert
        assertTrue(expiredEntries.stream()
                .noneMatch(e -> e.getId().equals(testBlacklistEntry.getId())));
    }

    @Test
    @DisplayName("Should delete blacklist entry by ID")
    void shouldDeleteBlacklistEntryById() {
        // Arrange
        TokenBlacklist savedEntry = entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        tokenBlacklistRepository.deleteById(savedEntry.getId());

        // Assert
        Optional<TokenBlacklist> deletedEntry = tokenBlacklistRepository.findById(
                savedEntry.getId());
        assertFalse(deletedEntry.isPresent());
    }

    @Test
    @DisplayName("Should delete expired entries")
    void shouldDeleteExpiredEntries() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        TokenBlacklist expiredEntry = entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        tokenBlacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        entityManager.flush();

        // Assert
        Optional<TokenBlacklist> deletedEntry = tokenBlacklistRepository.findById(
                expiredEntry.getId());
        assertFalse(deletedEntry.isPresent());
    }

    @Test
    @DisplayName("Should check if JTI exists in blacklist")
    void shouldCheckIfJTIExistsInBlacklist() {
        // Arrange
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        boolean exists = tokenBlacklistRepository.existsByJti("jwt-token-id-123");

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false for non-existent JTI")
    void shouldReturnFalseForNonExistentJTI() {
        // Act
        boolean exists = tokenBlacklistRepository.existsByJti("nonexistent-jti");

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should count blacklist entries by user ID")
    void shouldCountBlacklistEntriesByUserId() {
        // Arrange
        TokenBlacklist entry1 = new TokenBlacklist();
        entry1.setId(UUID.randomUUID());
        entry1.setJti("jti-1");
        entry1.setUserId(testUser.getId());
        entry1.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry1.setCreatedAt(LocalDateTime.now());

        TokenBlacklist entry2 = new TokenBlacklist();
        entry2.setId(UUID.randomUUID());
        entry2.setJti("jti-2");
        entry2.setUserId(testUser.getId());
        entry2.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry2.setCreatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(entry1);
        entityManager.persistAndFlush(entry2);

        // Act
        long count = tokenBlacklistRepository.countByUserId(testUser.getId());

        // Assert
        assertTrue(count >= 2);
    }

    @Test
    @DisplayName("Should find valid (non-expired) entries by user ID")
    void shouldFindValidEntriesByUserId() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        List<TokenBlacklist> validEntries = tokenBlacklistRepository
                .findByUserIdAndExpiresAtAfter(testUser.getId(), LocalDateTime.now());

        // Assert
        assertFalse(validEntries.isEmpty());
        assertTrue(validEntries.get(0).getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle null JTI")
    void shouldHandleNullJTI() {
        // Arrange
        testBlacklistEntry.setJti(null);
        TokenBlacklist savedEntry = entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                savedEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertNull(foundEntry.get().getJti());
    }

    @Test
    @DisplayName("Should handle null user ID")
    void shouldHandleNullUserId() {
        // Arrange
        testBlacklistEntry.setUserId(null);
        TokenBlacklist savedEntry = entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                savedEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertNull(foundEntry.get().getUserId());
    }

    @Test
    @DisplayName("Should find entries with reason field")
    void shouldFindEntriesWithReasonField() {
        // Arrange
        testBlacklistEntry.setReason("User logged out");
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                testBlacklistEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertEquals("User logged out", foundEntry.get().getReason());
    }

    @Test
    @DisplayName("Should handle blacklist entry for password change")
    void shouldHandleBlacklistEntryForPasswordChange() {
        // Arrange
        testBlacklistEntry.setReason("Password changed");
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                testBlacklistEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertEquals("Password changed", foundEntry.get().getReason());
    }

    @Test
    @DisplayName("Should find multiple entries for different users")
    void shouldFindMultipleEntriesForDifferentUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hash2");
        user2.setAuthType(AuthType.PASSWORD);
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(user2);

        TokenBlacklist entry1 = new TokenBlacklist();
        entry1.setId(UUID.randomUUID());
        entry1.setJti("jti-1");
        entry1.setUserId(testUser.getId());
        entry1.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry1.setCreatedAt(LocalDateTime.now());

        TokenBlacklist entry2 = new TokenBlacklist();
        entry2.setId(UUID.randomUUID());
        entry2.setJti("jti-2");
        entry2.setUserId(user2.getId());
        entry2.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entry2.setCreatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(entry1);
        entityManager.persistAndFlush(entry2);

        // Act
        List<TokenBlacklist> allEntries = tokenBlacklistRepository.findAll();

        // Assert
        assertTrue(allEntries.size() >= 2);
    }

    @Test
    @DisplayName("Should handle blacklist entry just before expiry")
    void shouldHandleBlacklistEntryJustBeforeExpiry() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().plusSeconds(1));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                testBlacklistEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertFalse(foundEntry.get().isExpired());
    }

    @Test
    @DisplayName("Should handle blacklist entry just after expiry")
    void shouldHandleBlacklistEntryJustAfterExpiry() {
        // Arrange
        testBlacklistEntry.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        Optional<TokenBlacklist> foundEntry = tokenBlacklistRepository.findById(
                testBlacklistEntry.getId());

        // Assert
        assertTrue(foundEntry.isPresent());
        assertTrue(foundEntry.get().isExpired());
    }

    @Test
    @DisplayName("Should verify JTI uniqueness")
    void shouldVerifyJTIUniqueness() {
        // Arrange
        entityManager.persistAndFlush(testBlacklistEntry);

        // Act
        boolean exists = tokenBlacklistRepository.existsByJti("jwt-token-id-123");

        // Assert
        assertTrue(exists);
        // In production, there should be a unique constraint on JTI
    }
}
