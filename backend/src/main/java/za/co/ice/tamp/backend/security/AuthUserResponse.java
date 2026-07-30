package za.co.ice.tamp.backend.security;

import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.User;

/**
 * What a client is shown after registration or in the login response: never the password
 * hash. Deliberately narrower than {@code web.dto.UserResponse} (no complianceStatus or
 * timestamps) and named distinctly from it so the two are never confused at a glance.
 */
public record AuthUserResponse(UUID id, String fullName, String email, String role) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
