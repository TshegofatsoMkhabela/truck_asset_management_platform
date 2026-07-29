package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Proves the constraints on {@code trucks} and {@code loads}, the two sides matching compares.
 *
 * <p>Contains the issue's Minimum Integration Test: a user, a load and a truck persisted with
 * their foreign-key relationships intact.
 */
class AssetsSchemaTest extends MigratedSchemaTestBase {

    @Test
    void creates_user_load_and_truck_with_intact_foreign_keys() throws SQLException {
        String freightOwner = insertUser("owner@example.com", "FREIGHT_OWNER");
        String transporter = insertUser("transporter@example.com", "TRANSPORTER");

        String loadId = insertLoad(freightOwner);
        String truckId = insertTruck(transporter);

        assertThat(loadId).isNotBlank();
        assertThat(truckId).isNotBlank();
        assertThat(single("SELECT owner_id::text FROM loads WHERE id = '" + loadId + "'"))
                .isEqualTo(freightOwner);
        assertThat(single("SELECT transporter_id::text FROM trucks WHERE id = '" + truckId + "'"))
                .isEqualTo(transporter);
    }

    @Test
    void rejects_load_with_nonexistent_owner() {
        assertThatThrownBy(() -> insertLoad(GHOST))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("loads_owner_id_fkey");
    }

    @Test
    void rejects_truck_with_nonexistent_transporter() {
        assertThatThrownBy(() -> insertTruck(GHOST))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("trucks_transporter_id_fkey");
    }

    @Test
    void rejects_nonpositive_truck_capacity() throws SQLException {
        String transporter = insertUser("t@example.com", "TRANSPORTER");

        assertThatThrownBy(() -> insertTruck(transporter, -1_000))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("trucks_capacity_kg_check");
    }

    @Test
    void rejects_nonpositive_load_weight() throws SQLException {
        String owner = insertUser("o@example.com", "FREIGHT_OWNER");

        assertThatThrownBy(() -> insertLoad(owner, 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("loads_weight_kg_check");
    }

    @Test
    void rejects_truck_availability_window_that_ends_before_it_starts() throws SQLException {
        String transporter = insertUser("backwards@example.com", "TRANSPORTER");

        assertThatThrownBy(() -> insertTruck(transporter, 20_000, FRIDAY, MONDAY))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("trucks_availability_window_check");
    }

    @Test
    void rejects_load_pickup_window_that_ends_before_it_starts() throws SQLException {
        String owner = insertUser("backwards2@example.com", "FREIGHT_OWNER");

        assertThatThrownBy(() -> insertLoad(owner, 5_000, FRIDAY, MONDAY))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("loads_pickup_window_check");
    }

    @Test
    void rejects_unknown_vehicle_type() throws SQLException {
        String transporter = insertUser("spaceship@example.com", "TRANSPORTER");

        assertThatThrownBy(() -> insertTruck(transporter, 20_000, MONDAY, FRIDAY, "HOVERCRAFT"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("trucks_vehicle_type_check");
    }

    @Test
    void rejects_nonpositive_truck_volume_capacity() throws SQLException {
        String transporter = insertUser("volume@example.com", "TRANSPORTER");

        assertThatThrownBy(() -> execute(
                        "INSERT INTO trucks (transporter_id, vehicle_type, capacity_kg, capacity_m3,"
                                + " current_city, available_from, available_until) VALUES ('"
                                + transporter + "', 'CONTAINER', 30000, 0, 'Johannesburg', now(),"
                                + " now() + interval '2 days')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("trucks_capacity_m3_check");
    }

    @Test
    void allows_a_truck_with_no_volume_capacity_recorded() throws SQLException {
        // A flatbed has no meaningful enclosed volume, so null means "no volume constraint"
        // rather than "unknown". #13 treats it as always compatible on the volume rule.
        String transporter = insertUser("flatbed@example.com", "TRANSPORTER");

        insertTruck(transporter);

        assertThat(single("SELECT count(*)::text FROM trucks WHERE capacity_m3 IS NULL"))
                .isEqualTo("1");
    }

    @Test
    void defaults_a_new_load_to_open_and_a_new_truck_to_available() throws SQLException {
        String owner = insertUser("status@example.com", "FREIGHT_OWNER");
        String transporter = insertUser("status2@example.com", "TRANSPORTER");

        String loadId = insertLoad(owner);
        String truckId = insertTruck(transporter);

        assertThat(single("SELECT status FROM loads WHERE id = '" + loadId + "'")).isEqualTo("OPEN");
        assertThat(single("SELECT status FROM trucks WHERE id = '" + truckId + "'"))
                .isEqualTo("AVAILABLE");
    }
}
