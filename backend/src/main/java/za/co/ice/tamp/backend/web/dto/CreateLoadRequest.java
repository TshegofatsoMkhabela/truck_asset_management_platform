package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The body of {@code POST /loads}.
 *
 * <p>{@code ownerId} is accepted as an explicit field, not read from an authenticated
 * principal: #9 (RBAC and auth) has not merged, so there is no principal to read yet. This is
 * a deliberate, temporary seam, replaced by the authenticated caller's own id once #9 lands,
 * not a permanent design choice.
 *
 * <p>{@code cargoType} is validated here against the same six values the database's
 * {@code CHECK} constraint on {@code loads.cargo_type} enforces, so an invalid type is
 * rejected with a 400 before it reaches Postgres.
 */
public record CreateLoadRequest(
        @NotNull UUID ownerId,
        @NotBlank String originCity,
        @NotBlank String destinationCity,
        @NotBlank @Pattern(regexp = "GENERAL|REFRIGERATED|HAZARDOUS|LIQUID|CONTAINER|BULK") String cargoType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal volumeM3,
        @NotNull OffsetDateTime pickupWindowStart,
        @NotNull OffsetDateTime pickupWindowEnd
) {
}
