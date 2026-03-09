package com.vibe.auth.service;

import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.model.AuthType;
import com.vibe.auth.model.User;
import com.vibe.auth.repository.UserRepository;
import com.vibe.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service handling login operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticate user with username and password.
     *
     * @param request Login request containing credentials
     * @return JWT access token
     * @throws IllegalArgumentException if credentials are invalid
     */
    public String authenticate(LoginRequest request) {
        User user = authenticateAndGetUser(request);
        return generateAccessToken(user);
    }

    /**
     * Authenticate user with username and password, returning the User entity.
     *
     * @param request Login request containing credentials
     * @return Authenticated User entity
     * @throws IllegalArgumentException if credentials are invalid
     */
    public User authenticateAndGetUser(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (user.getAuthType() != AuthType.PASSWORD || user.getPasswordHash() == null) {
            throw new IllegalArgumentException("This account uses OAuth2 authentication");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    /**
     * Generate JWT access token for a user.
     *
     * @param user User entity
     * @return JWT access token
     */
    public String generateAccessToken(User user) {
        return jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthType().name().toLowerCase()
        );
    }
}
