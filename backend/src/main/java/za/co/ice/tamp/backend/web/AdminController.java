package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import za.co.ice.tamp.backend.web.dto.AuditLogResponse;
import za.co.ice.tamp.backend.web.dto.DisputeResponse;
import za.co.ice.tamp.backend.web.dto.MetricsResponse;
import za.co.ice.tamp.backend.web.dto.UserResponse;

/**
 * Admin oversight: users with compliance status, the audit trail, flagged/disputed items,
 * and basic platform counts (FR-10, FR-11, FR-12, issue #17). Read-only by design — the
 * issue scopes out administrative action-taking (suspension, dispute resolution).
 *
 * <p>Role enforcement is a temporary service-layer check: each endpoint takes an explicit
 * {@code adminId} and refuses anyone whose stored role is not ADMIN. #9 (auth/RBAC) owns
 * real authentication and has not merged; when it lands, the parameter disappears and the
 * check reads the authenticated principal instead. The seam is the same explicit-ID
 * precedent as #10–#15, but unlike those issues the rejection itself is enforced here,
 * because "non-admin users are rejected" is this issue's acceptance criterion.
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
            description = "Required role: ADMIN. adminId is temporary (seam until #9 auth/RBAC "
                    + "merges); the role will be read from the authenticated principal instead. "
                    + "Use the seeded admin user's id from the dev seed to try this from Swagger UI.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Counts retrieved",
                content = @Content(schema = @Schema(implementation = MetricsResponse.class))),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public MetricsResponse metrics(
            @Parameter(description = "Id of a user holding the ADMIN role", required = true)
            @RequestParam UUID adminId) {
        requireAdmin(adminId);
        return new MetricsResponse(
                userRepository.count(),
                loadRepository.count(),
                truckRepository.count(),
                matchRepository.count());
    }

    @GetMapping("/admin/users")
    @Operation(summary = "List every user with their role and compliance status, paginated",
            description = "Required role: ADMIN. adminId is temporary (seam until #9 auth/RBAC merges). "
                    + "page and size are standard Spring pagination params (default page=0, size=20, capped at 100).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<UserResponse> users(@RequestParam UUID adminId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        requireAdmin(adminId);
        return userRepository.findAll(cap(pageable)).map(UserResponse::from).toList();
    }

    @GetMapping("/admin/audit-logs")
    @Operation(summary = "View the audit trail, newest entry first, paginated",
            description = "Required role: ADMIN. adminId is temporary (seam until #9 auth/RBAC merges). "
                    + "page and size are standard Spring pagination params (default page=0, size=20, capped at 100); "
                    + "audit_logs is append-only and grows without bound, so an unpaged read is never safe.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit entries retrieved (may be empty)"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<AuditLogResponse> auditLogs(@RequestParam UUID adminId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(adminId);
        return auditLogRepository.findAll(cap(pageable)).map(AuditLogResponse::from).toList();
    }

    @GetMapping("/admin/disputes")
    @Operation(summary = "List flagged and disputed items, paginated",
            description = "Required role: ADMIN. adminId is temporary (seam until #9 auth/RBAC merges). "
                    + "Read-only oversight; resolution actions are out of this issue's scope. "
                    + "page and size are standard Spring pagination params (default page=0, size=20, capped at 100).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disputes retrieved (may be empty)"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN")
    })
    public List<DisputeResponse> disputes(@RequestParam UUID adminId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        requireAdmin(adminId);
        return disputeRepository.findAll(cap(pageable)).map(DisputeResponse::from).toList();
    }

    /**
     * Caps the page size at 100 regardless of what the caller requests, so a page=0&size=100000
     * query can't defeat the pagination this fix exists to enforce.
     */
    private Pageable cap(Pageable pageable) {
        int boundedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), boundedSize, pageable.getSort());
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
