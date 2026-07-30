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
 * <p>{@code ownerId} is optional here, not {@code @NotNull}: {@link za.co.ice.tamp.backend.web.LoadController}
 * overrides it with the caller's JWT id when a real {@code Authorization: Bearer} token is
 * presented (see {@link za.co.ice.tamp.backend.security.CurrentUser}), and rejects the request
 * itself with a clear 400 if neither a token nor this field supplied one. Not a permanent
 * design choice; see known-limitations.md for the larger gap this is a stopgap for.
 *
 * <p>{@code cargoType} is validated here against the same six values the database's
 * {@code CHECK} constraint on {@code loads.cargo_type} enforces, so an invalid type is
 * rejected with a 400 before it reaches Postgres.
 */
public record CreateLoadRequest(
        UUID ownerId,
        @NotBlank String originCity,
        @NotBlank String destinationCity,
        @NotBlank @Pattern(regexp = "GENERAL|REFRIGERATED|HAZARDOUS|LIQUID|CONTAINER|BULK") String cargoType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal volumeM3,
        @NotNull OffsetDateTime pickupWindowStart,
        @NotNull OffsetDateTime pickupWindowEnd
) {
}
