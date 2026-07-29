package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the constraints on {@code ratings} (FR-09) and {@code audit_logs} (FR-12).
 *
 * <p>The audit tests are the inverse of every other test in this suite: they prove rows
 * survive rather than that bad rows are refused.
 */
class RatingsAndAuditSchemaTest extends MigratedSchemaTestBase {

    private String owner;
    private String transporter;
    private String matchId;

    @BeforeEach
    void seedCompletedJob() throws SQLException {
        owner = insertUser("owner@example.com", "FREIGHT_OWNER");
        transporter = insertUser("transporter@example.com", "TRANSPORTER");
        matchId = insertMatch(insertLoad(owner), insertTruck(transporter));
    }

    @Test
    void rejects_rating_score_outside_one_to_five() {
        assertThatThrownBy(() -> insertRating(owner, transporter, 6))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ratings_score_range_check");
    }

    @Test
    void rejects_second_rating_by_same_rater_for_same_match() throws SQLException {
        insertRating(owner, transporter, 5);

        assertThatThrownBy(() -> insertRating(owner, transporter, 1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ratings_match_rater_unique");
    }

    @Test
    void rejects_a_user_rating_themselves() {
        assertThatThrownBy(() -> insertRating(owner, owner, 5))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ratings_distinct_parties_check");
    }

    @Test
    void allows_each_party_to_rate_the_other_once() throws SQLException {
        insertRating(owner, transporter, 5);
        insertRating(transporter, owner, 4);

        assertThat(single("SELECT count(*)::text FROM ratings")).isEqualTo("2");
    }

    @Test
    void retains_audit_log_after_the_entity_it_describes_is_deleted() throws SQLException {
        String loadId = insertLoad(owner);
        insertAuditLog("LOAD_CREATED", "LOAD", loadId);

        execute("DELETE FROM loads WHERE id = '" + loadId + "'");

        assertThat(single("SELECT count(*)::text FROM audit_logs WHERE entity_id = '" + loadId + "'"))
                .isEqualTo("1");
    }

    @Test
    void rejects_an_update_to_a_recorded_audit_event() throws SQLException {
        insertAuditLog("LOAD_CREATED", "LOAD", null);

        assertThatThrownBy(() -> execute("UPDATE audit_logs SET action = 'NOTHING_HAPPENED'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void rejects_a_delete_of_a_recorded_audit_event() throws SQLException {
        insertAuditLog("LOAD_CREATED", "LOAD", null);

        assertThatThrownBy(() -> execute("DELETE FROM audit_logs"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void stores_audit_details_as_queryable_json() throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, entity_type, details) "
                        + "VALUES (?::uuid, 'MATCH_ACCEPTED', 'MATCH', ?::jsonb)")) {
            insert.setString(1, owner);
            insert.setString(2, "{\"previous_status\": \"PROPOSED\"}");
            insert.execute();
        }

        assertThat(single("SELECT details->>'previous_status' FROM audit_logs"))
                .isEqualTo("PROPOSED");
    }

    private void insertRating(String rater, String ratee, int score) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO ratings (match_id, rater_id, ratee_id, score, comment) "
                        + "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'On time')")) {
            insert.setString(1, matchId);
            insert.setString(2, rater);
            insert.setString(3, ratee);
            insert.setInt(4, score);
            insert.execute();
        }
    }

    private void insertAuditLog(String action, String entityType, String entityId)
            throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id) "
                        + "VALUES (?::uuid, ?, ?, ?::uuid)")) {
            insert.setString(1, owner);
            insert.setString(2, action);
            insert.setString(3, entityType);
            insert.setString(4, entityId);
            insert.execute();
        }
    }
}
