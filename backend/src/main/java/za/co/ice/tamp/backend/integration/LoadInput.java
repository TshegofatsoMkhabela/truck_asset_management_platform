package za.co.ice.tamp.backend.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * The load half of a {@code /match} request to matching-service.
 *
 * <p>Deliberately separate from the {@code Load} JPA entity: this describes the
 * wire format the other service expects, and the two should be free to diverge.
 *
 * <p>Every field is explicitly named via {@link JsonProperty}: Jackson (the JSON
 * library Spring uses) serialises Java records to {@code camelCase} by default,
 * but matching-service's Pydantic schema expects exact {@code snake_case} keys
 * ({@code origin_city}, not {@code originCity}). Without these annotations the
 * request would serialise to field names the other service silently ignores,
 * since FastAPI treats an unrecognised key as simply absent rather than an error.
 */
public record LoadInput(
        String id,
        @JsonProperty("origin_city") String originCity,
        @JsonProperty("cargo_type") String cargoType,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("pickup_window_start") OffsetDateTime pickupWindowStart,
        @JsonProperty("pickup_window_end") OffsetDateTime pickupWindowEnd) {
}
