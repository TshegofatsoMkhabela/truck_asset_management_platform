package za.co.ice.tamp.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the constraints on {@code matches} and {@code receipts}, the record of which truck was
 * proposed for which load, what was decided, and the confirmation that decision produced.
 */
class MatchLifecycleSchemaTest extends MigratedSchemaTestBase {

    private String owner;
    private String loadId;
    private String truckId;

    @BeforeEach
    void seedParties() throws SQLException {
        owner = insertUser("owner@example.com", "FREIGHT_OWNER");
        loadId = insertLoad(owner);
        truckId = insertTruck(insertUser("transporter@example.com", "TRANSPORTER"));
    }

    @Test
    void rejects_match_referencing_nonexistent_load() {
        assertThatThrownBy(() -> insertMatch(GHOST, truckId))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("matches_load_id_fkey");
    }

    @Test
    void rejects_match_referencing_nonexistent_truck() {
        assertThatThrownBy(() -> insertMatch(loadId, GHOST))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("matches_truck_id_fkey");
    }

    @Test
    void rejects_duplicate_match_for_same_load_and_truck() throws SQLException {
        insertMatch(loadId, truckId);

        assertThatThrownBy(() -> insertMatch(loadId, truckId))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("matches_load_truck_unique");
    }

    @Test
    void stores_match_reasons_as_queryable_json() throws SQLException {
        insertMatch(loadId, truckId, 87.5, "[\"capacity sufficient\", \"same origin city\"]");

        assertThat(single("SELECT reasons->>0 FROM matches")).isEqualTo("capacity sufficient");
        assertThat(single("SELECT score::text FROM matches")).isEqualTo("87.50");
    }

    @Test
    void rejects_a_decided_match_with_no_decision_timestamp() throws SQLException {
        String matchId = insertMatch(loadId, truckId);

        assertThatThrownBy(() -> execute(
                        "UPDATE matches SET status = 'ACCEPTED' WHERE id = '" + matchId + "'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("matches_decision_consistency_check");
    }

    @Test
    void rejects_second_receipt_for_same_match() throws SQLException {
        String matchId = accept(insertMatch(loadId, truckId));
        insertReceipt(matchId);

        assertThatThrownBy(() -> insertReceipt(matchId))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("receipts_match_id_key");
    }

    @Test
    void issues_a_distinct_contract_id_for_every_receipt() throws SQLException {
        String firstMatch = accept(insertMatch(loadId, truckId));
        String secondTruck = insertTruck(insertUser("second@example.com", "TRANSPORTER"));
        String secondMatch = accept(insertMatch(loadId, secondTruck));

        insertReceipt(firstMatch);
        insertReceipt(secondMatch);

        assertThat(single("SELECT count(DISTINCT contract_id)::text FROM receipts")).isEqualTo("2");
        assertThat(single("SELECT contract_id FROM receipts LIMIT 1")).startsWith("TAMP-");
    }

    @Test
    void accepts_a_receipt_with_no_ip_or_user_agent_recorded() throws SQLException {
        insertReceipt(accept(insertMatch(loadId, truckId)));

        // Brief section 2.2 asks for IP/user-agent "where available", so absence is valid data,
        // not a missing value to be defaulted.
        assertThat(single("SELECT count(*)::text FROM receipts WHERE ip_address IS NULL"))
                .isEqualTo("1");
    }

    /** Moves a match to ACCEPTED, setting all three decision columns as the schema requires. */
    private String accept(String matchId) throws SQLException {
        execute("UPDATE matches SET status = 'ACCEPTED', decided_at = now(), decided_by = '"
                + owner + "' WHERE id = '" + matchId + "'");
        return matchId;
    }

    private void insertReceipt(String matchId) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO receipts (match_id, decision, actor_id) "
                        + "VALUES (?::uuid, 'ACCEPTED', ?::uuid)")) {
            insert.setString(1, matchId);
            insert.setString(2, owner);
            insert.execute();
        }
    }
}
