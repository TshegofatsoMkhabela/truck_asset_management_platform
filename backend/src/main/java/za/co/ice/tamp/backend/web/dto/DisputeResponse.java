package za.co.ice.tamp.backend.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.Dispute;

/** One flagged/disputed item as returned to an admin (FR-10, issue #17). */
public record DisputeResponse(
        UUID id,
        UUID matchId,
        UUID subjectUserId,
        UUID raisedBy,
        String description,
        String status,
        String resolutionNote,
        UUID resolvedBy,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt) {

    public static DisputeResponse from(Dispute dispute) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getMatchId(),
                dispute.getSubjectUserId(),
                dispute.getRaisedBy(),
                dispute.getDescription(),
                dispute.getStatus(),
                dispute.getResolutionNote(),
                dispute.getResolvedBy(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt());
    }
}
