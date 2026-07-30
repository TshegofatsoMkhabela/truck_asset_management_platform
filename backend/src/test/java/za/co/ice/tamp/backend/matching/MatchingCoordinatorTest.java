package za.co.ice.tamp.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import za.co.ice.tamp.backend.integration.MatchResponse;
import za.co.ice.tamp.backend.integration.MatchResult;
import za.co.ice.tamp.backend.integration.MatchingServiceClient;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Proves the full chain a matching request triggers: fetch the load and available
 * trucks, call matching-service, persist the eligible results, and write an audit
 * event naming the actor, the load, and how many matches came back.
 *
 * <p>{@link MatchingServiceClient} is mocked here rather than called for real:
 * this test exists to prove persistence and the audit trail, not the network hop
 * (already proven in {@code MatchingServiceClientTest}) or the 2-second budget
 * (its own dedicated timing test, which does use the real client).
 */
class MatchingCoordinatorTest extends JpaTestBase {

    @Autowired
    private MatchingCoordinator coordinator;
    @Autowired
    private UserRepository users;
    @Autowired
    private LoadRepository loads;
    @Autowired
    private TruckRepository trucks;
    @Autowired
    private MatchRepository matches;
    @Autowired
    private AuditLogRepository auditLogs;
    @MockBean
    private MatchingServiceClient matchingServiceClient;

    @Test
    void generatesAndPersistsEligibleMatchesWithAuditEvent() {
        User owner = users.save(new User("Owner", "mc-owner1@example.com", "hash", "FREIGHT_OWNER"));
        User transporter = users.save(
                new User("Transporter", "mc-transporter1@example.com", "hash", "TRANSPORTER"));
        Load load = loads.save(new Load(owner.getId(), "Johannesburg", "Durban", "GENERAL",
                new BigDecimal("10000.00"), new BigDecimal("20.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));
        Truck truck = trucks.save(new Truck(transporter.getId(), "FLATBED",
                new BigDecimal("20000.00"), null, "Johannesburg",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));

        when(matchingServiceClient.requestMatches(any())).thenReturn(new MatchResponse(
                List.of(new MatchResult(truck.getId().toString(), 87.5,
                        List.of("capacity sufficient", "same origin city")))));

        List<za.co.ice.tamp.backend.persistence.entity.Match> result =
                coordinator.generateMatches(load.getId(), owner.getId());

        assertThat(result).hasSize(1);
        assertThat(matches.findByLoadId(load.getId())).hasSize(1);

        List<za.co.ice.tamp.backend.persistence.entity.AuditLog> events =
                auditLogs.findByEntityTypeAndEntityId("LOAD", load.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getActorId()).isEqualTo(owner.getId());
        assertThat(events.get(0).getAction()).isEqualTo("MATCHES_REQUESTED");
        assertThat(events.get(0).getDetails()).containsEntry("matchCount", 1);
    }

    @Test
    void writesAnAuditEventEvenWhenNoTruckIsEligible() {
        // A zero-match result is a real outcome the audit trail must still record,
        // defending against an early "if no matches, skip the audit write" shortcut
        // that would make the trail silently incomplete for the empty case.
        User owner = users.save(new User("Owner2", "mc-owner2@example.com", "hash", "FREIGHT_OWNER"));
        Load load = loads.save(new Load(owner.getId(), "Johannesburg", "Durban", "GENERAL",
                new BigDecimal("10000.00"), new BigDecimal("20.00"),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2)));

        when(matchingServiceClient.requestMatches(any()))
                .thenReturn(new MatchResponse(List.of()));

        List<za.co.ice.tamp.backend.persistence.entity.Match> result =
                coordinator.generateMatches(load.getId(), owner.getId());

        assertThat(result).isEmpty();
        List<za.co.ice.tamp.backend.persistence.entity.AuditLog> events =
                auditLogs.findByEntityTypeAndEntityId("LOAD", load.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDetails()).containsEntry("matchCount", 0);
    }
}
