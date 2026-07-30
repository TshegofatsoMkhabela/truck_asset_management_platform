package za.co.ice.tamp.backend.integration;

import java.util.List;

/** The full {@code /match} request body: one load and its candidate trucks. */
public record MatchRequest(LoadInput load, List<TruckInput> trucks) {
}
