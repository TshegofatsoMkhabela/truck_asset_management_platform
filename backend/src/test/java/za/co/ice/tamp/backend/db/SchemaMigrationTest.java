package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Proves the migrations in {@code db/migrations} apply to a blank PostgreSQL 18 server.
 *
 * <p>Extends the un-migrated base on purpose: this is the one test that must start from an
 * empty database and apply the migrations itself.
 *
 * <p>PostgreSQL 18 is pinned because the schema generates primary keys with the built-in
 * {@code uuidv7()} function, which earlier servers do not provide.
 */
class SchemaMigrationTest extends PostgresTestBase {

    @Test
    void schema_applies_cleanly_to_an_empty_database() throws SQLException {
        SchemaFixture.applyAll(db);

        assertThat(tableNames())
                .contains("users", "trucks", "loads", "matches", "receipts", "ratings",
                        "audit_logs", "tracking_events", "disputes");
    }
}
