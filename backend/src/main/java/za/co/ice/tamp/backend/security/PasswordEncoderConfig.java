package za.co.ice.tamp.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the {@link PasswordEncoder} used to hash {@code users.password_hash}.
 *
 * <p>BCrypt is an adaptive hashing algorithm: it embeds a per-password random salt and a
 * configurable work factor, so two hashes of the same input never match each other, only the
 * original input via {@link PasswordEncoder#matches}.
 *
 * <p>Deliberately a standalone bean rather than part of a {@code SecurityFilterChain}: #9
 * (RBAC and auth) owns the authentication surface. This bean exists so #10's user creation
 * never stores a plaintext password while waiting for that surface to land, and so #9 can
 * reuse it directly instead of introducing a second encoder.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
