package za.co.ice.tamp.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the JWT (JSON Web Token, a signed string proving who the caller is)
 * returned on login. The signing key comes from configuration, never hardcoded, so #8's
 * environment-variable-driven deploy can set a real key without touching code.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(UUID userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Verifies the signature and reads both claims in a single pass, for callers (like
     * {@link JwtAuthenticationFilter}) that need both: calling {@link #extractUserId} and
     * {@link #extractRole} separately would parse and signature-verify the same token twice per
     * request for no benefit.
     */
    public JwtClaims parseToken(String token) {
        Claims claims = parseClaims(token);
        return new JwtClaims(UUID.fromString(claims.getSubject()), claims.get("role", String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record JwtClaims(UUID userId, String role) {
    }
}
