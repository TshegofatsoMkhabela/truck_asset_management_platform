package za.co.ice.tamp.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Maps {@code ratings} (db/migrations/V06__ratings.sql). */
@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "match_id")
    private UUID matchId;

    @Column(name = "rater_id")
    private UUID raterId;

    @Column(name = "ratee_id")
    private UUID rateeId;

    private Short score;

    private String comment;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Rating() {
    }

    public Rating(UUID matchId, UUID raterId, UUID rateeId, Short score, String comment) {
        this.matchId = matchId;
        this.raterId = raterId;
        this.rateeId = rateeId;
        this.score = score;
        this.comment = comment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getRaterId() {
        return raterId;
    }

    public UUID getRateeId() {
        return rateeId;
    }

    public Short getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }
}
