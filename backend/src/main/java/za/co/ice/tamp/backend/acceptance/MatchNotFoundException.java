package za.co.ice.tamp.backend.acceptance;

import java.util.UUID;

/** Thrown when a match id has no matching row, translated to a 404 by the controller. */
public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(UUID id) {
        super("No match with id " + id);
    }
}
