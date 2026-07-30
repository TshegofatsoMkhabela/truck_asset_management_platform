package za.co.ice.tamp.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * The 403 writer: see {@link ApiErrorWriter} for why this exists as a separate component from
 * {@code GlobalExceptionHandler}.
 */
@Component
public class RestAccessDeniedHandler extends ApiErrorWriter implements AccessDeniedHandler {

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to access this resource");
    }
}
