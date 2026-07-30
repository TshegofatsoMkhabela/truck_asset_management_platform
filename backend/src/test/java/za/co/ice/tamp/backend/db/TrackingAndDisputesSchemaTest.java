package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the constraints on {@code tracking_events} and {@code disputes}.
 *
 * <p>Neither table appears in issue #6's own list; both are required by brief section 3.2 and
 * section 2.2, and are included so the schema is decided once rather than migrated mid-build.
 */
class TrackingAndDisputesSchemaTest extends MigratedSchemaTestBase {

    private String owner;
    private String matchId;

    @BeforeEach
    void seedAcceptedJob() throws SQLException {
        owner = insertUser("owner@example.com", "FREIGHT_OWNER");
        matchId = insertMatch(
                insertLoad(owner), insertTruck(insertUser("t@example.com", "TRANSPORTER")));
    }

    @Test
    void rejects_tracking_event_for_nonexistent_match() {
        assertThatThrownBy(() -> insertTrackingEvent(GHOST, -26.2041, 28.0473, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tracking_events_match_id_fkey");
    }

    @Test
    void rejects_a_latitude_outside_the_possible_range() {
        assertThatThrownBy(() -> insertTrackingEvent(matchId, 91.0, 28.0473, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tracking_events_latitude_check");
    }

    @Test
    void rejects_a_longitude_outside_the_possible_range() {
        assertThatThrownBy(() -> insertTrackingEvent(matchId, -26.2041, 181.0, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tracking_events_longitude_check");
    }

    @Test
    void rejects_a_tracking_event_carrying_neither_coordinates_nor_status() {
        assertThatThrownBy(() -> insertTrackingEvent(matchId, null, null, null))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tracking_events_position_or_status_check");
    }

    @Test
    void accepts_a_status_only_tracking_event() throws SQLException {
        // Brief section 3.2 allows "latitude, longitude or route status", so a status
        // progression with no coordinates is a valid way to satisfy this requirement.
        insertTrackingEvent(matchId, null, null, "IN_TRANSIT");

        assertThat(single("SELECT count(*)::text FROM tracking_events")).isEqualTo("1");
    }

    @Test
    void rejects_dispute_for_nonexistent_match() {
        assertThatThrownBy(() -> insertDispute(GHOST))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("disputes_match_id_fkey");
    }

    @Test
    void defaults_a_new_dispute_to_open() throws SQLException {
        insertDispute(matchId);

        assertThat(single("SELECT status FROM disputes")).isEqualTo("OPEN");
    }

    @Test
    void accepts_a_flag_raised_against_a_user_with_no_match_involved() throws SQLException {
        // "Flagged/disputed items" covers more than matches: an admin may flag a user for
        // conduct with no match involved. Without this, the only way to express that
        // would be compliance_status = 'REJECTED', which means something different.
        String reported = insertUser("reported@example.com", "TRANSPORTER");

        insertUserFlag(reported);

        assertThat(single("SELECT count(*)::text FROM disputes WHERE match_id IS NULL"))
                .isEqualTo("1");
    }

    @Test
    void rejects_a_dispute_about_neither_a_match_nor_a_user() {
        assertThatThrownBy(() -> execute(
                        "INSERT INTO disputes (raised_by, description) VALUES ('" + owner
                                + "', 'Something went wrong somewhere')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("disputes_subject_check");
    }

    @Test
    void rejects_a_resolved_dispute_with_no_resolution_timestamp() throws SQLException {
        insertDispute(matchId);

        assertThatThrownBy(() -> execute("UPDATE disputes SET status = 'RESOLVED'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("disputes_resolution_consistency_check");
    }

    private void insertTrackingEvent(String match, Double lat, Double lon, String status)
            throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO tracking_events (match_id, latitude, longitude, status) "
                        + "VALUES (?::uuid, ?, ?, ?)")) {
            insert.setString(1, match);
            setNullableDouble(insert, 2, lat);
            setNullableDouble(insert, 3, lon);
            insert.setString(4, status);
            insert.execute();
        }
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NUMERIC);
        } else {
            statement.setDouble(index, value);
        }
    }

    private void insertUserFlag(String subjectUserId) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO disputes (subject_user_id, raised_by, description) "
                        + "VALUES (?::uuid, ?::uuid, 'Repeatedly cancels accepted jobs')")) {
            insert.setString(1, subjectUserId);
            insert.setString(2, owner);
            insert.execute();
        }
    }

    private void insertDispute(String match) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO disputes (match_id, raised_by, description) "
                        + "VALUES (?::uuid, ?::uuid, 'Cargo arrived damaged')")) {
            insert.setString(1, match);
            insert.setString(2, owner);
            insert.execute();
        }
    }
}
