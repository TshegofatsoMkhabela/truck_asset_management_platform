package za.co.ice.tamp.backend.tracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * One mock trip update.
 *
 * <p>Status is required and coordinates are optional, following the brief's "mock coordinates
 * <em>or</em> status progression". The schema allows either, but a demo that advances a trip
 * without saying what stage it reached would show nothing useful.
 *
 * <p>The ranges mirror the {@code tracking_events} CHECK constraints so an impossible position
 * is a 400 naming the field rather than a 500 carrying a Postgres constraint name.
 */
public record TrackingEventRequest(
        @NotNull
        @Pattern(
                regexp = "DISPATCHED|IN_TRANSIT|ARRIVED|DELIVERED",
                message = "status must be DISPATCHED, IN_TRANSIT, ARRIVED or DELIVERED")
        String status,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude) {
}
