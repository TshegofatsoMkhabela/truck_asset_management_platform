package za.co.ice.tamp.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * The single test issue #18 exists to produce: the whole eight-step journey (register, post a
 * load, post a truck, get a match, accept, receive a receipt, track the trip, rate the
 * counterparty) run continuously over real HTTP, closing with an admin view of what just
 * happened, using the real matching-service round trip rather than a persisted-directly match
 * (see {@link za.co.ice.tamp.backend.MatchFixture}, which every other controller test uses
 * specifically to skip this step).
 *
 * <p>Tagged {@code e2e} for the same reason {@code MatchingTimingE2ETest} is: this test needs a
 * real matching-service process reachable at {@code MATCHING_SERVICE_URL} (default
 * {@code http://localhost:8000}), which {@code mvn test} does not guarantee. Run with
 * {@code mvn verify -Pe2e} against a stack already started by {@code docker compose up}.
 *
 * <p>Registers its own users and posts its own load/truck rather than reusing
 * {@code db/seed/dev-seed.sql}'s pre-completed match, since the point of this test is to prove
 * the journey from a cold start, not to re-read a match that was already accepted before the
 * test ran.
 */
@Tag("e2e")
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DemoJourneyE2ETest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void walksTheFullEightStepJourneyOverRealHttp() throws Exception {
        String suffix = UUID.randomUUID().toString();

        // Step 1: register. Two parties plus an admin, so the journey's closing step has
        // someone to log the audit trail's actions against.
        UUID ownerId = registerUser(
                "Demo Owner", "owner." + suffix + "@e2e.test", "FREIGHT_OWNER");
        UUID transporterId = registerUser(
                "Demo Transporter", "transporter." + suffix + "@e2e.test", "TRANSPORTER");
        UUID adminId = registerUser(
                "Demo Admin", "admin." + suffix + "@e2e.test", "ADMIN");

        // Step 2: post a load.
        String fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now().plusDays(1));
        String fmtEnd = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now().plusDays(3));
        MvcResult loadResult = mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerId": "%s",
                                  "originCity": "Johannesburg",
                                  "destinationCity": "Durban",
                                  "cargoType": "GENERAL",
                                  "weightKg": 8000,
                                  "volumeM3": 18,
                                  "pickupWindowStart": "%s",
                                  "pickupWindowEnd": "%s"
                                }
                                """.formatted(ownerId, fmt, fmtEnd)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID loadId = UUID.fromString(readField(loadResult, "id"));

        // Step 3: post a truck eligible for that load: same city, sufficient capacity,
        // compatible cargo type, an overlapping availability window.
        mockMvc.perform(post("/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transporterId": "%s",
                                  "vehicleType": "FLATBED",
                                  "capacityKg": 10000,
                                  "capacityM3": 25,
                                  "currentCity": "Johannesburg",
                                  "availableFrom": "%s",
                                  "availableUntil": "%s"
                                }
                                """.formatted(transporterId, fmt, fmtEnd)))
                .andExpect(status().isCreated());

        // Step 4: get a match. Real HTTP call to matching-service, not a persisted-directly
        // Match entity: this is the one step in the whole journey that proves the two services
        // actually talk to each other, not just that each accepts requests in isolation.
        MvcResult matchResult = mockMvc.perform(post("/loads/{loadId}/matches", loadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestedBy": "%s"}
                                """.formatted(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andReturn();
        UUID matchId = UUID.fromString(objectMapper
                .readTree(matchResult.getResponse().getContentAsString())
                .get(0).get("id").asText());

        // Step 5: accept.
        mockMvc.perform(post("/matches/{matchId}/decision", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision": "ACCEPTED", "actorId": "%s"}
                                """.formatted(transporterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Step 6: fetch the receipt.
        mockMvc.perform(get("/matches/{matchId}/receipt", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(org.hamcrest.Matchers.startsWith("TAMP-")));

        // Step 7: track the trip, then read it back oldest-first.
        mockMvc.perform(post("/matches/{matchId}/tracking", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IN_TRANSIT"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/matches/{matchId}/tracking", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DELIVERED"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/matches/{matchId}/tracking", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$[1].status").value("DELIVERED"));

        // Step 8: rate the counterparty.
        mockMvc.perform(post("/matches/{matchId}/ratings", matchId)
                        .param("raterId", transporterId.toString())
                        .param("rateeId", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 5, "comment": "Smooth pickup, on time."}
                                """))
                .andExpect(status().isCreated());

        // Step 9: the admin view of everything that just happened.
        mockMvc.perform(get("/admin/metrics").param("adminId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/admin/audit-logs").param("adminId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private UUID registerUser(String fullName, String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "email": "%s",
                                  "password": "DemoPass2026!",
                                  "role": "%s"
                                }
                                """.formatted(fullName, email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readField(result, "id"));
    }

    private String readField(MvcResult result, String field) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get(field).asText();
    }
}
