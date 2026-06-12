package com.kabooz.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;

/**
 * Provides JWT creation, validation, and claim extraction.
 * Uses HMAC-SHA256 (HS256) with a 256-bit hex secret configured in application.properties.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Build the signing key from the hex-encoded secret.
     *
     * @return HMAC-SHA key
     */
    private SecretKey signingKey() {
        // HexFormat is available natively in Java 17+
        // Decoders.HEX does NOT exist in JJWT 0.11.5 — only BASE64 / BASE64URL do
        byte[] keyBytes = HexFormat.of().parseHex(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a signed JWT token for the authenticated admin user.
     *
     * @param authentication the Spring Security authentication object
     * @return compact JWT string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", "ROLE_ADMIN")
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a JWT directly from a username string (used after account validation).
     *
     * @param username admin username
     * @return compact JWT string
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", "ROLE_ADMIN")
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the username (subject) from a JWT token.
     *
     * @param token compact JWT string
     * @return username stored in subject claim
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validate a JWT token — checks signature and expiry.
     *
     * @param token compact JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Return the configured expiry in milliseconds.
     *
     * @return expiry in ms
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
