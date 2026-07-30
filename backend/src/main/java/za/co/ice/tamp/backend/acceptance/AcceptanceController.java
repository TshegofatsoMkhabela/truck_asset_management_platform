package za.co.ice.tamp.backend.acceptance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-06: accept or reject a proposed match.
 *
 * <p>No role annotation and no shared error-response reference yet: both depend on #9
 * (auth/RBAC), which has not merged. Same deliberate, recorded gap as {@code MatchController},
 * tracked in known-limitations.md rather than left implicit.
 */
@RestController
public class AcceptanceController {

    private final AcceptanceCoordinator coordinator;

    public AcceptanceController(AcceptanceCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Operation(
            summary = "Accept or reject a proposed match (FR-06)",
            description = "Persists the decision with its actor and timestamp, and records an "
                    + "audit event. Accepting also issues the receipt FR-07 requires.",
            requestBody = @RequestBody(content = @Content(examples = @ExampleObject(
                    name = "Accept, as the seeded demo owner (#7)",
                    value = "{\"decision\": \"ACCEPTED\", "
                            + "\"actorId\": \"00000000-0000-7000-8000-000000000001\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision recorded"),
            @ApiResponse(responseCode = "400", description = "decision was not ACCEPTED or REJECTED"),
            @ApiResponse(responseCode = "404", description = "No match with that id"),
            @ApiResponse(responseCode = "409", description = "The match was already decided")
    })
    @PostMapping("/matches/{matchId}/decision")
    public DecisionResponse decide(
            @PathVariable UUID matchId,
            @Valid @org.springframework.web.bind.annotation.RequestBody DecisionRequest request,
            HttpServletRequest httpRequest) {
        return DecisionResponse.from(coordinator.decide(
                matchId,
                request.decision(),
                request.actorId(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")));
    }

    @Operation(
            summary = "Fetch the receipt issued for an accepted match (FR-07)",
            description = "Returns the digital confirmation, including the human-readable "
                    + "contract ID the parties quote. A rejected or still-proposed match has "
                    + "no receipt, which is a 404 rather than an empty body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt found"),
            @ApiResponse(responseCode = "404", description = "This match has no receipt")
    })
    @GetMapping("/matches/{matchId}/receipt")
    public ReceiptResponse receipt(@PathVariable UUID matchId) {
        return ReceiptResponse.from(coordinator.receiptFor(matchId));
    }

    /**
     * Controller-scoped, and temporary for the same reason #10's are: #9 owns the single
     * global error shape every endpoint should share. These exist so #14 can be demoed and
     * Swagger-tested today, and are deleted rather than merged around once #9's
     * {@code @ControllerAdvice} lands.
     */
    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<String> handleNotFound(MatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(MatchAlreadyDecidedException.class)
    public ResponseEntity<String> handleAlreadyDecided(MatchAlreadyDecidedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(ReceiptNotFoundException.class)
    public ResponseEntity<String> handleNoReceipt(ReceiptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Catches the foreign-key violation an unknown {@code actorId} produces. Both
     * {@code matches.decided_by} and {@code receipts.actor_id} reference {@code users(id)},
     * so naming a user who does not exist fails at flush time; in practice the receipt insert
     * flushes first, giving {@code receipts_actor_id_fkey}. Left unhandled this is a 500
     * carrying a Postgres constraint name, when the caller's actual mistake was a bad id.
     *
     * <p>The constraint message is deliberately not echoed back: it names internal tables and
     * columns, which a caller has no use for and an attacker does.
     *
     * <p>One known imprecision, recorded in known-limitations.md rather than papered over: a
     * genuinely concurrent double-accept also lands here, via
     * {@code receipts.match_id UNIQUE}, and gets this 400 instead of the 409 the sequential
     * path returns. Distinguishing them means matching on constraint names, which is brittle,
     * and #9's global handler is where that belongs if it ever matters.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleUnknownReference(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest()
                .body("The request referenced a user that does not exist. "
                        + "Check actorId against a real user id.");
    }
}
