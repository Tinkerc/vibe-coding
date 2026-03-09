package com.vibe.auth.controller;

import com.vibe.auth.dto.request.LoginRequest;
import com.vibe.auth.dto.request.RefreshTokenRequest;
import com.vibe.auth.dto.response.AuthResponse;
import com.vibe.auth.model.User;
import com.vibe.auth.service.AuthService;
import com.vibe.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", description = "Returns JWT access and refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticateAndGetUser(request);
        String accessToken = authService.generateAccessToken(user);

        // Generate and persist refresh token
        String refreshToken = tokenService.createRefreshToken(user.getId());

        AuthResponse response = new AuthResponse(accessToken, refreshToken, "Bearer", 1800);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token for security")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = tokenService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the refresh token")
    public ResponseEntity<Void> logout(@RequestHeader("Refresh-Token") String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
