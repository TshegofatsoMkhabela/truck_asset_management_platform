package za.co.ice.tamp.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.web.dto.UserResponse;

/**
 * Exercises {@code UserController} against the real migrated schema (see {@link JpaTestBase}),
 * covering the issue's own Minimum Integration Test (create then fetch) plus the two edge
 * cases most likely to silently misbehave: an unknown id and a partial update.
 */
@AutoConfigureMockMvc
class UserControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsAndFetchesUser() throws Exception {
        String requestBody = """
                {
                  "fullName": "Jane Owner",
                  "email": "jane.owner@example.com",
                  "password": "s3cret-pass",
                  "role": "FREIGHT_OWNER"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Jane Owner"))
                .andExpect(jsonPath("$.email").value("jane.owner@example.com"))
                .andExpect(jsonPath("$.complianceStatus").value("PENDING"))
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        // Proves the stored hash is never the raw password, defending against a copy-paste
        // of the DTO field name straight into the entity constructor.
        String storedHash = userRepository.findById(created.id()).orElseThrow().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("s3cret-pass");

        mockMvc.perform(get("/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Owner"));
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitively() throws Exception {
        String body = """
                {
                  "fullName": "First Owner",
                  "email": "duplicate@example.com",
                  "password": "s3cret-pass",
                  "role": "FREIGHT_OWNER"
                }
                """;
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        String secondBody = """
                {
                  "fullName": "Second Owner",
                  "email": "DUPLICATE@example.com",
                  "password": "another-pass",
                  "role": "FREIGHT_OWNER"
                }
                """;
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(secondBody))
                .andExpect(status().isConflict());
    }

    @Test
    void returns404ForUnknownUserId() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesComplianceStatusOnlyWithoutResendingProfile() throws Exception {
        String createBody = """
                {
                  "fullName": "Amos Transporter",
                  "email": "amos.transporter@example.com",
                  "password": "s3cret-pass",
                  "role": "TRANSPORTER"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        mockMvc.perform(patch("/users/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "complianceStatus": "APPROVED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complianceStatus").value("APPROVED"))
                .andExpect(jsonPath("$.fullName").value("Amos Transporter"));
    }
}
