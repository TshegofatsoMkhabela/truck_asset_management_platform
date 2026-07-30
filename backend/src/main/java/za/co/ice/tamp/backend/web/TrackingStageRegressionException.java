package za.co.ice.tamp.backend.web;

import java.util.UUID;

/**
 * Thrown when a new tracking status would move a match's trip backwards
 * (e.g. DELIVERED then DISPATCHED), translated to a 409 by {@link TrackingController}.
 */
public class TrackingStageRegressionException extends RuntimeException {

    public TrackingStageRegressionException(UUID matchId, String currentStatus, String attemptedStatus) {
        super("Match " + matchId + " is already at " + currentStatus
                + "; cannot move back to " + attemptedStatus);
    }
}
