package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps {@code matches} (db/migrations/V04__matches.sql).
 *
 * <p>{@code reasons} uses Hibernate's native {@code @JdbcTypeCode(SqlTypes.JSON)} rather than
 * an {@code AttributeConverter<List<String>, String>}: PostgreSQL has no implicit cast from
 * {@code varchar} to {@code jsonb}, so binding a converted String parameter directly would fail
 * at the JDBC level with "column is of type jsonb but expression is of type character varying".
 * The native JSON type binds through the correct PostgreSQL wire format instead.
 */
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "load_id")
    private UUID loadId;

    @Column(name = "truck_id")
    private UUID truckId;

    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> reasons;

    @Column(insertable = false)
    private String status;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Match() {
    }

    public Match(UUID loadId, UUID truckId, BigDecimal score, List<String> reasons) {
        this.loadId = loadId;
        this.truckId = truckId;
        this.score = score;
        this.reasons = reasons;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoadId() {
        return loadId;
    }

    public UUID getTruckId() {
        return truckId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** Moves the match to a decided state, setting all three columns the schema requires together. */
    public void decide(String status, UUID decidedBy) {
        this.status = status;
        this.decidedBy = decidedBy;
        this.decidedAt = OffsetDateTime.now();
    }
}
