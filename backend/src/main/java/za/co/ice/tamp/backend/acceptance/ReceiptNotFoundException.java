package za.co.ice.tamp.backend.acceptance;

import java.util.UUID;

/**
 * Thrown when a match has no receipt, translated to a 404 by the controller.
 *
 * <p>This is the normal answer for a rejected or still-proposed match, not only for an
 * unknown id: FR-07 issues a receipt on acceptance, so its absence is meaningful rather
 * than an error condition to hide.
 */
public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(UUID matchId) {
        super("No receipt for match " + matchId + ". A receipt is issued only on acceptance.");
    }
}
