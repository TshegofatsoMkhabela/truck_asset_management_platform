package za.co.ice.tamp.backend.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Rating;

public record RatingResponse(
        UUID id,
        UUID matchId,
        UUID raterId,
        UUID rateeId,
        Short score,
        String comment,
        OffsetDateTime createdAt
) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getMatchId(),
                rating.getRaterId(),
                rating.getRateeId(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt());
    }
}
