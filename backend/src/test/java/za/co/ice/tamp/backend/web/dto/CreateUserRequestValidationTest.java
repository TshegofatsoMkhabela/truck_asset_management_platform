package za.co.ice.tamp.backend.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Proves malformed input is caught by bean validation before it can reach the database,
 * defending against a blank name or unparseable email surfacing only as an opaque
 * constraint-violation stack trace from Postgres instead of a client-readable 400.
 */
class CreateUserRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    void rejectsBlankFullName() {
        CreateUserRequest request = new CreateUserRequest("  ", "owner@example.com", "s3cret-pass", "FREIGHT_OWNER");

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void rejectsInvalidEmail() {
        CreateUserRequest request = new CreateUserRequest("Jane Owner", "not-an-email", "s3cret-pass", "FREIGHT_OWNER");

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void acceptsValidRequest() {
        CreateUserRequest request = new CreateUserRequest("Jane Owner", "owner@example.com", "s3cret-pass", "FREIGHT_OWNER");

        Set<ConstraintViolation<CreateUserRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).isEmpty();
    }
}
