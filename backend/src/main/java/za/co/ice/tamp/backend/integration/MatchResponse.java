package za.co.ice.tamp.backend.integration;

import java.util.List;

/** The full {@code /match} response body from matching-service. */
public record MatchResponse(List<MatchResult> matches) {
}
