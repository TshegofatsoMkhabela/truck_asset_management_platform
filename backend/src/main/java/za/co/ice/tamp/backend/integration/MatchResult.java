package za.co.ice.tamp.backend.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One eligible truck returned by matching-service, ranked by score.
 *
 * <p>{@code truckId} needs an explicit {@link JsonProperty}: matching-service's
 * response uses {@code truck_id}, the same snake_case-versus-camelCase mismatch
 * {@link LoadInput} explains for the request side.
 */
public record MatchResult(
        @JsonProperty("truck_id") String truckId, double score, List<String> reasons) {
}
