package za.co.ice.tamp.backend.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import za.co.ice.tamp.backend.MatchFixture;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.web.MatchNotFoundException;

/**
 * Proves a proposed match can be accepted or rejected, the decision is persisted with
 * its actor, and the commitment is recorded in the audit trail.
 *
 * <p>Emails carry an {@code ac-} prefix so they cannot collide with another test class's, for
 * the reason {@link MatchFixture} documents.
 */
class AcceptanceCoordinatorTest extends JpaTestBase {

    @Autowired
    private AcceptanceCoordinator coordinator;
    @Autowired
    private MatchFixture fixture;
    @Autowired
    private MatchRepository matches;
    @Autowired
    private AuditLogRepository auditLogs;

    @Test
    void persistsAnAcceptedDecisionWithItsActorAndAuditEvent() {
        User owner = fixture.owner("ac-owner1@example.com");
        Match match = fixture.proposedMatch(owner, "ac-transporter1@example.com");

        Match decided = coordinator.decide(match.getId(), "ACCEPTED", owner.getId(), null, null);

        assertThat(decided.getStatus()).isEqualTo("ACCEPTED");
        assertThat(decided.getDecidedBy()).isEqualTo(owner.getId());
        assertThat(decided.getDecidedAt()).isNotNull();

        Match reread = matches.findById(match.getId()).orElseThrow();
        assertThat(reread.getStatus()).isEqualTo("ACCEPTED");

        List<AuditLog> events = auditLogs.findByEntityTypeAndEntityId("MATCH", match.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getActorId()).isEqualTo(owner.getId());
        assertThat(events.get(0).getAction()).isEqualTo("MATCH_ACCEPTED");
    }

    @Test
    void rejectsASecondDecisionOnAnAlreadyDecidedMatch() {
        // The guard has to run before any write. Without it the second accept reaches the
        // database and trips receipts.match_id UNIQUE, which surfaces to the caller as an
        // opaque 500 rather than a 409 that says the match was already decided.
        User owner = fixture.owner("ac-owner2@example.com");
        Match match = fixture.proposedMatch(owner, "ac-transporter2@example.com");
        coordinator.decide(match.getId(), "ACCEPTED", owner.getId(), null, null);

        assertThatThrownBy(
                () -> coordinator.decide(match.getId(), "REJECTED", owner.getId(), null, null))
                .isInstanceOf(MatchAlreadyDecidedException.class);

        assertThat(matches.findById(match.getId()).orElseThrow().getStatus()).isEqualTo("ACCEPTED");
        assertThat(auditLogs.findByEntityTypeAndEntityId("MATCH", match.getId())).hasSize(1);
    }

    @Test
    void rejectsADecisionOnAMatchThatDoesNotExist() {
        assertThatThrownBy(
                () -> coordinator.decide(UUID.randomUUID(), "ACCEPTED", UUID.randomUUID(), null, null))
                .isInstanceOf(MatchNotFoundException.class);
    }
}
