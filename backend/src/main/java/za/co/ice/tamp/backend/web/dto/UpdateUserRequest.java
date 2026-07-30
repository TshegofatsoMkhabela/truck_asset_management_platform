package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.Pattern;

/**
 * The body of {@code PATCH /users/{id}}.
 *
 * <p>Every field is optional and nullable: a {@code PATCH} only overwrites what the caller
 * actually sends, so setting the compliance status alone does not require resending
 * {@code fullName} or {@code email}. {@code password} is intentionally absent; #9 (RBAC and
 * auth) owns credential changes, and this issue's scope is profile and compliance data only.
 */
public record UpdateUserRequest(
        String fullName,
        @Pattern(regexp = "PENDING|APPROVED|REJECTED") String complianceStatus
) {
}
