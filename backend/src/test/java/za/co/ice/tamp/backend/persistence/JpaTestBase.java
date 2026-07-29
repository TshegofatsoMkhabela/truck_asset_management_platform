package za.co.ice.tamp.backend.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.postgresql.PostgreSQLContainer;
import za.co.ice.tamp.backend.db.SchemaFixture;

/**
 * Boots the real Spring application context against a migrated PostgreSQL 18 container.
 *
 * <p>The migrations are applied once, in a static initializer, before Spring ever tries to
 * connect: {@code ddl-auto: validate} only checks a schema that already exists, so the context
 * would fail to start if the tables were not there first.
 *
 * <p>Public, and reused outside this package: once the application had a real datasource
 * (this issue), every {@code @SpringBootTest} needs one to boot at all, including #1/#2's
 * {@code HelloControllerTest} and {@code HealthControllerTest}, which touch no persistence
 * code themselves. Pointing them at H2 instead would introduce a second database with its own
 * fidelity gap for tests that do not even need real constraint behaviour; reusing the same
 * migrated container is one fewer thing to keep faithful to production.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public abstract class JpaTestBase {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("tamp")
                    .withUsername("tamp")
                    .withPassword("tamp");

    static {
        POSTGRES.start();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            SchemaFixture.applyAll(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not apply migrations before context start", e);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** Exposed for tests that need to point a second, throwaway context at the same database. */
    protected static String jdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    protected static String jdbcUsername() {
        return POSTGRES.getUsername();
    }

    protected static String jdbcPassword() {
        return POSTGRES.getPassword();
    }
}
