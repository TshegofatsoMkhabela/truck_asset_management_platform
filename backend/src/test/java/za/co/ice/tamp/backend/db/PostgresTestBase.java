package za.co.ice.tamp.backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A connection to an empty PostgreSQL database, one fresh schema per test.
 *
 * <p>Extend this only to test the migrations themselves. Everything that needs the schema
 * already applied should extend {@link MigratedSchemaTestBase} instead.
 *
 * <p>The container starts once for the whole test run rather than once per class, because
 * starting PostgreSQL costs seconds and the schema is dropped between tests anyway.
 */
abstract class PostgresTestBase {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("tamp")
                    .withUsername("tamp")
                    .withPassword("tamp");

    static {
        POSTGRES.start();
    }

    /** Open connection for the current test, on a database with an empty public schema. */
    protected Connection db;

    @BeforeEach
    void resetSchemaAndConnect() throws SQLException {
        db = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (Statement statement = db.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
        }
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (db != null) {
            db.close();
        }
    }

    protected void execute(String sql) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.execute();
        }
    }

    /** Runs a query expected to return one row and one column, as text. */
    protected String single(String sql) throws SQLException {
        try (PreparedStatement query = db.prepareStatement(sql);
                ResultSet rows = query.executeQuery()) {
            rows.next();
            return rows.getString(1);
        }
    }

    protected List<String> tableNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = db.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT table_name FROM information_schema.tables "
                                + "WHERE table_schema = 'public'")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }
}
