package za.co.ice.tamp.backend.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Applies the raw SQL migrations in {@code db/migrations} to a database, in filename order.
 *
 * <p>The migrations live at the repository root rather than inside this module because the
 * dockerized local database (#7) and the Python matching-service both read the same schema;
 * owning them here would make the Java build the gatekeeper for a shared artifact.
 */
public final class SchemaFixture {

    private static final Path MIGRATIONS = Path.of("..", "db", "migrations");

    private SchemaFixture() {
    }

    /** Returns every migration file, sorted by filename so V1 runs before V2. */
    public static List<Path> migrationFiles() {
        if (!Files.isDirectory(MIGRATIONS)) {
            throw new IllegalStateException("No migrations directory at " + MIGRATIONS.toAbsolutePath());
        }
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<Path> sorted = files.filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
            if (sorted.isEmpty()) {
                throw new IllegalStateException("No .sql migrations found in " + MIGRATIONS.toAbsolutePath());
            }
            return sorted;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read migrations from " + MIGRATIONS.toAbsolutePath(), e);
        }
    }

    /** Runs every migration against the supplied connection, failing loudly on the first bad file. */
    public static void applyAll(Connection connection) {
        for (Path migration : migrationFiles()) {
            String sql;
            try {
                sql = Files.readString(migration);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + migration, e);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new IllegalStateException("Migration failed: " + migration.getFileName(), e);
            }
        }
    }
}
