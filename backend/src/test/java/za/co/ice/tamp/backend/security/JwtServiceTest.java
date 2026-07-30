package za.co.ice.tamp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves a token issued by {@link JwtService} carries the user id and role it was given and can
 * be read back, defending against a signing/parsing mismatch that would make every login
 * unusable, or a token that silently drops the role RBAC depends on.
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "dev-only-signing-key-do-not-use-in-production-0123456789", 60);

    @Test
    void issuesTokenThatParsesBackToSameUserAndRole() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId, "TRANSPORTER");

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractRole(token)).isEqualTo("TRANSPORTER");
    }

    @Test
    void parseTokenReturnsBothClaimsFromOneVerification() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId, "ADMIN");
        JwtService.JwtClaims claims = jwtService.parseToken(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsTokenSignedWithADifferentKey() {
        JwtService otherService = new JwtService(
                "a-completely-different-signing-key-0123456789abcdef", 60);
        String token = otherService.issueToken(UUID.randomUUID(), "ADMIN");

        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.security.SignatureException.class,
                () -> jwtService.extractUserId(token));
    }
}
