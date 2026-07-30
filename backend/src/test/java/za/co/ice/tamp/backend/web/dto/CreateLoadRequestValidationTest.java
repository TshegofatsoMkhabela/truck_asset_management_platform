package za.co.ice.tamp.backend.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves malformed load input is caught by bean validation before it can reach the
 * database, defending against a blank city, non-positive weight/volume, or an unknown cargo
 * type surfacing only as an opaque constraint-violation stack trace from Postgres instead of
 * a client-readable 400.
 */
class CreateLoadRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private static CreateLoadRequest validRequest() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        return new CreateLoadRequest(
                UUID.randomUUID(), "Johannesburg", "Cape Town", "GENERAL",
                new BigDecimal("500.00"), new BigDecimal("2.50"),
                start, start.plusHours(6));
    }

    @Test
    void rejectsNonPositiveWeight() {
        CreateLoadRequest request = withWeight(validRequest(), new BigDecimal("0"));

        Set<ConstraintViolation<CreateLoadRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("weightKg"));
    }

    @Test
    void rejectsBlankOriginCity() {
        CreateLoadRequest r = validRequest();
        CreateLoadRequest request = new CreateLoadRequest(
                r.ownerId(), "  ", r.destinationCity(), r.cargoType(),
                r.weightKg(), r.volumeM3(), r.pickupWindowStart(), r.pickupWindowEnd());

        Set<ConstraintViolation<CreateLoadRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("originCity"));
    }

    @Test
    void rejectsUnknownCargoType() {
        CreateLoadRequest r = validRequest();
        CreateLoadRequest request = new CreateLoadRequest(
                r.ownerId(), r.originCity(), r.destinationCity(), "EXPLOSIVE",
                r.weightKg(), r.volumeM3(), r.pickupWindowStart(), r.pickupWindowEnd());

        Set<ConstraintViolation<CreateLoadRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cargoType"));
    }

    @Test
    void acceptsValidRequest() {
        Set<ConstraintViolation<CreateLoadRequest>> violations = VALIDATOR.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    private static CreateLoadRequest withWeight(CreateLoadRequest r, BigDecimal weight) {
        return new CreateLoadRequest(
                r.ownerId(), r.originCity(), r.destinationCity(), r.cargoType(),
                weight, r.volumeM3(), r.pickupWindowStart(), r.pickupWindowEnd());
    }
}
