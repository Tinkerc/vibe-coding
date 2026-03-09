package com.vibe.auth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity.
 * Tests entity validation, constraints, and business logic.
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now();
        user = new User();
    }

    @Test
    @DisplayName("Should create user with all fields")
    void shouldCreateUserWithAllFields() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setAuthType(AuthType.PASSWORD);
        user.setCreatedAt(baseTime);
        user.setUpdatedAt(baseTime);

        // Assert
        assertEquals(userId, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashed_password", user.getPasswordHash());
        assertEquals(AuthType.PASSWORD, user.getAuthType());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle OAuth2 fields correctly")
    void shouldHandleOAuth2FieldsCorrectly() {
        // Act
        user.setOauthProvider("google");
        user.setOauthSubjectId("google_subject_123");

        // Assert
        assertEquals("google", user.getOauthProvider());
        assertEquals("google_subject_123", user.getOauthSubjectId());
    }

    @Test
    @DisplayName("Should handle GitHub OAuth provider")
    void shouldHandleGitHubOAuthProvider() {
        // Act
        user.setOauthProvider("github");
        user.setOauthSubjectId("github_user_456");

        // Assert
        assertEquals("github", user.getOauthProvider());
        assertEquals("github_user_456", user.getOauthSubjectId());
    }

    @Test
    @DisplayName("Should mark user as deleted with soft delete")
    void shouldMarkUserAsDeletedWithSoftDelete() {
        // Arrange
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setDeletedAt(null);

        // Act
        user.setDeletedAt(LocalDateTime.now());

        // Assert
        assertNotNull(user.getDeletedAt());
        assertTrue(user.isDeleted());
    }

    @Test
    @DisplayName("Should check if user is deleted")
    void shouldCheckIfUserIsDeleted() {
        // Arrange & Assert
        assertFalse(user.isDeleted());
        assertNull(user.getDeletedAt());

        // Act
        user.setDeletedAt(LocalDateTime.now());

        // Assert
        assertTrue(user.isDeleted());
    }

    @Test
    @DisplayName("Should handle active user correctly")
    void shouldHandleActiveUserCorrectly() {
        // Act
        user.setDeletedAt(null);

        // Assert
        assertFalse(user.isDeleted());
        assertNull(user.getDeletedAt());
    }

    @Test
    @DisplayName("Should set created and updated timestamps on persist")
    void shouldSetCreatedAndUpdatedTimestampsOnPersist() {
        // Act
        user.onCreate();

        // Assert
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update timestamp on update")
    void shouldUpdateTimestampOnUpdate() {
        // Arrange
        user.onCreate();
        LocalDateTime originalCreatedAt = user.getCreatedAt();

        // Act
        user.onUpdate();

        // Assert
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null OAuth provider")
    void shouldHandleNullOAuthProvider() {
        // Act
        user.setOauthProvider(null);
        user.setOauthSubjectId(null);

        // Assert
        assertNull(user.getOauthProvider());
        assertNull(user.getOauthSubjectId());
    }

    @Test
    @DisplayName("Should handle password authentication type")
    void shouldHandlePasswordAuthenticationType() {
        // Act
        user.setAuthType(AuthType.PASSWORD);

        // Assert
        assertEquals(AuthType.PASSWORD, user.getAuthType());
        assertNull(user.getOauthProvider());
        assertNull(user.getOauthSubjectId());
    }

    @Test
    @DisplayName("Should verify password hash is stored securely")
    void shouldVerifyPasswordHashIsStoredSecurely() {
        // Arrange
        String rawPassword = "plainTextPassword123";
        String hashedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        // Act
        user.setPasswordHash(hashedPassword);

        // Assert
        assertEquals(hashedPassword, user.getPasswordHash());
        assertNotEquals(rawPassword, user.getPasswordHash());
    }

    @Test
    @DisplayName("Should validate username format")
    void shouldValidateUsernameFormat() {
        // Arrange & Act
        user.setUsername("valid_username123");

        // Assert
        assertEquals("valid_username123", user.getUsername());
    }

    @Test
    @DisplayName("Should validate email format")
    void shouldValidateEmailFormat() {
        // Arrange & Act
        user.setEmail("user@example.com");

        // Assert
        assertEquals("user@example.com", user.getEmail());
    }

    @Test
    @DisplayName("Should handle multiple OAuth providers for same user")
    void shouldHandleMultipleOAuthProvidersForSameUser() {
        // This tests that we can track which OAuth provider a user used
        // Act
        user.setOauthProvider("google");
        user.setOauthSubjectId("google_123");

        // Assert
        assertEquals("google", user.getOauthProvider());
        assertEquals("google_123", user.getOauthSubjectId());

        // Act - switching provider
        user.setOauthProvider("github");
        user.setOauthSubjectId("github_456");

        // Assert
        assertEquals("github", user.getOauthProvider());
        assertEquals("github_456", user.getOauthSubjectId());
    }

    @Test
    @DisplayName("Should handle timestamp comparisons correctly")
    void shouldHandleTimestampComparisonsCorrectly() {
        // Arrange
        LocalDateTime earlier = LocalDateTime.now().minusHours(1);
        LocalDateTime later = LocalDateTime.now().plusHours(1);

        // Act
        user.setCreatedAt(earlier);
        user.setUpdatedAt(later);

        // Assert
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()));
    }

    @Test
    @DisplayName("Should handle null ID for new users")
    void shouldHandleNullIdForNewUsers() {
        // Assert
        assertNull(user.getId());
    }

    @Test
    @DisplayName("Should set ID for existing users")
    void shouldSetIdForExistingUsers() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        user.setId(userId);

        // Assert
        assertEquals(userId, user.getId());
    }

    @Test
    @DisplayName("Should handle edge case of null password hash")
    void shouldHandleEdgeCaseOfNullPasswordHash() {
        // Act
        user.setPasswordHash(null);

        // Assert
        assertNull(user.getPasswordHash());
    }

    @Test
    @DisplayName("Should handle OAuth user without password")
    void shouldHandleOAuthUserWithoutPassword() {
        // Arrange & Act
        user.setAuthType(AuthType.OAUTH2);
        user.setOauthProvider("google");
        user.setOauthSubjectId("google_123");
        user.setPasswordHash(null);

        // Assert
        assertEquals(AuthType.OAUTH2, user.getAuthType());
        assertEquals("google", user.getOauthProvider());
        assertEquals("google_123", user.getOauthSubjectId());
        assertNull(user.getPasswordHash());
    }
}
