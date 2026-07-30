package za.co.ice.tamp.backend.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * One mock tracking update for an accepted match (FR-08, issue #15).
 *
 * <p>Mirrors the schema's {@code tracking_events_position_or_status_check}: an event must
 * carry a status, a full coordinate pair, or both. Validating it here returns a clean 400
 * instead of surfacing the database constraint as a 500.
 */
@Schema(description = "A mock trip update: a status, a coordinate pair, or both.",
        example = """
                {"status": "IN_TRANSIT", "latitude": -26.204103, "longitude": 28.047305}
                """)
public record CreateTrackingEventRequest(

        @Pattern(regexp = "DISPATCHED|IN_TRANSIT|ARRIVED|DELIVERED",
                message = "status must be one of DISPATCHED, IN_TRANSIT, ARRIVED, DELIVERED")
        @Schema(description = "Mock trip stage", example = "IN_TRANSIT", nullable = true)
        String status,

        @DecimalMin(value = "-90") @DecimalMax(value = "90")
        @Schema(description = "Synthetic latitude", example = "-26.204103", nullable = true)
        BigDecimal latitude,

        @DecimalMin(value = "-180") @DecimalMax(value = "180")
        @Schema(description = "Synthetic longitude", example = "28.047305", nullable = true)
        BigDecimal longitude) {

    @AssertTrue(message = "an event needs a status, a latitude/longitude pair, or both")
    @Schema(hidden = true)
    public boolean isStatusOrPositionPresent() {
        return status != null || (latitude != null && longitude != null);
    }
}
