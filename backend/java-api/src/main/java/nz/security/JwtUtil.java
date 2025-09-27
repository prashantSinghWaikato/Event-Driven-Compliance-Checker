// src/main/java/nz/compliscan/api/security/JwtUtil.java
package nz.compliscan.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // <-- use SecretKey
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key; // <-- SecretKey, not Key
    private final long ttlMillis;

    public JwtUtil(
            @Value("${app.security.jwtSecret}") String secret,
            @Value("${app.security.jwtTtlMinutes}") long ttlMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be >= 32 bytes (256-bit) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes); // returns SecretKey
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    public String issue(String username, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key) // HS256 with SecretKey
                .compact();
    }

    public String verifyAndGetSubject(String token) {
        return Jwts.parser()
                .verifyWith(key) // expects SecretKey
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
