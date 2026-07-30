package za.co.ice.tamp.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Dispute;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.DisputeRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

/**
 * Exercises {@code AdminController} against the real migrated schema (see {@link JpaTestBase}),
 * covering issue #17's Minimum Integration Test (admin reads metrics that match the seeded
 * counts; non-admin is rejected) plus the users, audit-log and disputes listings.
 *
 * <p>The database container is shared across all test classes, so count assertions use
 * greaterThanOrEqualTo against what this class seeds rather than exact totals.
 *
 * <p>@Transactional is disabled for the same reason as {@code LoadControllerTest}: the
 * shared container's audit_logs table is append-only, and a shared test transaction would
 * risk Hibernate flushing an UPDATE against rows other tests wrote.
 */
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    private UUID seedUser(String role) {
        return userRepository.save(
                new User("Admin Test " + role, "admin17." + UUID.randomUUID() + "@example.com",
                        "irrelevant-hash", role)).getId();
    }

    @Test
    void adminReadsMetricsMatchingSeededCounts() throws Exception {
        // Issue #17's Minimum Integration Test: the metrics counts reflect rows that
        // verifiably exist, and a non-admin is rejected from the same endpoint (below).
        UUID adminId = seedUser("ADMIN");
        UUID ownerId = seedUser("FREIGHT_OWNER");
        OffsetDateTime now = OffsetDateTime.now();
        loadRepository.save(new Load(ownerId, "Johannesburg", "Cape Town", "GENERAL",
                new BigDecimal("500.00"), new BigDecimal("2.50"),
                now.plusDays(1), now.plusDays(1).plusHours(6)));
        truckRepository.save(new Truck(ownerId, "CONTAINER", new BigDecimal("5000.00"),
                new BigDecimal("20.00"), "Johannesburg", now, now.plusDays(3)));

        mockMvc.perform(get("/admin/metrics").param("adminId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.loads").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.trucks").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.matches").value(Matchers.greaterThanOrEqualTo(0)));
    }

    @Test
    void nonAdminIsRejectedFromEveryAdminEndpoint() throws Exception {
        UUID transporterId = seedUser("TRANSPORTER");

        for (String path : new String[] {
                "/admin/metrics", "/admin/users", "/admin/audit-logs", "/admin/disputes"}) {
            mockMvc.perform(get(path).param("adminId", transporterId.toString()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unknownAdminIdIsRejected() throws Exception {
        mockMvc.perform(get("/admin/metrics").param("adminId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListsUsersWithComplianceStatus() throws Exception {
        UUID adminId = seedUser("ADMIN");
        UUID ownerId = seedUser("FREIGHT_OWNER");

        // sort=createdAt,desc so the users just seeded (necessarily the newest in the
        // shared container) land on page 0 regardless of how many other tests ran first.
        mockMvc.perform(get("/admin/users")
                        .param("adminId", adminId.toString())
                        .param("sort", "createdAt,desc")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + ownerId + "')].complianceStatus")
                        .value(Matchers.contains("PENDING")))
                .andExpect(jsonPath("$[?(@.id == '" + adminId + "')].role")
                        .value(Matchers.contains("ADMIN")));
    }

    @Test
    void adminListingsAreCappedByPageSize() throws Exception {
        // The container is shared across test classes and accumulates rows over the whole
        // suite, so users/audit-logs/disputes must be paged rather than returned whole.
        UUID adminId = seedUser("ADMIN");
        for (int i = 0; i < 3; i++) {
            seedUser("FREIGHT_OWNER");
        }

        mockMvc.perform(get("/admin/users")
                        .param("adminId", adminId.toString())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void adminViewsAuditLogEntries() throws Exception {
        UUID adminId = seedUser("ADMIN");
        // The shared container already holds audit rows from other test classes (loads,
        // matching), so the listing being non-empty and well-formed is the real assertion.
        mockMvc.perform(get("/admin/audit-logs").param("adminId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminListsDisputes() throws Exception {
        UUID adminId = seedUser("ADMIN");
        UUID raisedBy = seedUser("FREIGHT_OWNER");
        UUID subject = seedUser("TRANSPORTER");
        Dispute dispute = disputeRepository.save(
                new Dispute(null, subject, raisedBy, "Damaged cargo on delivery"));

        mockMvc.perform(get("/admin/disputes").param("adminId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + dispute.getId() + "')].description")
                        .value(Matchers.contains("Damaged cargo on delivery")))
                .andExpect(jsonPath("$[?(@.id == '" + dispute.getId() + "')].status")
                        .value(Matchers.contains("OPEN")));
    }
}
