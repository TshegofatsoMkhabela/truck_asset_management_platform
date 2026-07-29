package za.co.ice.tamp.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.persistence.JpaTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves {@code /health} answers 200 with a status payload.
 *
 * <p>Defends against a health endpoint that is misrouted or errors: an orchestrator
 * polling a broken probe restart-loops a service that is actually serving traffic
 * perfectly well, which is far worse than having no probe at all.
 *
 * <p>Extends {@code JpaTestBase} rather than a bare {@code @SpringBootTest}: once the
 * application had a real datasource (#25), every full-context test needs one to boot, even
 * one that touches no persistence code itself.
 */
@AutoConfigureMockMvc
class HealthControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsHealthy() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("backend"));
    }
}
