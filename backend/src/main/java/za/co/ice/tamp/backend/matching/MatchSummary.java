package za.co.ice.tamp.backend.matching;

import java.util.List;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Match;

/** The HTTP response shape for one persisted match, not the JPA entity directly. */
public record MatchSummary(UUID id, UUID truckId, double score, List<String> reasons) {

    static MatchSummary from(Match match) {
        return new MatchSummary(
                match.getId(), match.getTruckId(), match.getScore().doubleValue(), match.getReasons());
    }
}
