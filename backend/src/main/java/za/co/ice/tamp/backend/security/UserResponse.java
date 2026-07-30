package za.co.ice.tamp.backend.security;

import java.util.UUID;
import za.co.ice.tamp.backend.persistence.entity.User;

/** What a client is shown after registration or in the login response: never the password hash. */
public record UserResponse(UUID id, String fullName, String email, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
