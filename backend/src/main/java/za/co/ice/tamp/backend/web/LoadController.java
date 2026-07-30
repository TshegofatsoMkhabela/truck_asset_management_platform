package za.co.ice.tamp.backend.web;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.web.dto.CreateLoadRequest;
import za.co.ice.tamp.backend.web.dto.LoadResponse;
import za.co.ice.tamp.backend.web.dto.UpdateLoadRequest;

/**
 * CRUD for cargo load postings (FR-03, Freight Owner can create and view cargo loads).
 *
 * <p>Talks directly to {@link LoadRepository} with no intervening service class, matching
 * #10's precedent for plain CRUD with a not-found check.
 *
 * <p>Deliberately unauthenticated: #9 (RBAC, role-based access control, and auth) owns role
 * enforcement and has not merged yet. {@code ownerId} is accepted as an explicit field/query
 * parameter rather than read from an authenticated principal for the same reason; both are
 * documented in the OpenAPI summaries as temporary, replaced once #9 lands.
 */
@RestController
public class LoadController {

    private final LoadRepository loadRepository;
    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;
    private final ObjectProvider<LoadController> selfProxy;

    public LoadController(LoadRepository loadRepository, AuditLogRepository auditLogRepository,
            EntityManager entityManager, ObjectProvider<LoadController> selfProxy) {
        this.loadRepository = loadRepository;
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
        this.selfProxy = selfProxy;
    }

    @PostMapping("/loads")
    public ResponseEntity<LoadResponse> create(@Valid @RequestBody CreateLoadRequest request) {
        Load load = new Load(
                request.ownerId(),
                request.originCity(),
                request.destinationCity(),
                request.cargoType(),
                request.weightKg(),
                request.volumeM3(),
                request.pickupWindowStart(),
                request.pickupWindowEnd());
        Load saved = loadRepository.save(load);
        // save() returns the entity as Hibernate holds it in the persistence context, which
        // never re-reads database-generated defaults (status, created_at, updated_at) after
        // an insert; only a fresh SELECT sees the values Postgres actually wrote. The same
        // read-after-write pattern #10's UserController needed for compliance_status.
        Load persisted = loadRepository.findById(saved.getId()).orElseThrow();

        // Write audit in a separate transaction (REQUIRES_NEW) to isolate it from this
        // request's transaction. This avoids Hibernate trying to UPDATE the append-only
        // audit_logs table when this transaction commits. Use selfProxy to ensure the
        // @Transactional annotation on the helper method is honored.
        selfProxy.getObject().writeAuditInNewTransaction(request.ownerId(), persisted);

        return ResponseEntity.status(HttpStatus.CREATED).body(LoadResponse.from(persisted));
    }

    @GetMapping("/loads/{id}")
    public LoadResponse get(@PathVariable UUID id) {
        return LoadResponse.from(findOrThrow(id));
    }

    @GetMapping("/loads")
    public List<LoadResponse> listByOwner(@RequestParam UUID ownerId) {
        return loadRepository.findByOwnerId(ownerId).stream().map(LoadResponse::from).toList();
    }

    @PatchMapping("/loads/{id}")
    public LoadResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLoadRequest request) {
        Load load = findOrThrow(id);
        if (request.status() != null) {
            load.setStatus(request.status());
        }
        return LoadResponse.from(loadRepository.save(load));
    }

    private Load findOrThrow(UUID id) {
        return loadRepository.findById(id).orElseThrow(() -> new LoadNotFoundException(id));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAuditInNewTransaction(UUID actorId, Load load) {
        // Use native SQL to bypass Hibernate ORM entirely. This prevents Hibernate from
        // managing the AuditLog entity and trying to UPDATE the append-only table later.
        String detailsJson = """
                {"originCity": "%s", "destinationCity": "%s", "weightKg": %s}
                """.formatted(
                load.getOriginCity().replace("\"", "\\\""),
                load.getDestinationCity().replace("\"", "\\\""),
                load.getWeightKg());

        entityManager.createNativeQuery("""
                INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ? :: jsonb)
                """)
                .setParameter(1, actorId)
                .setParameter(2, "LOAD_POSTED")
                .setParameter(3, "load")
                .setParameter(4, load.getId())
                .setParameter(5, detailsJson)
                .executeUpdate();
    }

    @ExceptionHandler(LoadNotFoundException.class)
    public ResponseEntity<String> handleNotFound(LoadNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
