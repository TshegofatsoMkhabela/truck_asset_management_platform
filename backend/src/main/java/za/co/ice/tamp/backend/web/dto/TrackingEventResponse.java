package za.co.ice.tamp.backend.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;

/** A stored tracking event as returned by the API. */
public record TrackingEventResponse(
        UUID id,
        UUID matchId,
        BigDecimal latitude,
        BigDecimal longitude,
        String status,
        OffsetDateTime occurredAt) {

    public static TrackingEventResponse from(TrackingEvent event) {
        return new TrackingEventResponse(
                event.getId(),
                event.getMatchId(),
                event.getLatitude(),
                event.getLongitude(),
                event.getStatus(),
                event.getOccurredAt());
    }
}
