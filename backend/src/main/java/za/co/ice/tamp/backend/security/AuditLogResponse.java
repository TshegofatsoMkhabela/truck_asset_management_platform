package za.co.ice.tamp.backend.security;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;

public record AuditLogResponse(
        UUID id, UUID actorId, String action, String entityType, UUID entityId,
        Map<String, Object> details, OffsetDateTime occurredAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorId(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getDetails(), log.getOccurredAt());
    }
}
