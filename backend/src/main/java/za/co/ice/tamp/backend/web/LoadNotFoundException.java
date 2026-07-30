package za.co.ice.tamp.backend.web;

import java.util.UUID;

/** Thrown when a requested load id has no matching row, translated to a 404 by {@link LoadController}. */
public class LoadNotFoundException extends RuntimeException {

    public LoadNotFoundException(UUID id) {
        super("No load with id " + id);
    }
}
