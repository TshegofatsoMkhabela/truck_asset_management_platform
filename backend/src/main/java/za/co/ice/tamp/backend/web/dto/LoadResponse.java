package za.co.ice.tamp.backend.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Load;

/** The body returned for every load-facing endpoint in this issue. */
public record LoadResponse(
        UUID id,
        UUID ownerId,
        String originCity,
        String destinationCity,
        String cargoType,
        BigDecimal weightKg,
        BigDecimal volumeM3,
        OffsetDateTime pickupWindowStart,
        OffsetDateTime pickupWindowEnd,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static LoadResponse from(Load load) {
        return new LoadResponse(
                load.getId(),
                load.getOwnerId(),
                load.getOriginCity(),
                load.getDestinationCity(),
                load.getCargoType(),
                load.getWeightKg(),
                load.getVolumeM3(),
                load.getPickupWindowStart(),
                load.getPickupWindowEnd(),
                load.getStatus(),
                load.getCreatedAt(),
                load.getUpdatedAt());
    }
}
