package za.co.ice.tamp.backend.web;

import java.util.UUID;

/** Thrown when a requested user id has no matching row, translated to a 404 by {@link UserController}. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("No user with id " + id);
    }
}
