package za.co.ice.tamp.backend.matching;

import java.util.UUID;

/**
 * Request body for {@code POST /loads/{loadId}/matches}.
 *
 * <p>{@code requestedBy} stands in for an authenticated caller; see known-limitations.md
 * for why that is an explicit, recorded gap rather than a silent assumption.
 */
public record GenerateMatchesRequest(UUID requestedBy) {
}
