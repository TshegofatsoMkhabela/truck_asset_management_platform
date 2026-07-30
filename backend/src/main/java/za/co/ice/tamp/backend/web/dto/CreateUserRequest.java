package za.co.ice.tamp.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The body of {@code POST /users}.
 *
 * <p>Deliberately separate from the {@code User} entity: this shape is what a client sends,
 * not what is stored. {@code role} is validated here against the same three values the
 * database's {@code CHECK} constraint enforces, so a bad role is rejected with a 400 before it
 * reaches Postgres, rather than surfacing as a constraint-violation stack trace.
 */
public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank @Pattern(regexp = "FREIGHT_OWNER|TRANSPORTER|ADMIN") String role
) {
}
