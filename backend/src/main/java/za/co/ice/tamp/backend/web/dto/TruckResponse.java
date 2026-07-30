package za.co.ice.tamp.backend.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Truck;

public record TruckResponse(
        UUID id,
        UUID transporterId,
        String vehicleType,
        BigDecimal capacityKg,
        BigDecimal capacityM3,
        String currentCity,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TruckResponse from(Truck truck) {
        return new TruckResponse(
                truck.getId(),
                truck.getTransporterId(),
                truck.getVehicleType(),
                truck.getCapacityKg(),
                truck.getCapacityM3(),
                truck.getCurrentCity(),
                truck.getStatus(),
                truck.getCreatedAt(),
                truck.getUpdatedAt()
        );
    }
}
