package za.co.ice.tamp.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Proves registration stores a hashed password (never plaintext) and writes an audit event,
 * defending against a regression that logs or persists the raw password, and against a silent
 * gap in the audit trail from the very first user action.
 */
@AutoConfigureMockMvc
class AuthControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void registerHashesPasswordNotPlaintext() throws Exception {
        String email = "driver-" + System.nanoTime() + "@example.com";
        String body = """
                {"fullName":"Jane Transporter","email":"%s","password":"correct-horse-battery","role":"TRANSPORTER"}
                """.formatted(email);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("TRANSPORTER"));

        var saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("correct-horse-battery");
        assertThat(saved.getPasswordHash()).startsWith("$2");
    }

    @Test
    void registerWritesAuditEvent() throws Exception {
        String email = "audit-" + System.nanoTime() + "@example.com";
        String body = """
                {"fullName":"Owner Person","email":"%s","password":"correct-horse-battery","role":"FREIGHT_OWNER"}
                """.formatted(email);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        var events = auditLogRepository.findByEntityTypeAndEntityId("User", user.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAction()).isEqualTo("REGISTERED");
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        String email = "login-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Login Person","email":"%s","password":"correct-horse-battery","role":"ADMIN"}
                        """.formatted(email)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = "wrongpass-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Wrong Pass","email":"%s","password":"correct-horse-battery","role":"ADMIN"}
                        """.formatted(email)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"not-the-right-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void loginRejectsUnknownEmailWithSameShapeAsWrongPassword() throws Exception {
        String unknownEmail = "never-registered-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"whatever-password"}
                                """.formatted(unknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("invalid email or password"));
    }

    @Test
    void loginWritesAuditEvent() throws Exception {
        String email = "loginaudit-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Login Audit","email":"%s","password":"correct-horse-battery","role":"ADMIN"}
                        """.formatted(email)));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"correct-horse-battery"}
                        """.formatted(email)));

        var user = userRepository.findByEmail(email).orElseThrow();
        var events = auditLogRepository.findByEntityTypeAndEntityId("User", user.getId());
        assertThat(events).extracting(e -> e.getAction()).contains("LOGGED_IN");
    }

    @Test
    void registerRejectsBlankFullNameWithDocumentedShape() throws Exception {
        String body = """
                {"fullName":"","email":"x@example.com","password":"correct-horse-battery","role":"ADMIN"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void registerRejectsDuplicateEmailWithDocumentedShape() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        String body = """
                {"fullName":"First Attempt","email":"%s","password":"correct-horse-battery","role":"ADMIN"}
                """.formatted(email);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}
