package com.vibe.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * JWKS endpoint for public key distribution.
 * Allows API Gateway and other services to validate JWT tokens.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * JWKS endpoint returning public key in JWKS format.
     * This endpoint is public and does not require authentication.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of(
                "keys", List.of(
                        Map.of(
                                "kty", "RSA",
                                "kid", jwtTokenProvider.getKeyId(),
                                "n", jwtTokenProvider.getModulus(),
                                "e", jwtTokenProvider.getExponent(),
                                "alg", "RS256",
                                "use", "sig"
                        )
                )
        );
    }
}
