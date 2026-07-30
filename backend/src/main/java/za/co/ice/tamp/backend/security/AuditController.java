package za.co.ice.tamp.backend.security;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;

/**
 * Backs FR-12 (key actions available in an audit trail) and the Administration domain's "audit
 * events" requirement: an Admin can retrieve the trail written from registration and login
 * onward. Restricted to Admin, since the trail can include actions by every user.
 */
@RestController
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogResponse> listAuditEvents() {
        return auditLogRepository.findAll().stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
