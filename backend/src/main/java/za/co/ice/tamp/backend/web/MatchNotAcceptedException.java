package za.co.ice.tamp.backend.web;

import java.util.UUID;

/**
 * Thrown when tracking is attempted on a match that exists but is not ACCEPTED,
 * translated to a 409 by {@link TrackingController}: a PROPOSED or REJECTED match has no trip.
 */
public class MatchNotAcceptedException extends RuntimeException {

    public MatchNotAcceptedException(UUID id, String status) {
        super("Match " + id + " is " + status + ", not ACCEPTED; only accepted matches can be tracked");
    }
}
