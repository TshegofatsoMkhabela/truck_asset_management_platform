package za.co.ice.tamp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Proves the {@link PasswordEncoder} bean actually hashes rather than passing a value
 * through unchanged, defending against a config mistake (e.g. a {@code NoOpPasswordEncoder})
 * that would leave {@code users.password_hash} storing plain text.
 */
class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void encodedValueIsNeverEqualToRawInput() {
        String raw = "correct-horse-battery-staple";

        String encoded = encoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void sameInputProducesDifferentHashesEachTime() {
        String raw = "correct-horse-battery-staple";

        String first = encoder.encode(raw);
        String second = encoder.encode(raw);

        assertThat(first).isNotEqualTo(second);
    }
}
