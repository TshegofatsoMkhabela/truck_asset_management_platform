package za.co.ice.tamp.backend.security;

/** Thrown by registration when the email is already in use. Mapped to 409 by GlobalExceptionHandler. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email " + email + " already exists");
    }
}
