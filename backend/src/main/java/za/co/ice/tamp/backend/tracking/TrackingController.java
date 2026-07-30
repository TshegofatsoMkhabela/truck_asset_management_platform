package za.co.ice.tamp.backend.tracking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.acceptance.MatchNotFoundException;

/**
 * FR-08: advance and read simulated trip progress.
 *
 * <p>No role annotation and no shared error-response reference yet: both depend on #9
 * (auth/RBAC), which has not merged. Same deliberate, recorded gap as the other controllers.
 */
@RestController
public class TrackingController {

    private final TrackingCoordinator coordinator;

    public TrackingController(TrackingCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Operation(
            summary = "Advance a trip to its next mock stage (FR-08)",
            description = "Records one simulated trip update against an accepted match. "
                    + "Coordinates are optional; the brief asks for mock coordinates or "
                    + "status progression, not both.",
            requestBody = @RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Status only", value = "{\"status\": \"IN_TRANSIT\"}"),
                    @ExampleObject(name = "Status with a mock position",
                            value = "{\"status\": \"DELIVERED\", \"latitude\": -29.858680, "
                                    + "\"longitude\": 31.021840}")})))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trip update recorded"),
            @ApiResponse(responseCode = "400", description = "Unknown status, or a coordinate out of range"),
            @ApiResponse(responseCode = "404", description = "No match with that id"),
            @ApiResponse(responseCode = "409", description = "The match was never accepted, so it has no trip")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/matches/{matchId}/tracking")
    public TrackingEventResponse advance(
            @PathVariable UUID matchId,
            @Valid @org.springframework.web.bind.annotation.RequestBody TrackingEventRequest request) {
        return TrackingEventResponse.from(coordinator.record(
                matchId, request.status(), request.latitude(), request.longitude()));
    }

    @Operation(
            summary = "Read a trip's recorded progress, oldest first (FR-08)",
            description = "Returns every mock update recorded against the match, in the order "
                    + "they occurred, so the response reads as a journey rather than a set.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip history, possibly empty"),
            @ApiResponse(responseCode = "404", description = "No match with that id")
    })
    @GetMapping("/matches/{matchId}/tracking")
    public List<TrackingEventResponse> history(@PathVariable UUID matchId) {
        return coordinator.historyFor(matchId).stream()
                .map(TrackingEventResponse::from)
                .toList();
    }

    /**
     * Controller-scoped and temporary, for the same reason the acceptance handlers are: #9
     * owns the single global error shape, at which point these are deleted rather than
     * merged around.
     */
    @ExceptionHandler(MatchNotAcceptedException.class)
    public ResponseEntity<String> handleNotAccepted(MatchNotAcceptedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Repeated from {@code AcceptanceController} because {@code @ExceptionHandler} is scoped
     * to the controller that declares it, so the acceptance package's handler does not cover
     * this one. Duplicated deliberately rather than promoted to a {@code @ControllerAdvice}
     * now: #9 introduces exactly that global handler, and adding a second one here first
     * would mean writing a class whose only purpose is to be deleted on merge.
     */
    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<String> handleNotFound(MatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
