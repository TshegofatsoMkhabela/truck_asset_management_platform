package za.co.ice.tamp.backend.web.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;

/** One audit trail entry as returned to an admin. */
public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> details,
        OffsetDateTime occurredAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getOccurredAt());
    }
}
