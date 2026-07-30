package za.co.ice.tamp.backend.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.User;

/**
 * The body returned for every user-facing endpoint in this issue.
 *
 * <p>Never includes {@code passwordHash}: the entity carries it, but Jackson (the JSON
 * library Spring Boot wires in by default) would serialise it straight into the response if
 * the entity itself were ever returned directly. Keeping this as its own class is what
 * prevents that leak regardless of what fields the entity gains later.
 */
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        String complianceStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getComplianceStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
