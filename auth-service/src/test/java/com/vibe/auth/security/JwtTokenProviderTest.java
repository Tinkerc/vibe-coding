package com.vibe.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 */
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.setKeyPath(tempDir.toString());
        jwtTokenProvider.setAccessTokenExpiry(1800); // 30 minutes
        jwtTokenProvider.setIssuer("auth-service");
        jwtTokenProvider.setAudience("api-gateway");
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate RSA key pair on initialization")
    void shouldGenerateRsaKeyPairOnInitialization() {
        // Assert
        assertNotNull(jwtTokenProvider.getPrivateKey());
        assertNotNull(jwtTokenProvider.getPublicKey());
        assertNotNull(jwtTokenProvider.getKeyId());
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        String email = "test@example.com";

        // Act
        String token = jwtTokenProvider.generateToken(userId, username, email, "password");

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Should validate valid JWT token")
    void shouldValidateValidJwtToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, "testuser", "test@example.com", "password");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid JWT token")
    void shouldRejectInvalidJwtToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.here");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract user ID from valid token")
    void shouldExtractUserIdFromValidToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, "testuser", "test@example.com", "password");

        // Act
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // Arrange
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(UUID.randomUUID(), username, "test@example.com", "password");

        // Act
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    @DisplayName("Should extract JTI from valid token")
    void shouldExtractJtiFromValidToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(UUID.randomUUID(), "testuser", "test@example.com", "password");

        // Act
        String jti = jwtTokenProvider.getJtiFromToken(token);

        // Assert
        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void shouldGetExpirationDateFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(UUID.randomUUID(), "testuser", "test@example.com", "password");

        // Act
        Instant expiration = jwtTokenProvider.getExpirationFromToken(token);

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Arrange
        jwtTokenProvider.setAccessTokenExpiry(-1); // Negative value for testing
        jwtTokenProvider.init();
        String token = jwtTokenProvider.generateToken(UUID.randomUUID(), "testuser", "test@example.com", "password");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should persist keys across restarts")
    void shouldPersistKeysAcrossRestarts() {
        // Arrange - Generate keys with first provider
        String keyId = jwtTokenProvider.getKeyId();

        // Act - Create new provider with same path
        JwtTokenProvider newProvider = new JwtTokenProvider();
        newProvider.setKeyPath(tempDir.toString());
        newProvider.setAccessTokenExpiry(1800);
        newProvider.setIssuer("auth-service");
        newProvider.setAudience("api-gateway");
        newProvider.init();

        // Assert
        assertEquals(keyId, newProvider.getKeyId());
    }

    @Test
    @DisplayName("Should get key ID")
    void shouldGetKeyId() {
        // Act
        String keyId = jwtTokenProvider.getKeyId();

        // Assert
        assertNotNull(keyId);
        assertFalse(keyId.isBlank());
        assertTrue(keyId.startsWith("key-"));
    }

    @Test
    @DisplayName("Should handle OAuth2 authentication type")
    void shouldHandleOAuth2AuthenticationType() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        String token = jwtTokenProvider.generateToken(userId, "oauthuser", "oauth@example.com", "oauth2");

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getUserIdFromToken("not-a-jwt");
        });
    }
}
