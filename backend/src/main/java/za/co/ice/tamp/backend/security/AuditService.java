package za.co.ice.tamp.backend.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;

/**
 * The single place every feature writes an audit event through, so later issues call
 * {@link #record} instead of constructing {@link AuditLog} themselves. A synchronous, direct
 * write: nothing else consumes audit events, so an async queue would add failure modes (lost
 * events on crash) with no corresponding benefit.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UUID actorId, String action, String entityType, UUID entityId,
            Map<String, Object> details) {
        repository.save(new AuditLog(actorId, action, entityType, entityId, details));
    }
}
