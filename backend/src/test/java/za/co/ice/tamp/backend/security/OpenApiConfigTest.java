package za.co.ice.tamp.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.persistence.JpaTestBase;

/**
 * Proves the generated OpenAPI description is reachable without a token, lists the auth
 * endpoints, and registers the JWT bearer scheme, defending against springdoc misconfiguration
 * silently excluding the security package, which would leave Swagger UI showing no Authorize
 * button and no way for a reviewer to exercise a protected endpoint from the browser.
 */
@AutoConfigureMockMvc
class OpenApiConfigTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAreReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths./auth/register").exists())
                .andExpect(jsonPath("$.paths./auth/login").exists())
                .andExpect(jsonPath("$.paths./audit").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void swaggerUiPageIsReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
