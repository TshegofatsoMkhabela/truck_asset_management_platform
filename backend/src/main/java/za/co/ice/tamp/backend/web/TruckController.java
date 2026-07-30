package za.co.ice.tamp.backend.web;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import za.co.ice.tamp.backend.web.dto.CreateTruckRequest;
import za.co.ice.tamp.backend.web.dto.TruckResponse;
import za.co.ice.tamp.backend.web.dto.UpdateTruckRequest;

/**
 * CRUD for available trucks (FR-04, Transporter can create and view available trucks).
 *
 * <p>Talks directly to {@link TruckRepository} with no intervening service class, matching
 * #11's precedent for plain CRUD with a not-found check.
 *
 * <p>Deliberately unauthenticated: #9 (RBAC, role-based access control, and auth) owns role
 * enforcement and has not merged yet. {@code transporterId} is accepted as an explicit field/query
 * parameter rather than read from an authenticated principal for the same reason; both are
 * documented in the OpenAPI summaries as temporary, replaced once #9 lands.
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
    public ResponseEntity<TruckResponse> create(@Valid @RequestBody CreateTruckRequest request) {
        Truck truck = new Truck(
                request.transporterId(),
                request.vehicleType(),
                request.capacityKg(),
                request.capacityM3(),
                request.currentCity(),
                request.availableFrom(),
                request.availableUntil());
        Truck saved = truckRepository.save(truck);
        // Hibernate caches database-generated defaults (status, created_at, updated_at) in
        // the persistence context but never re-reads them after an insert; refresh reads
        // the actual database values without an extra query via findById.
        entityManager.refresh(saved);

        // Written directly here, not through a shared audit-writing component: #9 (RBAC and
        // auth) is scoped to build that single reusable component, and duplicating it here
        // would create two competing audit-writing paths to reconcile later. This call is
        // deliberately cheap to swap for that component once it exists.
        AuditLog audit = new AuditLog(
                request.transporterId(), "TRUCK_POSTED", "truck", saved.getId(),
                Map.of("vehicleType", saved.getVehicleType(),
                        "capacityKg", saved.getCapacityKg(),
                        "currentCity", saved.getCurrentCity()));
        auditLogRepository.save(audit);
        // Detach so Hibernate won't try to UPDATE it later (audit_logs is append-only).
        entityManager.detach(audit);

        return ResponseEntity.status(HttpStatus.CREATED).body(TruckResponse.from(saved));
    }

    @GetMapping("/trucks/{id}")
    public TruckResponse get(@PathVariable UUID id) {
        return TruckResponse.from(findOrThrow(id));
    }

    @GetMapping("/trucks")
    public List<TruckResponse> listByTransporter(@RequestParam UUID transporterId) {
        return truckRepository.findByTransporterId(transporterId).stream()
                .map(TruckResponse::from).toList();
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
