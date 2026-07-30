package za.co.ice.tamp.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Match;

/**
 * The issue's stated evidence for the brief's one quantified performance figure:
 * matching returns within 2 seconds on the seeded sample data (#7).
 *
 * <p>Tagged {@code e2e} like {@code CrossServiceIntegrationE2ETest} was: this test
 * needs a real matching-service process reachable at {@code MATCHING_SERVICE_URL}
 * (default {@code http://localhost:8000}), not a mock, because the number this test
 * produces is meant to answer "how long does the real round trip take", not "how
 * fast is the Java side alone". Run with {@code mvn verify -Pe2e}.
 *
 * <p>Applies #7's actual {@code db/seed/dev-seed.sql}, not ad-hoc rows built in
 * Java, so "against the seeded sample data" means the literal dataset a reviewer
 * would see after a fresh {@code docker compose up}, not a lookalike.
 */
@Tag("e2e")
class MatchingTimingE2ETest extends JpaTestBase {

    private static final UUID OPEN_LOAD_ID =
            UUID.fromString("00000000-0000-7000-8000-000000000011");
    private static final UUID OWNER_ID =
            UUID.fromString("00000000-0000-7000-8000-000000000001");
    private static final UUID ELIGIBLE_TRUCK_ID =
            UUID.fromString("00000000-0000-7000-8000-000000000021");

    @Autowired
    private MatchingCoordinator coordinator;

    @BeforeAll
    static void applySeedData() throws SQLException, IOException {
        String seedSql = Files.readString(Path.of("..", "db", "seed", "dev-seed.sql"));
        try (Connection connection =
                        DriverManager.getConnection(jdbcUrl(), jdbcUsername(), jdbcPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(seedSql);
        }
    }

    @Test
    void matchesTheSeededOpenLoadWithinTwoSecondsAndFindsTheEligibleTruck() {
        long start = System.nanoTime();

        List<Match> matches = coordinator.generateMatches(OPEN_LOAD_ID, OWNER_ID);

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        // Recorded in docs/testing-summary.md rather than only asserted: the brief
        // asks for evidence of the figure, not just a pass/fail against it.
        System.out.println("Matching round trip took " + elapsedMillis + "ms");

        assertThat(elapsedMillis).isLessThan(2000);
        assertThat(matches).extracting(Match::getTruckId).containsExactly(ELIGIBLE_TRUCK_ID);
    }
}
