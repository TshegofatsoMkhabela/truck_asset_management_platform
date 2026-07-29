package za.co.ice.tamp.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import brokenentity.BrokenEntityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Proves {@code ddl-auto: validate} actually rejects a mismatched entity, rather than starting
 * anyway and failing later at query time. A passing "validate" setting that never rejects
 * anything would be indistinguishable from no validation at all, the same vacuous-test trap
 * that a coin-flip UUID assertion fell into during #6.
 */
class SchemaValidationTest extends JpaTestBase {

    @Test
    void refuses_to_start_when_an_entity_does_not_match_the_schema() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.datasource.url=" + jdbcUrl(),
                        "spring.datasource.username=" + jdbcUsername(),
                        "spring.datasource.password=" + jdbcPassword(),
                        "spring.jpa.hibernate.ddl-auto=validate")
                .withUserConfiguration(BrokenEntityConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
