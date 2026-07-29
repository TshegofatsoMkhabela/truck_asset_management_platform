package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Maps {@code disputes} (db/migrations/V09__disputes.sql). */
@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    /** Set when the dispute is about a match; null when it is a direct flag on a user. */
    @Column(name = "match_id")
    private UUID matchId;

    /** Set when the dispute is a direct flag on a user; null when it is about a match. */
    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "raised_by")
    private UUID raisedBy;

    private String description;

    @Column(insertable = false)
    private String status;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected Dispute() {
    }

    public Dispute(UUID matchId, UUID subjectUserId, UUID raisedBy, String description) {
        this.matchId = matchId;
        this.subjectUserId = subjectUserId;
        this.raisedBy = raisedBy;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getSubjectUserId() {
        return subjectUserId;
    }

    public UUID getRaisedBy() {
        return raisedBy;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    /** Moves the dispute to a resolved state, setting all three columns the schema requires together. */
    public void resolve(String status, UUID resolvedBy, String resolutionNote) {
        this.status = status;
        this.resolvedBy = resolvedBy;
        this.resolutionNote = resolutionNote;
        this.resolvedAt = OffsetDateTime.now();
    }
}
