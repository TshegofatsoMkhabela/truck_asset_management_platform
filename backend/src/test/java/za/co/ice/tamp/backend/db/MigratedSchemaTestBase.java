package za.co.ice.tamp.backend.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;

/**
 * A database with every migration applied, plus the row builders the schema tests share.
 *
 * <p>The builders exist so a test reads as the rule it is checking rather than as an INSERT
 * statement: a test about rating scores should not restate how a load is constructed. Every
 * builder is parameterised only on the columns some test actually varies; everything else is
 * a valid default, so an unrelated constraint failure cannot be mistaken for the rule under test.
 *
 * <p>All statements are parameterised rather than concatenated. Production code must never
 * build SQL by string joining, and test fixtures are the first place that habit erodes.
 */
abstract class MigratedSchemaTestBase extends PostgresTestBase {

    protected static final Instant MONDAY = Instant.parse("2026-08-03T06:00:00Z");
    protected static final Instant FRIDAY = MONDAY.plus(4, ChronoUnit.DAYS);

    /** A well-formed UUID that is deliberately not present in any table. */
    protected static final String GHOST = "0192f0a0-0000-7000-8000-000000000000";

    @BeforeEach
    void applyMigrations() {
        SchemaFixture.applyAll(db);
    }

    protected String insertUser(String email, String role) throws SQLException {
        return insertReturningId(
                "INSERT INTO users (full_name, email, password_hash, role) "
                        + "VALUES ('Test User', ?, 'not-a-real-hash', ?) RETURNING id::text",
                email, role);
    }

    protected String insertLoad(String ownerId) throws SQLException {
        return insertLoad(ownerId, 12_000);
    }

    protected String insertLoad(String ownerId, int weightKg) throws SQLException {
        return insertLoad(ownerId, weightKg, MONDAY, FRIDAY);
    }

    protected String insertLoad(String ownerId, int weightKg, Instant from, Instant until)
            throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO loads (owner_id, origin_city, destination_city, cargo_type, "
                        + "weight_kg, volume_m3, pickup_window_start, pickup_window_end) "
                        + "VALUES (?::uuid, 'Johannesburg', 'Durban', 'GENERAL', ?, 40, ?, ?) "
                        + "RETURNING id::text")) {
            insert.setString(1, ownerId);
            insert.setInt(2, weightKg);
            insert.setTimestamp(3, Timestamp.from(from));
            insert.setTimestamp(4, Timestamp.from(until));
            return firstColumn(insert);
        }
    }

    protected String insertTruck(String transporterId) throws SQLException {
        return insertTruck(transporterId, 30_000);
    }

    protected String insertTruck(String transporterId, int capacityKg) throws SQLException {
        return insertTruck(transporterId, capacityKg, MONDAY, FRIDAY, "FLATBED");
    }

    protected String insertTruck(String transporterId, int capacityKg, Instant from, Instant until)
            throws SQLException {
        return insertTruck(transporterId, capacityKg, from, until, "FLATBED");
    }

    protected String insertTruck(
            String transporterId, int capacityKg, Instant from, Instant until, String vehicleType)
            throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO trucks (transporter_id, vehicle_type, capacity_kg, current_city, "
                        + "available_from, available_until) "
                        + "VALUES (?::uuid, ?, ?, 'Johannesburg', ?, ?) RETURNING id::text")) {
            insert.setString(1, transporterId);
            insert.setString(2, vehicleType);
            insert.setInt(3, capacityKg);
            insert.setTimestamp(4, Timestamp.from(from));
            insert.setTimestamp(5, Timestamp.from(until));
            return firstColumn(insert);
        }
    }

    protected String insertMatch(String loadId, String truckId) throws SQLException {
        return insertMatch(loadId, truckId, 75.0, "[\"capacity sufficient\"]");
    }

    protected String insertMatch(String loadId, String truckId, double score, String reasonsJson)
            throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO matches (load_id, truck_id, score, reasons) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?::jsonb) RETURNING id::text")) {
            insert.setString(1, loadId);
            insert.setString(2, truckId);
            insert.setDouble(3, score);
            insert.setString(4, reasonsJson);
            return firstColumn(insert);
        }
    }

    private String insertReturningId(String sql, String... values) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                insert.setString(i + 1, values[i]);
            }
            return firstColumn(insert);
        }
    }

    private String firstColumn(PreparedStatement statement) throws SQLException {
        try (ResultSet row = statement.executeQuery()) {
            row.next();
            return row.getString(1);
        }
    }
}
