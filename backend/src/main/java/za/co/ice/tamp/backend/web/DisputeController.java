package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.Dispute;
import za.co.ice.tamp.backend.persistence.repository.DisputeRepository;
import za.co.ice.tamp.backend.web.dto.DisputeRequest;
import za.co.ice.tamp.backend.web.dto.DisputeResponse;

@RestController
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final EntityManager entityManager;
    private final ObjectProvider<DisputeController> selfProxy;

    public DisputeController(DisputeRepository disputeRepository, EntityManager entityManager,
            ObjectProvider<DisputeController> selfProxy) {
        this.disputeRepository = disputeRepository;
        this.entityManager = entityManager;
        this.selfProxy = selfProxy;
    }

    @PostMapping("/matches/{matchId}/disputes")
    @Operation(summary = "Raise a dispute against a match",
            description = "Flag a match as having an issue. Temporarily requires raisedBy as query param until #9 auth merges.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dispute created",
                content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed (blank description)")
    })
    public ResponseEntity<DisputeResponse> raiseDispute(
            @PathVariable UUID matchId,
            @RequestParam UUID raisedBy,
            @Valid @RequestBody DisputeRequest disputeRequest) {
        Dispute dispute = new Dispute(matchId, null, raisedBy, disputeRequest.description());
        Dispute saved = disputeRepository.save(dispute);

        selfProxy.getObject().writeDisputeAudit(raisedBy, saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(DisputeResponse.from(saved));
    }

    @GetMapping("/disputes")
    @Operation(summary = "List open disputes for admin review",
            description = "Returns disputes with status=OPEN for the admin queue.")
    @ApiResponse(responseCode = "200", description = "Disputes retrieved")
    public List<DisputeResponse> listOpenDisputes() {
        return disputeRepository.findByStatus("OPEN").stream()
                .map(DisputeResponse::from)
                .toList();
    }

    @GetMapping("/disputes/{id}")
    @Operation(summary = "Fetch a dispute by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispute found",
                content = @Content(schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public DisputeResponse getDispute(@PathVariable UUID id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new DisputeNotFoundException(id));
        return DisputeResponse.from(dispute);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDisputeAudit(UUID raisedBy, Dispute dispute) {
        String descriptionPreview = dispute.getDescription().length() > 50
                ? dispute.getDescription().substring(0, 50)
                : dispute.getDescription();

        entityManager.createNativeQuery("""
                INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ? :: jsonb)
                """)
                .setParameter(1, raisedBy)
                .setParameter(2, "DISPUTE_RAISED")
                .setParameter(3, "dispute")
                .setParameter(4, dispute.getId())
                .setParameter(5, Map.of("description", descriptionPreview, "matchId",
                        dispute.getMatchId() != null ? dispute.getMatchId().toString() : null))
                .executeUpdate();
    }

    @ExceptionHandler(DisputeNotFoundException.class)
    public ResponseEntity<String> handleNotFound(DisputeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
