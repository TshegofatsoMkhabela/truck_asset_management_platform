package za.co.ice.tamp.backend.matching;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05: given a load, return the eligible, ranked trucks with reasons.
 *
 * <p>No role annotation and no reference to a shared error response yet: both
 * depend on #9 (auth/RBAC), which has not merged. Recorded in
 * known-limitations.md as a deliberate, temporary gap, not an oversight. The
 * summary and example body below do not depend on #9, so they are added now.
 */
@RestController
public class MatchController {

    private final MatchingCoordinator coordinator;

    public MatchController(MatchingCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Operation(
            summary = "Generate rule-based matches for a load against available trucks (FR-05)",
            description = "Fetches the load and every AVAILABLE truck, asks matching-service for "
                    + "eligible, ranked results, persists them, and records an audit event naming "
                    + "the requester, the load, and how many matches came back.",
            requestBody = @RequestBody(content = @Content(examples = @ExampleObject(
                    name = "Seeded demo owner (#7)",
                    value = "{\"requestedBy\": \"00000000-0000-7000-8000-000000000001\"}"))))
    @PostMapping("/loads/{loadId}/matches")
    public List<MatchSummary> generateMatches(
            @PathVariable UUID loadId,
            @org.springframework.web.bind.annotation.RequestBody GenerateMatchesRequest request) {
        return coordinator.generateMatches(loadId, request.requestedBy()).stream()
                .map(MatchSummary::from)
                .toList();
    }
}
