package za.co.ice.tamp.backend.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The registration request body. {@code role} is restricted to the three values the
 * {@code users.role} check constraint (db/migrations/V01__users.sql) accepts, so a bad role is
 * rejected here with the documented validation error shape rather than as a raw database
 * constraint violation.
 */
public record RegisterRequest(
        @NotBlank(message = "full name must not be blank") String fullName,
        @NotBlank(message = "email must not be blank") @Email(message = "email must be valid") String email,
        @NotBlank(message = "password must not be blank") @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotBlank(message = "role must not be blank") @Pattern(regexp = "FREIGHT_OWNER|TRANSPORTER|ADMIN", message = "role must be one of FREIGHT_OWNER, TRANSPORTER, ADMIN") String role) {
}
