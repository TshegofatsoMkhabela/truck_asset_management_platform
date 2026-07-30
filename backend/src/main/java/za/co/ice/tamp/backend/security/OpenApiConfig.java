package za.co.ice.tamp.backend.security;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Registers the JWT (JSON Web Token) as a {@code bearerAuth} security scheme in the generated
 * OpenAPI description, so Swagger UI shows an Authorize button that attaches the token to every
 * subsequent request. This is the whole demo path when no front-end exists: a reviewer logs in
 * once through {@code /auth/login}, applies the returned token, and exercises every protected
 * endpoint from the browser.
 */
@OpenAPIDefinition
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}
