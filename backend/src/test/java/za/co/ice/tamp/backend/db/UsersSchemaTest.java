package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Proves the constraints on {@code users} are enforced by PostgreSQL, not merely intended. */
class UsersSchemaTest extends MigratedSchemaTestBase {

    @Test
    void assigns_a_time_ordered_uuid_to_every_new_user() throws SQLException {
        // UUIDv7 puts a timestamp in the leading bits, so later rows sort after earlier ones.
        // Ten rows rather than two: two random UUIDv4 values land in the right order half the
        // time, so a two-row assertion would pass against the wrong ID type by coin flip.
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(insertUser("user" + i + "@example.com", "TRANSPORTER"));
        }

        assertThat(ids).doesNotContainNull().isSorted();
    }

    @Test
    void rejects_duplicate_email_regardless_of_case() throws SQLException {
        insertUser("Sam@Example.com", "FREIGHT_OWNER");

        assertThatThrownBy(() -> insertUser("sam@example.com", "TRANSPORTER"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("users_email_key");
    }

    @Test
    void rejects_unknown_role_value() {
        assertThatThrownBy(() -> insertUser("new@example.com", "SUPER_ADMIN"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("users_role_check");
    }

    @Test
    void rejects_unknown_compliance_status() {
        assertThatThrownBy(() -> execute(
                        "INSERT INTO users (full_name, email, password_hash, role, compliance_status) "
                                + "VALUES ('Nia', 'nia@example.com', 'hash', 'ADMIN', 'VERIFIED')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("users_compliance_status_check");
    }

    @Test
    void defaults_new_users_to_pending_compliance() throws SQLException {
        insertUser("pending@example.com", "TRANSPORTER");

        assertThat(single("SELECT compliance_status FROM users WHERE email = 'pending@example.com'"))
                .isEqualTo("PENDING");
    }

    @Test
    void rejects_blank_full_name() {
        assertThatThrownBy(() -> execute(
                        "INSERT INTO users (full_name, email, password_hash, role) "
                                + "VALUES ('   ', 'blank@example.com', 'hash', 'ADMIN')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("users_full_name_check");
    }
}
