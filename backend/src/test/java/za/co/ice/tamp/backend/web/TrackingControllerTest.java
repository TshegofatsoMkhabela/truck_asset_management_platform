package za.co.ice.tamp.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Exercises {@code TrackingController} against the real migrated schema (see
 * {@link JpaTestBase}), covering issue #15's Minimum Integration Test (advance a match's
 * tracking status, fetch it back, confirm it persisted) plus the refusal cases.
 *
 * <p>@Transactional is disabled for the same reason as {@code LoadControllerTest}:
 * audit_logs is append-only, and a shared test transaction would make Hibernate flush an
 * UPDATE against it at test end.
 */
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private MatchRepository matchRepository;

    /**
     * matches.load_id/truck_id are real foreign keys, and the decision-consistency
     * constraint requires decided_at/decided_by alongside a non-PROPOSED status, so an
     * accepted match needs the whole chain seeded: user, load, truck, then the decided match.
     */
    private UUID seedMatch(String status) {
        User user = userRepository.save(
                new User("Demo User", "tracking." + UUID.randomUUID() + "@example.com",
                        "irrelevant-hash", "TRANSPORTER"));
        OffsetDateTime now = OffsetDateTime.now();
        Load load = loadRepository.save(new Load(user.getId(), "Johannesburg", "Cape Town",
                "GENERAL", new BigDecimal("500.00"), new BigDecimal("2.50"),
                now.plusDays(1), now.plusDays(1).plusHours(6)));
        Truck truck = truckRepository.save(new Truck(user.getId(), "CONTAINER",
                new BigDecimal("5000.00"), new BigDecimal("20.00"), "Johannesburg",
                now, now.plusDays(3)));
        Match match = matchRepository.save(new Match(load.getId(), truck.getId(),
                new BigDecimal("90.00"), List.of("seeded for tracking test")));
        if (!"PROPOSED".equals(status)) {
            match.decide(status, user.getId());
            match = matchRepository.save(match);
        }
        return match.getId();
    }

    private String eventBody(String status) {
        return """
                {"status": "%s", "latitude": -26.204103, "longitude": 28.047305}
                """.formatted(status);
    }

    @Test
    void advancesStatusAndReadsItBackInOrder() throws Exception {
        // Issue #15's Minimum Integration Test: advance tracking status via the API, then
        // fetch it back and confirm the update persisted, oldest event first.
        UUID matchId = seedMatch("ACCEPTED");

        mockMvc.perform(post("/matches/" + matchId + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("IN_TRANSIT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()));

        mockMvc.perform(post("/matches/" + matchId + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("DELIVERED")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/matches/" + matchId + "/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$[1].status").value("DELIVERED"));
    }

    @Test
    void acceptsAPositionOnlyEvent() throws Exception {
        // The schema's "position or status" rule: coordinates alone are a valid event.
        UUID matchId = seedMatch("ACCEPTED");

        mockMvc.perform(post("/matches/" + matchId + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude": -29.858680, "longitude": 31.021840}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitude").value(-29.858680))
                .andExpect(jsonPath("$.status").isEmpty());
    }

    @Test
    void rejectsAnEventWithNeitherPositionNorStatus() throws Exception {
        UUID matchId = seedMatch("ACCEPTED");

        mockMvc.perform(post("/matches/" + matchId + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesTrackingForAMatchThatIsNotAccepted() throws Exception {
        UUID matchId = seedMatch("PROPOSED");

        mockMvc.perform(post("/matches/" + matchId + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("DISPATCHED")))
                .andExpect(status().isConflict());
    }

    @Test
    void returns404ForAnUnknownMatch() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(post("/matches/" + unknown + "/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("DISPATCHED")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/matches/" + unknown + "/tracking"))
                .andExpect(status().isNotFound());
    }
}
