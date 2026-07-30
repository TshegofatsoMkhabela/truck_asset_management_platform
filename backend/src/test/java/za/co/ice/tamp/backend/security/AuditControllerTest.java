package za.co.ice.tamp.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.persistence.JpaTestBase;

/**
 * Proves the acceptance criterion directly: an endpoint restricted to Admin rejects requests
 * from a non-admin token, allows an admin token, and rejects a missing token, each with the
 * documented error shape. Defends against a role check that only distinguishes
 * authenticated-vs-not without checking the specific role, which would let any logged-in user
 * read the audit trail.
 */
@AutoConfigureMockMvc
class AuditControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndLogin(String role) throws Exception {
        String email = "audit-" + role + "-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test User","email":"%s","password":"correct-horse-battery","role":"%s"}
                        """.formatted(email, role)));

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(loginResponse);
        return node.get("token").asText();
    }

    @Test
    void adminEndpointAllowsAdminToken() throws Exception {
        String token = registerAndLogin("ADMIN");

        mockMvc.perform(get("/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointRejectsNonAdminToken() throws Exception {
        String token = registerAndLogin("TRANSPORTER");

        mockMvc.perform(get("/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void adminEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }
}
