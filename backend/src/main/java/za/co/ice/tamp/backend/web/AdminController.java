package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.DisputeRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.security.CurrentUser;
import za.co.ice.tamp.backend.web.dto.AuditLogResponse;
import za.co.ice.tamp.backend.web.dto.DisputeResponse;
import za.co.ice.tamp.backend.web.dto.MetricsResponse;
import za.co.ice.tamp.backend.web.dto.UserResponse;

/**
 * Admin oversight: users with compliance status, the audit trail, flagged/disputed items,
 * and basic platform counts. Read-only by design — the issue scopes out administrative
 * action-taking (suspension, dispute resolution).
 *
 * <p>Role enforcement is a service-layer check: each endpoint takes an explicit
 * {@code adminId} and refuses anyone whose stored role is not ADMIN, rather than reading the
 * authenticated principal that #9's JWT would provide. See known-limitations.md: #9 built
 * login and token issuance but never wired identity checks into the controllers built before
 * it, this one included. The seam is the same explicit-ID precedent as #10 to #15, but unlike
 * those issues the rejection itself is enforced here, because "non-admin users are rejected"
 * is this issue's acceptance criterion.
 */
@RestController
public class AdminController {

    private final UserRepository userRepository;
    private final LoadRepository loadRepository;
    private final TruckRepository truckRepository;
    private final MatchRepository matchRepository;
    private final AuditLogRepository auditLogRepository;
    private final DisputeRepository disputeRepository;

    public AdminController(UserRepository userRepository, LoadRepository loadRepository,
            TruckRepository truckRepository, MatchRepository matchRepository,
            AuditLogRepository auditLogRepository, DisputeRepository disputeRepository) {
        this.userRepository = userRepository;
        this.loadRepository = loadRepository;
        this.truckRepository = truckRepository;
        this.matchRepository = matchRepository;
        this.auditLogRepository = auditLogRepository;
        this.disputeRepository = disputeRepository;
    }

    @GetMapping("/admin/metrics")
    @Operation(summary = "Basic platform counts: users, loads, trucks, matches",
            description = "Required role: ADMIN. adminId is a caller-supplied id, not read from "
                    + "an authenticated principal (see known-limitations.md). "
                    + "Use the seeded admin user's id from the dev seed to try this from Swagger UI.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Counts retrieved",
                content = @Content(schema = @Schema(implementation = MetricsResponse.class))),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public MetricsResponse metrics(
            @Parameter(description = "Id of a user holding the ADMIN role", required = true)
            @RequestParam(required = false) UUID adminId, Authentication authentication) {
        requireAdmin(CurrentUser.requireIdOrFallback(authentication, adminId, "adminId"));
        return new MetricsResponse(
                userRepository.count(),
                loadRepository.count(),
                truckRepository.count(),
                matchRepository.count());
    }

    @GetMapping("/admin/users")
    @Operation(summary = "List every user with their role and compliance status",
            description = "Required role: ADMIN. adminId is a caller-supplied id, not read from an "
                    + "authenticated principal (see known-limitations.md).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<UserResponse> users(@RequestParam(required = false) UUID adminId, Authentication authentication) {
        requireAdmin(CurrentUser.requireIdOrFallback(authentication, adminId, "adminId"));
        return userRepository.findAll(Sort.by("createdAt")).stream()
                .map(UserResponse::from).toList();
    }

    @GetMapping("/admin/audit-logs")
    @Operation(summary = "View the audit trail, newest entry first",
            description = "Required role: ADMIN. adminId is a caller-supplied id, not read from an "
                    + "authenticated principal (see known-limitations.md).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit entries retrieved (may be empty)"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<AuditLogResponse> auditLogs(@RequestParam(required = false) UUID adminId, Authentication authentication) {
        requireAdmin(CurrentUser.requireIdOrFallback(authentication, adminId, "adminId"));
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt")).stream()
                .map(AuditLogResponse::from).toList();
    }

    @GetMapping("/admin/disputes")
    @Operation(summary = "List flagged and disputed items",
            description = "Required role: ADMIN. adminId is a caller-supplied id, not read from an "
                    + "authenticated principal (see known-limitations.md). "
                    + "Read-only oversight; resolution actions are out of this issue's scope.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disputes retrieved (may be empty)"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<DisputeResponse> disputes(@RequestParam(required = false) UUID adminId, Authentication authentication) {
        requireAdmin(CurrentUser.requireIdOrFallback(authentication, adminId, "adminId"));
        return disputeRepository.findAll(Sort.by("createdAt")).stream()
                .map(DisputeResponse::from).toList();
    }

    /**
     * An unknown id and a non-admin id both return the same 403: distinguishing them would
     * tell an unauthenticated caller which UUIDs correspond to real users.
     */
    private void requireAdmin(UUID adminId) {
        boolean isAdmin = userRepository.findById(adminId)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
        if (!isAdmin) {
            throw new NotAdminException();
        }
    }

    @ExceptionHandler(NotAdminException.class)
    public ResponseEntity<String> handleNotAdmin(NotAdminException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
