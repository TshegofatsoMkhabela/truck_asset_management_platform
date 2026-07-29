package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Proves {@code updated_at} tracks the last modification rather than the creation time.
 *
 * <p>Without this, the column is worse than absent: it looks authoritative while reporting
 * something that has not been true since the row was inserted.
 */
class UpdatedAtSchemaTest extends MigratedSchemaTestBase {

    @Test
    void advances_updated_at_when_a_user_is_modified() throws SQLException {
        insertUser("edited@example.com", "TRANSPORTER");

        execute("UPDATE users SET full_name = 'Renamed' WHERE email = 'edited@example.com'");

        assertThat(single("SELECT (updated_at > created_at)::text FROM users")).isEqualTo("true");
    }

    @Test
    void leaves_updated_at_equal_to_created_at_on_insert() throws SQLException {
        insertUser("fresh@example.com", "TRANSPORTER");

        assertThat(single("SELECT (updated_at = created_at)::text FROM users")).isEqualTo("true");
    }

    @Test
    void advances_updated_at_when_a_load_or_truck_is_modified() throws SQLException {
        insertLoad(insertUser("owner@example.com", "FREIGHT_OWNER"));
        insertTruck(insertUser("transporter@example.com", "TRANSPORTER"));

        execute("UPDATE loads SET status = 'MATCHED'");
        execute("UPDATE trucks SET status = 'MATCHED'");

        assertThat(single("SELECT (updated_at > created_at)::text FROM loads")).isEqualTo("true");
        assertThat(single("SELECT (updated_at > created_at)::text FROM trucks")).isEqualTo("true");
    }
}
