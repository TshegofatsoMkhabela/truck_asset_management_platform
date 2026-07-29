package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import za.co.ice.tamp.backend.persistence.InetAddressConverter;

/** Maps {@code receipts} (db/migrations/V05__receipts.sql). */
@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "contract_id", insertable = false, updatable = false)
    private String contractId;

    @Column(name = "match_id")
    private UUID matchId;

    private String decision;

    @Column(name = "actor_id")
    private UUID actorId;

    /**
     * Nullable: brief section 2.2 asks for IP/user-agent "where available". See
     * {@link InetAddressConverter} for why both the converter and {@code @JdbcTypeCode} are
     * needed together, and {@code columnDefinition} for why schema validation needs the DDL
     * string pinned rather than inferred.
     */
    @Column(name = "ip_address", columnDefinition = "inet")
    @Convert(converter = InetAddressConverter.class)
    @JdbcTypeCode(SqlTypes.OTHER)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private OffsetDateTime issuedAt;

    protected Receipt() {
    }

    public Receipt(UUID matchId, String decision, UUID actorId, String ipAddress, String userAgent) {
        this.matchId = matchId;
        this.decision = decision;
        this.actorId = actorId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public String getDecision() {
        return decision;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }
}
