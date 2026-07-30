package za.co.ice.tamp.backend.acceptance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.MatchStatus;

/**
 * The accept-or-reject instruction for a match.
 *
 * <p>The pattern is checked here rather than left to the database: {@code matches.status}
 * carries its own CHECK constraint, but reaching it turns a caller typo into a 500 with a
 * Postgres error string attached, instead of a 400 naming the field.
 *
 * <p>{@code actorId} is in the body because there is no authenticated caller to read it from
 * until #9 lands, matching what #13 already does for {@code requestedBy}.
 */
public record DecisionRequest(
        @NotNull
        @Pattern(
                regexp = MatchStatus.ACCEPTED + "|" + MatchStatus.REJECTED,
                message = "decision must be ACCEPTED or REJECTED")
        String decision,

        @NotNull UUID actorId) {
}
