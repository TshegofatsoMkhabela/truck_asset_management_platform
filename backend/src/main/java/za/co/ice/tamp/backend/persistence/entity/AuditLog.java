package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps {@code audit_logs} (db/migrations/V07__audit_logs.sql).
 *
 * <p>No relationship annotations to {@code User} or any other entity: the table itself carries
 * no foreign keys, by design (ADR-2), so rows survive deletion of whatever they describe.
 * Mapping {@code actor_id}/{@code entity_id} as a JPA association would silently reintroduce
 * that coupling on the Java side even though the database has none.
 *
 * <p>The append-only trigger from V07 means this entity is read and inserted, never updated
 * or deleted through the repository; no setters exist for that reason.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(name = "occurred_at", insertable = false, updatable = false)
    private OffsetDateTime occurredAt;

    protected AuditLog() {
    }

    public AuditLog(UUID actorId, String action, String entityType, UUID entityId,
            Map<String, Object> details) {
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
