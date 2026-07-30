package za.co.ice.tamp.backend.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * One candidate truck in a {@code /match} request to matching-service.
 *
 * <p>See {@link LoadInput} for why every field carries an explicit
 * {@link JsonProperty}: the snake_case wire names matching-service's Pydantic
 * schema expects do not match Jackson's default camelCase serialisation.
 */
public record TruckInput(
        String id,
        @JsonProperty("current_city") String currentCity,
        @JsonProperty("vehicle_type") String vehicleType,
        @JsonProperty("capacity_kg") double capacityKg,
        @JsonProperty("available_from") OffsetDateTime availableFrom,
        @JsonProperty("available_until") OffsetDateTime availableUntil) {
}
