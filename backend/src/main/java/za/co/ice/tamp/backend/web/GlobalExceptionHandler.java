package za.co.ice.tamp.backend.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import za.co.ice.tamp.backend.security.EmailAlreadyRegisteredException;

/**
 * The single place every endpoint's failures are converted to {@link ApiError}, so the eight
 * feature issues that follow #9 do not each invent their own error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more fields failed validation", fieldErrors);
    }

    /**
     * Covers {@link za.co.ice.tamp.backend.security.CurrentUser#requireIdOrFallback}: thrown
     * when a request has neither a JWT nor the caller-supplied id field it stands in for.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        return respond(HttpStatus.valueOf(exception.getStatusCode().value()),
                "BAD_REQUEST", exception.getReason());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception) {
        return respond(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
        return respond(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return respond(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", exception.getMessage());
    }

    /**
     * Absorbed from {@code UserController} (#10), whose own local handler was explicitly written
     * as a placeholder "until #9's @ControllerAdvice lands, since that handler will cover this
     * same exception type globally."
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException exception) {
        return respond(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage());
    }

    /**
     * Defense in depth against the race window between {@code AuthController}'s existence check
     * and its insert: two concurrent registrations for the same email could both pass the check
     * before either commits. The database's unique constraint is the real guarantee; this only
     * ensures whichever request loses that race still gets the documented shape instead of a raw
     * SQL constraint name reaching the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return respond(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data");
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, error, message));
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String error, String message,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(ApiError.of(status, error, message, fieldErrors));
    }
}
