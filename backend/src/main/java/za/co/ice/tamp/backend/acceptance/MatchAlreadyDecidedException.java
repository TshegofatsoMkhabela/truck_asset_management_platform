package za.co.ice.tamp.backend.acceptance;

import java.util.UUID;

/**
 * Thrown when a match that already carries a decision is decided again, translated to a 409
 * by the controller.
 *
 * <p>Caught in the service layer rather than left to the database: a second acceptance would
 * otherwise violate {@code receipts.match_id UNIQUE} and reach the caller as an unexplained
 * 500, which says nothing about what the caller did wrong.
 */
public class MatchAlreadyDecidedException extends RuntimeException {

    public MatchAlreadyDecidedException(UUID id, String currentStatus) {
        super("Match " + id + " was already decided as " + currentStatus);
    }
}
