package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTruckRequest(
        @NotNull(message = "transporterId is required")
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
