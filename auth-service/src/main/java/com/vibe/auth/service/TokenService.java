package com.vibe.auth.service;

import com.vibe.auth.dto.request.RefreshTokenRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.RefreshToken;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.RefreshTokenRepository;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Token service handling refresh token operations.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 1800; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    /**
     * Refresh access token using refresh token.
     * Implements token rotation for security.
     *
     * @param request Refresh token request
     * @return New auth response with rotated refresh token
     * @throws IllegalArgumentException if refresh token is invalid
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Handle idempotency for concurrent requests
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            AuthResponse cached = getCachedResponse(request.idempotencyKey());
            if (cached != null) {
                return cached;
            }
        }

        String tokenHash = hashToken(request.refreshToken());
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!existingToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Revoke old token
        existingToken.setRevoked(true);
        existingToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(existingToken);

        // Generate new tokens
        String accessToken = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthType().name().toLowerCase()
        );

        String newRefreshToken = generateRefreshToken();
        RefreshToken newTokenEntity = createRefreshTokenEntity(
                user.getId(),
                newRefreshToken,
                existingToken.getDeviceId()
        );
        refreshTokenRepository.save(newTokenEntity);

        AuthResponse response = new AuthResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                ACCESS_TOKEN_EXPIRY_SECONDS
        );

        // Cache for idempotency (5 minutes)
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            cacheResponse(request.idempotencyKey(), response);
        }

        return response;
    }

    /**
     * Create refresh token entity.
     */
    private RefreshToken createRefreshTokenEntity(UUID userId, String token, String deviceId) {
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUserId(userId);
        tokenEntity.setTokenHash(hashToken(token));
        tokenEntity.setDeviceId(deviceId);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));
        return tokenEntity;
    }

    /**
     * Generate random refresh token.
     */
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Hash refresh token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Get cached response for idempotency key.
     */
    private AuthResponse getCachedResponse(String idempotencyKey) {
        Cache cache = cacheManager.getCache("idempotency");
        if (cache != null) {
            return cache.get(idempotencyKey, AuthResponse.class);
        }
        return null;
    }

    /**
     * Cache response for idempotency key.
     */
    private void cacheResponse(String idempotencyKey, AuthResponse response) {
        Cache cache = cacheManager.getCache("idempotency");
        if (cache != null) {
            cache.put(idempotencyKey, response);
        }
    }

    /**
     * Create and persist a new refresh token for a user.
     *
     * @param userId User ID
     * @return Refresh token value
     */
    @Transactional
    public String createRefreshToken(UUID userId) {
        String token = generateRefreshToken();
        RefreshToken tokenEntity = createRefreshTokenEntity(userId, token, null);
        refreshTokenRepository.save(tokenEntity);
        return token;
    }

    /**
     * Revoke a refresh token by hashing and looking it up.
     *
     * @param token Refresh token to revoke
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);

        if (existingToken != null) {
            existingToken.setRevoked(true);
            existingToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(existingToken);
        }
    }
}
