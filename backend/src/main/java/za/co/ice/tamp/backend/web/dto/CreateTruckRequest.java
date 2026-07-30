package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code transporterId} is optional here, not {@code @NotNull}: {@link za.co.ice.tamp.backend.web.TruckController}
 * overrides it with the caller's JWT id when a real {@code Authorization: Bearer} token is
 * presented, and rejects the request with a clear 400 if neither a token nor this field
 * supplied one.
 */
public record CreateTruckRequest(
        UUID transporterId,

        @NotBlank(message = "vehicleType is required")
        String vehicleType,

        @NotNull(message = "capacityKg is required")
        @Positive(message = "capacityKg must be positive")
        BigDecimal capacityKg,

        /** Nullable: flatbed trucks have no volume constraint. */
        BigDecimal capacityM3,

        @NotBlank(message = "currentCity is required")
        String currentCity,

        @NotNull(message = "availableFrom is required")
        OffsetDateTime availableFrom,

        @NotNull(message = "availableUntil is required")
        OffsetDateTime availableUntil
) {
}
