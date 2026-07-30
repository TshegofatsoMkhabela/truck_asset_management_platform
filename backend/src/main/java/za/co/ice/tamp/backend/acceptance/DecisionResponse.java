package za.co.ice.tamp.backend.acceptance;

import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Match;

/** What the caller gets back after deciding a match: the outcome, who decided, and when. */
public record DecisionResponse(
        UUID matchId,
        String status,
        UUID decidedBy,
        OffsetDateTime decidedAt) {

    public static DecisionResponse from(Match match) {
        return new DecisionResponse(
                match.getId(),
                match.getStatus(),
                match.getDecidedBy(),
                match.getDecidedAt());
    }
}
