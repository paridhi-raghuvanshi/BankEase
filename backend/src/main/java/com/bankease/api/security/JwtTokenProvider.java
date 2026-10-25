package com.bankease.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility component for JWT (JSON Web Token) operations in the BankEase application.
 *
 * <p>Handles generation, validation, and parsing of both access and refresh tokens
 * using the jjwt 0.12.x API. The signing key is derived from the configured secret
 * using HMAC-SHA algorithm.</p>
 *
 * <p>Configuration properties (application.yml / application.properties):</p>
 * <ul>
 *   <li>{@code app.jwt.secret} – Base secret for HMAC signing (min. 256 bits recommended)</li>
 *   <li>{@code app.jwt.expiration} – Access token TTL in milliseconds</li>
 *   <li>{@code app.jwt.refresh-expiration} – Refresh token TTL in milliseconds</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLES  = "roles";
    private static final String CLAIM_TYPE   = "tokenType";
    private static final String TYPE_ACCESS  = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    /** HMAC-SHA secret key derived from configured secret string. */
    private final SecretKey signingKey;

    /** Access token time-to-live in milliseconds. */
    private final long accessTokenExpirationMs;

    /** Refresh token time-to-live in milliseconds. */
    private final long refreshTokenExpirationMs;

    /**
     * Constructs the provider and derives the HMAC signing key from the configured secret.
     *
     * @param secret                  raw secret string (injected from {@code app.jwt.secret})
     * @param accessTokenExpirationMs access token TTL in ms (injected from {@code app.jwt.expiration})
     * @param refreshTokenExpirationMs refresh token TTL in ms (injected from {@code app.jwt.refresh-expiration})
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-expiration}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // -------------------------------------------------------------------------
    // Token Generation
    // -------------------------------------------------------------------------

    /**
     * Generates a short-lived JWT access token for the given user.
     *
     * <p>The token carries the user's email as the subject and their authorities
     * as a {@code roles} claim.</p>
     *
     * @param userDetails the authenticated user's details
     * @return a compact, signed JWT access token string
     */
    public String generateAccessToken(UserDetails userDetails) {
        log.debug("Generating access token for user: {}", userDetails.getUsername());
        Map<String, Object> claims = buildClaims(userDetails, TYPE_ACCESS);
        return buildToken(userDetails.getUsername(), claims, accessTokenExpirationMs);
    }

    /**
     * Generates a long-lived JWT refresh token for the given user.
     *
     * <p>The refresh token carries only the user email and token-type claim.
     * It should be stored securely and used exclusively to obtain new access tokens.</p>
     *
     * @param userDetails the authenticated user's details
     * @return a compact, signed JWT refresh token string
     */
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("Generating refresh token for user: {}", userDetails.getUsername());
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return buildToken(userDetails.getUsername(), claims, refreshTokenExpirationMs);
    }

    // -------------------------------------------------------------------------
    // Token Parsing
    // -------------------------------------------------------------------------

    /**
     * Extracts the email address (subject) from the given JWT token.
     *
     * @param token a compact JWT string
     * @return the email stored as the subject claim
     * @throws JwtException if the token is malformed or signature verification fails
     */
    public String getEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token a compact JWT string
     * @return the {@link Date} at which the token expires
     * @throws JwtException if the token is malformed or signature verification fails
     */
    public Date getExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validates the token by verifying its signature, checking expiry, and ensuring
     * the subject matches the given {@link UserDetails}.
     *
     * @param token       the JWT token string to validate
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT token compact representation is empty: {}", ex.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Internal Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds role-based claims map for an access token.
     */
    private Map<String, Object> buildClaims(UserDetails userDetails, String tokenType) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_TYPE, tokenType);
        return claims;
    }

    /**
     * Constructs and signs a JWT with the provided subject, claims, and TTL.
     */
    private String buildToken(String subject, Map<String, Object> extraClaims, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts a single claim from the token using the provided resolver function.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the token's signature, returning all claims.
     *
     * @throws JwtException on signature mismatch or malformed token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns {@code true} if the token's expiration date is in the past.
     */
    private boolean isTokenExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }
}
