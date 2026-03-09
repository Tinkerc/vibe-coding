package com.vibe.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider using RS256 signing algorithm.
 * Generates and validates JWT tokens for authentication.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.key-path:/opt/auth-service/keys}")
    private String keyPath;

    @Value("${jwt.access-token-expiry:1800}")
    private long accessTokenExpiry;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    @Value("${jwt.audience:api-gateway}")
    private String audience;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String keyId;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            generateKeysIfNotExists();
            loadKeys();
            this.keyId = readKeyId();
            this.jwtParser = Jwts.parser()
                    .verifyWith(publicKey)
                    .build();
            logger.info("JWT token provider initialized with key ID: {}", keyId);
        } catch (Exception e) {
            logger.error("Failed to initialize JWT token provider", e);
            throw new RuntimeException("Failed to initialize JWT token provider", e);
        }
    }

    /**
     * Generate RSA key pair if they don't exist.
     */
    private void generateKeysIfNotExists() throws Exception {
        Path keyDir = Paths.get(keyPath);
        Path privateKeyPath = keyDir.resolve("private.key");
        Path publicKeyPath = keyDir.resolve("public.key");
        Path keyVersionPath = keyDir.resolve("key-version.txt");

        if (!Files.exists(privateKeyPath)) {
            Files.createDirectories(keyDir);

            // Generate RSA key pair
            KeyPair keyPair = generateRsaKeyPair();

            // Save private key
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateSpec);
            Files.write(privateKeyPath, privateKey.getEncoded());
            privateKeyPath.toFile().setReadable(true, true);
            privateKeyPath.toFile().setWritable(false, false);

            // Save public key
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
            PublicKey publicKey = keyFactory.generatePublic(publicSpec);
            Files.write(publicKeyPath, publicKey.getEncoded());
            publicKeyPath.toFile().setReadable(true, true);
            publicKeyPath.toFile().setWritable(false, false);

            // Save key version
            String newKeyId = "key-" + java.time.LocalDate.now() + "-v1";
            Files.writeString(keyVersionPath, newKeyId);

            logger.info("Generated new RSA key pair with ID: {}", newKeyId);
        }
    }

    private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private void loadKeys() throws Exception {
        Path keyDir = Paths.get(keyPath);
        Path privateKeyPath = keyDir.resolve("private.key");
        Path publicKeyPath = keyDir.resolve("public.key");

        // Load private key
        byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateSpec);

        // Load public key
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = keyFactory.generatePublic(publicSpec);
    }

    private String readKeyId() throws IOException {
        Path keyVersionPath = Paths.get(keyPath, "key-version.txt");
        if (Files.exists(keyVersionPath)) {
            return Files.readString(keyVersionPath).trim();
        }
        return "key-default";
    }

    /**
     * Generate a JWT token for the given user.
     */
    public String generateToken(UUID userId, String username, String email, String authType) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpiry, ChronoUnit.SECONDS);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(userId.toString())
                .claim("username", username)
                .claim("email", email)
                .claim("auth_type", authType)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey)
                .header().add("kid", keyId).and()
                .compact();
    }

    /**
     * Validate a JWT token.
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("JWT token signature is invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract user ID from token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract username from token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.get("username", String.class);
    }

    /**
     * Extract email from token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.get("email", String.class);
    }

    /**
     * Extract JTI (JWT ID) from token.
     */
    public String getJtiFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getId();
    }

    /**
     * Extract expiration date from token.
     */
    public Instant getExpirationFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getExpiration().toInstant();
    }

    /**
     * Get key ID.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Get modulus for JWKS endpoint.
     */
    public String getModulus() {
        java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray());
    }

    /**
     * Get exponent for JWKS endpoint.
     */
    public String getExponent() {
        java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getExponent().toByteArray());
    }

    @PreDestroy
    public void cleanup() {
        logger.info("JWT token provider destroyed");
    }

    // Getters for testing
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    protected PublicKey getPublicKey() {
        return publicKey;
    }

    protected void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    protected void setAccessTokenExpiry(long accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    protected void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    protected void setAudience(String audience) {
        this.audience = audience;
    }
}
