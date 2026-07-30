package za.co.ice.tamp.backend.web;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.security.CurrentUser;
import za.co.ice.tamp.backend.web.dto.CreateTruckRequest;
import za.co.ice.tamp.backend.web.dto.TruckResponse;
import za.co.ice.tamp.backend.web.dto.UpdateTruckRequest;

/**
 * CRUD for available trucks: a Transporter can create and view available trucks.
 *
 * <p>Talks directly to {@link TruckRepository} with no intervening service class, matching
 * #11's precedent for plain CRUD with a not-found check.
 *
 * <p>Deliberately unauthenticated: #9 (RBAC, role-based access control, and auth) built login
 * and JWT issuance but never wired role checks into this controller. {@code transporterId} is
 * accepted as an explicit field/query parameter rather than read from an authenticated
 * principal for the same reason; see known-limitations.md.
 */
@RestController
public class TruckController {

    private final TruckRepository truckRepository;
    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    public TruckController(TruckRepository truckRepository, AuditLogRepository auditLogRepository,
            EntityManager entityManager) {
        this.truckRepository = truckRepository;
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
    }

    @PostMapping("/trucks")
    public ResponseEntity<TruckResponse> create(@Valid @RequestBody CreateTruckRequest request,
            Authentication authentication) {
        UUID transporterId = CurrentUser.requireIdOrFallback(
                authentication, request.transporterId(), "transporterId");
        Truck truck = new Truck(
                transporterId,
                request.vehicleType(),
                request.capacityKg(),
                request.capacityM3(),
                request.currentCity(),
                request.availableFrom(),
                request.availableUntil());
        // Re-read after save: Hibernate never re-reads database-generated defaults (status,
        // created_at, updated_at) after an insert, and entityManager.refresh() is not an
        // option here — refresh requires an active transaction, and this controller method
        // deliberately has none (each repository call commits on its own).
        Truck saved = truckRepository.findById(truckRepository.save(truck).getId()).orElseThrow();

        // Written directly here, not through a shared audit-writing component: #9 (RBAC and
        // auth) is scoped to build that single reusable component, and duplicating it here
        // would create two competing audit-writing paths to reconcile later. This call is
        // deliberately cheap to swap for that component once it exists.
        AuditLog audit = new AuditLog(
                transporterId, "TRUCK_POSTED", "truck", saved.getId(),
                // capacityKg as String, not BigDecimal: Hibernate dirty-checks JSON columns by
                // round-tripping through the serializer, and BigDecimal comes back as a plain
                // number — a false "dirty" that triggers an UPDATE the append-only trigger on
                // audit_logs rejects. Strings survive the round-trip unchanged.
                Map.of("vehicleType", saved.getVehicleType(),
                        "capacityKg", saved.getCapacityKg().toPlainString(),
                        "currentCity", saved.getCurrentCity()));
        auditLogRepository.save(audit);
        // Not dead code: open-in-view keeps one persistence context alive for the whole
        // request, and Hibernate's dirty check on the JSON details map always reports
        // changed, so without detach it issues an UPDATE at flush — which the append-only
        // trigger on audit_logs rejects.
        entityManager.detach(audit);

        return ResponseEntity.status(HttpStatus.CREATED).body(TruckResponse.from(saved));
    }

    @GetMapping("/trucks/{id}")
    public TruckResponse get(@PathVariable UUID id) {
        return TruckResponse.from(findOrThrow(id));
    }

    @GetMapping("/trucks")
    public List<TruckResponse> listByTransporter(@RequestParam UUID transporterId,
            Authentication authentication) {
        return truckRepository.findByTransporterId(CurrentUser.idOrFallback(authentication, transporterId))
                .stream().map(TruckResponse::from).toList();
    }

    @PatchMapping("/trucks/{id}")
    public TruckResponse update(@PathVariable UUID id, @RequestBody UpdateTruckRequest request) {
        Truck truck = findOrThrow(id);
        if (request.status() != null) {
            truck.setStatus(request.status());
        }
        return TruckResponse.from(truckRepository.save(truck));
    }

    private Truck findOrThrow(UUID id) {
        return truckRepository.findById(id).orElseThrow(() -> new TruckNotFoundException(id));
    }

    @ExceptionHandler(TruckNotFoundException.class)
    public ResponseEntity<String> handleNotFound(TruckNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
