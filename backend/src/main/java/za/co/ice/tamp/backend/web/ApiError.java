package za.co.ice.tamp.backend.web;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * The one error response shape every endpoint returns, so later feature issues reference this
 * instead of each inventing their own. {@code fieldErrors} is empty (not null) when the failure
 * is not a validation failure, so clients never need a null check.
 *
 * <p>{@link #of} is the single construction path, used both by {@link GlobalExceptionHandler}
 * (failures inside a controller) and by the Spring Security layer's own writers (failures
 * rejected before a request ever reaches a controller, which {@code @RestControllerAdvice}
 * cannot intercept). The two call sites can't share dispatch machinery, but they share this
 * factory so the shape itself can't drift between them.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String error, String message) {
        return new ApiError(OffsetDateTime.now(), status.value(), error, message, Map.of());
    }

    public static ApiError of(HttpStatus status, String error, String message,
            Map<String, String> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), status.value(), error, message, fieldErrors);
    }
}
