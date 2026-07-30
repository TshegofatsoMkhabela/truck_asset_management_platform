package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.Pattern;

/**
 * The body of {@code PATCH /loads/{id}}.
 *
 * <p>Only {@code status} is updatable. The issue's Out of Scope list excludes lifecycle
 * workflows beyond a "basic update"; a status transition (e.g. to {@code MATCHED} once #13's
 * matching logic exists) is that basic update. Other fields (cities, weight, window) are not
 * exposed here since nothing in the issue or the brief calls for correcting them after
 * posting, and adding that now would be unrequested scope.
 */
public record UpdateLoadRequest(
        @Pattern(regexp = "OPEN|MATCHED|IN_TRANSIT|DELIVERED|CANCELLED") String status
) {
}
