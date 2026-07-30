package za.co.ice.tamp.backend.web;

/**
 * Thrown when an admin-only endpoint is called without a valid ADMIN user id,
 * translated to a 403 by {@link AdminController}.
 */
public class NotAdminException extends RuntimeException {

    public NotAdminException() {
        super("This endpoint requires the ADMIN role");
    }
}
