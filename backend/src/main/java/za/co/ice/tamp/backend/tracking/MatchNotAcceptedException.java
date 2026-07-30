package za.co.ice.tamp.backend.tracking;

import java.util.UUID;

/**
 * Thrown when a trip event is logged against a match nobody accepted, translated to a 409
 * by the controller.
 *
 * <p>The brief scopes tracking to "an accepted match". The database cannot express that rule,
 * since a CHECK constraint cannot read another table, so this guard is the only thing
 * enforcing it. known-limitations.md recorded the gap and assigned it to #15.
 */
public class MatchNotAcceptedException extends RuntimeException {

    public MatchNotAcceptedException(UUID matchId, String currentStatus) {
        super("Match " + matchId + " is " + currentStatus
                + ", so it cannot be tracked. Only an accepted match has a trip.");
    }
}
