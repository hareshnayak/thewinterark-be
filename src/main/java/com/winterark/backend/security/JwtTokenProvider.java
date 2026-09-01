package com.winterark.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:2592000000}") // Default 30 days in milliseconds (2592000000)
    private long jwtExpirationMs;

    private SecretKey key() {
        byte[] keyBytes;
        try {
            // First attempt Base64 decode
            keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) {
                keyBytes = MessageDigest.getInstance("SHA-256")
                        .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256")
                        .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        // Ensure at least 30 days validity if property is missing or too small
        long validityMs = (jwtExpirationMs > 0) ? jwtExpirationMs : (1000L * 60 * 60 * 24 * 30);
        Date expiryDate = new Date(now.getTime() + validityMs);

        log.info("Generating JWT token for username: {}, issuedAt: {}, expiresAt: {}", username, now, expiryDate);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parser()
                .clockSkewSeconds(300) // 5 minutes clock skew tolerance
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .clockSkewSeconds(300) // 5 minutes clock skew tolerance
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token has expired! Claims: issuedAt={}, expiration={}, serverNow={}",
                    ex.getClaims().getIssuedAt(), ex.getClaims().getExpiration(), new Date());
            return false;
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }
}
