package za.co.ice.tamp.backend.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads the caller's id from a real JWT when one was presented, falling back to a
 * caller-supplied value otherwise.
 *
 * <p>Additive rather than enforced: every business endpoint still accepts the old
 * caller-supplied id fields (see known-limitations.md for why nothing has been made to
 * require a token), but a request that actually carries a valid {@code Authorization: Bearer}
 * header now has its own id used in place of whatever the body/query claims, so a Swagger UI
 * demo that has logged in no longer needs the same id retyped into every request. This is
 * deliberately not the same as real RBAC enforcement: a caller who omits the header still
 * gets through unauthenticated, exactly as before.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID idOrFallback(Authentication authentication, UUID fallback) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return fallback;
        }
        return UUID.fromString(authentication.getName());
    }

    /**
     * Same as {@link #idOrFallback}, but for the fields made optional in their request DTO so
     * a real token could replace them: throws a 400 naming the field if neither a token nor
     * the field itself supplied an id, rather than letting a null reach persistence.
     */
    public static UUID requireIdOrFallback(Authentication authentication, UUID fallback, String fieldName) {
        UUID id = idOrFallback(authentication, fallback);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required when not logged in with a Bearer token");
        }
        return id;
    }
}
