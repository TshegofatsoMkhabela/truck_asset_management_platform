package za.co.ice.tamp.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import za.co.ice.tamp.backend.web.ApiError;

/**
 * Shared by {@link RestAuthenticationEntryPoint} and {@link RestAccessDeniedHandler}: both write
 * the same {@link ApiError} shape {@code GlobalExceptionHandler} uses, but from Spring Security's
 * filter chain, which runs before {@code DispatcherServlet} and so cannot be intercepted by
 * {@code @RestControllerAdvice}. This is the one place that duplication is collapsed to.
 */
abstract class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    protected ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected void writeError(HttpServletResponse response, HttpStatus status, String error,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, error, message));
    }
}
