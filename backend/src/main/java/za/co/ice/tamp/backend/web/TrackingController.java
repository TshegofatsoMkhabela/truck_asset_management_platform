package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.TrackingEvent;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TrackingEventRepository;
import za.co.ice.tamp.backend.web.dto.CreateTrackingEventRequest;
import za.co.ice.tamp.backend.web.dto.TrackingEventResponse;

/**
 * Mock trip tracking for an accepted match (FR-08, issue #15).
 *
 * <p>Talks directly to the repositories with no intervening service class, matching #11's
 * precedent for plain CRUD with a not-found check. Coordinates and statuses are synthetic:
 * the brief excludes live GPS/telematics, so "tracking" means appending mock events and
 * reading them back oldest-first.
 *
 * <p>Deliberately unauthenticated: #9 (RBAC, role-based access control, and auth) owns role
 * enforcement and has not merged yet; the intended role is documented in the OpenAPI
 * summaries as temporary, replaced once #9 lands.
 */
@RestController
public class TrackingController {

    private final MatchRepository matchRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final EntityManager entityManager;
    private final ObjectProvider<TrackingController> selfProxy;

    public TrackingController(MatchRepository matchRepository,
            TrackingEventRepository trackingEventRepository, EntityManager entityManager,
            ObjectProvider<TrackingController> selfProxy) {
        this.matchRepository = matchRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.entityManager = entityManager;
        this.selfProxy = selfProxy;
    }

    @PostMapping("/matches/{matchId}/tracking")
    @Operation(summary = "Record a mock tracking update for an accepted match",
            description = "Appends a synthetic status and/or coordinate event to the match's trip "
                    + "history. Only ACCEPTED matches can be tracked: a PROPOSED or REJECTED match "
                    + "has no trip. Intended role: TRANSPORTER (unenforced until #9 auth/RBAC merges).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Event recorded",
                content = @Content(schema = @Schema(implementation = TrackingEventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Neither a status nor a coordinate pair supplied, or values out of range"),
        @ApiResponse(responseCode = "404", description = "Match not found"),
        @ApiResponse(responseCode = "409", description = "Match exists but is not ACCEPTED")
    })
    public ResponseEntity<TrackingEventResponse> record(@PathVariable UUID matchId,
            @Valid @RequestBody CreateTrackingEventRequest request) {
        Match match = findOrThrow(matchId);
        if (!"ACCEPTED".equals(match.getStatus())) {
            throw new MatchNotAcceptedException(matchId, match.getStatus());
        }

        TrackingEvent saved = trackingEventRepository.save(new TrackingEvent(
                matchId, request.latitude(), request.longitude(), request.status()));
        // Re-read: Hibernate never re-reads database-generated defaults (occurred_at)
        // after an insert; only a fresh SELECT sees the value Postgres actually wrote.
        TrackingEvent persisted = trackingEventRepository.findById(saved.getId()).orElseThrow();

        // Same REQUIRES_NEW native-insert pattern as LoadController: audit_logs is
        // append-only, and letting Hibernate manage the row risks a rejected UPDATE
        // when a later flush dirty-checks it.
        selfProxy.getObject().writeAuditInNewTransaction(match, persisted);

        return ResponseEntity.status(HttpStatus.CREATED).body(TrackingEventResponse.from(persisted));
    }

    @GetMapping("/matches/{matchId}/tracking")
    @Operation(summary = "Fetch a match's tracking history, oldest event first",
            description = "Returns every recorded mock trip event for the match. Intended role: "
                    + "any authenticated party to the match (unenforced until #9 auth/RBAC merges).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Events retrieved (may be empty)"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public List<TrackingEventResponse> history(@PathVariable UUID matchId) {
        findOrThrow(matchId);
        return trackingEventRepository.findByMatchIdOrderByOccurredAtAsc(matchId).stream()
                .map(TrackingEventResponse::from).toList();
    }

    private Match findOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAuditInNewTransaction(Match match, TrackingEvent event) {
        String detailsJson = """
                {"status": %s, "latitude": %s, "longitude": %s}
                """.formatted(
                event.getStatus() == null ? "null" : "\"" + event.getStatus() + "\"",
                event.getLatitude(),
                event.getLongitude());

        entityManager.createNativeQuery("""
                INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ? :: jsonb)
                """)
                .setParameter(1, match.getDecidedBy())
                .setParameter(2, "TRACKING_EVENT_RECORDED")
                .setParameter(3, "tracking_event")
                .setParameter(4, event.getId())
                .setParameter(5, detailsJson)
                .executeUpdate();
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<String> handleNotFound(MatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(MatchNotAcceptedException.class)
    public ResponseEntity<String> handleNotAccepted(MatchNotAcceptedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
