package za.co.ice.tamp.backend.web;

import java.util.UUID;

public class DisputeNotFoundException extends RuntimeException {

    public DisputeNotFoundException(UUID id) {
        super("No dispute with id " + id);
    }
}
