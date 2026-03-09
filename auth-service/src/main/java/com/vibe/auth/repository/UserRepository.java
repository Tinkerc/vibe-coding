package com.vibe.auth.repository;

import com.vibe.auth.model.AuthType;
import com.vibe.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by OAuth provider and subject ID.
     */
    @Query("SELECT u FROM User u WHERE u.oauthProvider = :provider AND u.oauthSubjectId = :subjectId")
    Optional<User> findByProviderAndSubjectId(@Param("provider") String provider, @Param("subjectId") String subjectId);

    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Soft delete user by ID.
     */
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDeleteById(@Param("id") UUID id);
}
