package za.co.ice.tamp.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the JWT (JSON Web Token) from the {@code Authorization: Bearer <token>} header and, when
 * valid, populates Spring Security's context with the caller's id and role so downstream
 * {@code @PreAuthorize} checks have something to evaluate. Invalid or absent tokens leave the
 * context empty rather than throwing, letting the {@code SecurityFilterChain}'s own
 * authorization rules decide whether the request may proceed anonymously (e.g. {@code /health})
 * or must be rejected.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtService.JwtClaims claims = jwtService.parseToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.userId().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException invalidToken) {
                // JwtException: malformed, expired, or bad-signature token. IllegalArgumentException:
                // a structurally valid, correctly signed token whose subject claim isn't a UUID.
                // Anything else (e.g. a misconfigured JwtService) is a real bug and should propagate
                // rather than be reported to the caller as an ordinary unauthenticated request.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
