package za.co.ice.tamp.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.persistence.JpaTestBase;

/**
 * Proves the filter chain is deny-by-default with an explicit allow-list, defending against a
 * misconfigured chain that accidentally permits all requests, which would make every
 * {@code @PreAuthorize} check downstream unreachable dead code.
 */
@AutoConfigureMockMvc
class SecurityConfigTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void helloStaysPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void registerAndLoginStayPublic() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unlistedPathRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/audit")).andExpect(status().isUnauthorized());
    }
}
