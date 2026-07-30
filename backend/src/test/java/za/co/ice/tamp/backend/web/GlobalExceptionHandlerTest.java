package za.co.ice.tamp.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

/**
 * Proves every exception the handler is responsible for comes back as the same {@link ApiError}
 * shape with the correct HTTP status, defending against a later feature issue inventing its own
 * error body because this one was inconsistent, or a status code that doesn't match the actual
 * failure (e.g. an unauthenticated request returning 200).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsDocumentedShapeOnValidationFailure() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("returnsDocumentedShapeOnValidationFailure"),
                -1);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.fieldErrors()).containsEntry("email", "must not be blank");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void returnsDocumentedShapeOnAuthenticationFailure() {
        ResponseEntity<ApiError> response =
                handler.handleAuthentication(new BadCredentialsException("bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("UNAUTHENTICATED");
        assertThat(body.fieldErrors()).isEmpty();
    }

    @Test
    void returnsDocumentedShapeOnAccessDenied() {
        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("ACCESS_DENIED");
    }
}
