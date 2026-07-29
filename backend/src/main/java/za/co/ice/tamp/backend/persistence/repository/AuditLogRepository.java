package za.co.ice.tamp.backend.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Backs FR-12: an admin views the trail for one entity regardless of whether it still exists. */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}
