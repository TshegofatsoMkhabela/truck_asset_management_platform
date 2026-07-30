package za.co.ice.tamp.backend.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Deny-by-default: only the routes named here are reachable without a valid JWT (JSON Web
 * Token). Everything else requires the {@link JwtAuthenticationFilter} to have populated the
 * security context, and role-restricted endpoints layer {@code @PreAuthorize} on top of that.
 * Deny-by-default rather than allow-by-default with per-endpoint annotations, because a
 * forgotten annotation on a new endpoint would otherwise silently ship unauthenticated.
 *
 * <p>The {@code PasswordEncoder} bean lives in {@link PasswordEncoderConfig} (#10), not here:
 * #10 introduced it first so user creation never stored a plaintext password while this class
 * was still being built, on the explicit understanding that #9 would reuse it rather than
 * define a second one.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * {@code JwtAuthenticationFilter} is a {@code @Component} so it can be constructor-injected
     * here like the other two Security-layer beans, but Spring Boot auto-registers every
     * {@code Filter} bean as a servlet-container-level filter by default, which would run it a
     * second time outside the chain built below. This registration is disabled so
     * {@code addFilterBefore} in {@link #filterChain} is the only place it actually runs.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health", "/", "/auth/register", "/auth/login", "/users", "/users/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
