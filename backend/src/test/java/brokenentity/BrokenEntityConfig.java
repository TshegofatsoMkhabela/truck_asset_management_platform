package brokenentity;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Boots only {@link BrokenEntity}, deliberately excluding the real application.
 *
 * <p>Must live as a top-level class here, not as a static nested class inside the test that
 * uses it: Spring Boot Test auto-detects a nested {@code @Configuration} class as the test
 * class's own {@code @SpringBootTest} configuration. Nesting this inside
 * {@code SchemaValidationTest} caused that test's own inherited outer context (from
 * {@code JpaTestBase}) to boot with this broken configuration instead of the real
 * application, failing before the test method's assertion ever ran.
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = BrokenEntity.class)
public class BrokenEntityConfig {
}
