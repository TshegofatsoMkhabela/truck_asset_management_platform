package za.co.ice.tamp.backend.tracking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;

/** One recorded stage of a simulated trip. Coordinates are null when only a status was sent. */
public record TrackingEventResponse(
        UUID id,
        UUID matchId,
        String status,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime occurredAt) {

    public static TrackingEventResponse from(TrackingEvent event) {
        return new TrackingEventResponse(
                event.getId(),
                event.getMatchId(),
                event.getStatus(),
                event.getLatitude(),
                event.getLongitude(),
                event.getOccurredAt());
    }
}
