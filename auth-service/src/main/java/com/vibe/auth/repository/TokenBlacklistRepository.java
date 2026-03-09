package com.vibe.auth.repository;

import com.vibe.auth.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TokenBlacklist entity.
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /**
     * Find blacklist entry by JWT ID.
     */
    Optional<TokenBlacklist> findByJti(String jti);

    /**
     * Check if a token is blacklisted.
     */
    boolean existsByJtiAndExpiresAtAfter(String jti, LocalDateTime now);

    /**
     * Delete expired entries.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :expiryDate")
    void deleteExpiredEntries(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Blacklist all tokens for a user (for account deletion).
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO token_blacklist (jti, user_id, expires_at, created_at) " +
                   "SELECT :jtiPrefix || rt.id::text, :userId, rt.expires_at, CURRENT_TIMESTAMP " +
                   "FROM refresh_tokens rt WHERE rt.user_id = :userId AND rt.revoked = false",
           nativeQuery = true)
    void blacklistAllUserTokens(@Param("userId") UUID userId, @Param("jtiPrefix") String jtiPrefix);
}
