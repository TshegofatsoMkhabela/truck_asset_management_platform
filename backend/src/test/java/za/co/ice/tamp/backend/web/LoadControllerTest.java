package za.co.ice.tamp.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.web.dto.LoadResponse;

/**
 * Exercises {@code LoadController} against the real migrated schema (see {@link JpaTestBase}),
 * covering the issue's own Minimum Integration Test (create then fetch the owner's list) plus
 * the 404 case.
 *
 * Each test is isolated by disabling {@link JpaTestBase}'s inherited @Transactional so MockMvc
 * operations don't share a persistence context. This is necessary because audit_logs is
 * append-only, and Hibernate would try to UPDATE the AuditLog when flushing at test end.
 */
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LoadControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    /** loads.owner_id is a foreign key to users, so a real seeded user is required first. */
    private UUID seedOwner() {
        User owner = userRepository.save(
                new User("Jane Owner", "jane.owner." + UUID.randomUUID() + "@example.com",
                        "irrelevant-hash", "FREIGHT_OWNER"));
        return owner.getId();
    }

    private String createBody(UUID ownerId) {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        return """
                {
                  "ownerId": "%s",
                  "originCity": "Johannesburg",
                  "destinationCity": "Cape Town",
                  "cargoType": "GENERAL",
                  "weightKg": 500.00,
                  "volumeM3": 2.50,
                  "pickupWindowStart": "%s",
                  "pickupWindowEnd": "%s"
                }
                """.formatted(ownerId, start, start.plusHours(6));
    }

    @Test
    void createsAndListsLoadForItsOwner() throws Exception {
        // The issue's own Minimum Integration Test: create a load, then fetch the owner's
        // list of loads, and confirm it appears there.
        UUID ownerId = seedOwner();

        MvcResult createResult = mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originCity").value("Johannesburg"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        LoadResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), LoadResponse.class);

        mockMvc.perform(get("/loads").param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(created.id().toString()))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void getByIdReturnsTheCreatedLoad() throws Exception {
        UUID ownerId = seedOwner();
        MvcResult createResult = mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(ownerId)))
                .andReturn();

        LoadResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), LoadResponse.class);

        mockMvc.perform(get("/loads/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()));
    }

    @Test
    void updatesStatusOnlyWithoutResendingFields() throws Exception {
        UUID ownerId = seedOwner();
        MvcResult createResult = mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(ownerId)))
                .andReturn();

        LoadResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), LoadResponse.class);

        mockMvc.perform(patch("/loads/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "MATCHED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.originCity").value("Johannesburg"));
    }

    @Test
    void postingALoadWritesAnAuditEvent() throws Exception {
        UUID ownerId = seedOwner();

        MvcResult createResult = mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(ownerId)))
                .andReturn();

        LoadResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), LoadResponse.class);

        List<AuditLog> events = auditLogRepository.findByEntityTypeAndEntityId("load", created.id());

        assertThat(events).hasSize(1);
        AuditLog event = events.get(0);
        assertThat(event.getActorId()).isEqualTo(ownerId);
        assertThat(event.getAction()).isEqualTo("LOAD_POSTED");
    }

    @Test
    void returns404ForUnknownLoadId() throws Exception {
        mockMvc.perform(get("/loads/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
