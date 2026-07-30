package za.co.ice.tamp.backend.matching;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import za.co.ice.tamp.backend.integration.LoadInput;
import za.co.ice.tamp.backend.integration.MatchRequest;
import za.co.ice.tamp.backend.integration.MatchResponse;
import za.co.ice.tamp.backend.integration.MatchResult;
import za.co.ice.tamp.backend.integration.MatchingServiceClient;
import za.co.ice.tamp.backend.integration.TruckInput;
import za.co.ice.tamp.backend.persistence.entity.AuditLog;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.repository.AuditLogRepository;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;

/**
 * Fetches a load and its candidate trucks, asks matching-service for eligible
 * matches, persists the results, and writes the audit event FR-12 requires.
 *
 * <p>Kept separate from the HTTP controller: this exact sequence is what the
 * 2-second timing test measures in isolation from HTTP serialisation overhead,
 * and a plausible future admin "re-run matching" action would call this directly
 * without going through a full HTTP round trip.
 *
 * <p>{@code requestedBy} is supplied by the caller rather than read from an
 * authenticated session: #9 (auth/RBAC) has not landed yet, so there is no
 * security context to pull an actor from. This is the same honest stand-in #7's
 * seed data already uses (a caller-supplied identity in place of a real one),
 * documented in known-limitations.md rather than left implicit.
 */
@Service
public class MatchingCoordinator {

    private final LoadRepository loads;
    private final TruckRepository trucks;
    private final MatchRepository matches;
    private final AuditLogRepository auditLogs;
    private final MatchingServiceClient matchingServiceClient;

    public MatchingCoordinator(
            LoadRepository loads,
            TruckRepository trucks,
            MatchRepository matches,
            AuditLogRepository auditLogs,
            MatchingServiceClient matchingServiceClient) {
        this.loads = loads;
        this.trucks = trucks;
        this.matches = matches;
        this.auditLogs = auditLogs;
        this.matchingServiceClient = matchingServiceClient;
    }

    public List<Match> generateMatches(UUID loadId, UUID requestedBy) {
        Load load = loads.findById(loadId)
                .orElseThrow(() -> new NoSuchElementException("No load with id " + loadId));
        List<Truck> availableTrucks = trucks.findByStatus("AVAILABLE");

        MatchResponse response = matchingServiceClient.requestMatches(
                new MatchRequest(toLoadInput(load), availableTrucks.stream()
                        .map(this::toTruckInput)
                        .toList()));

        List<Match> saved = response.matches().stream()
                .map(result -> matches.save(toMatchEntity(load, result)))
                .toList();

        auditLogs.save(new AuditLog(
                requestedBy,
                "MATCHES_REQUESTED",
                "LOAD",
                loadId,
                Map.of("matchCount", saved.size())));

        return saved;
    }

    private LoadInput toLoadInput(Load load) {
        return new LoadInput(
                load.getId().toString(),
                load.getOriginCity(),
                load.getCargoType(),
                load.getWeightKg().doubleValue(),
                load.getPickupWindowStart(),
                load.getPickupWindowEnd());
    }

    private TruckInput toTruckInput(Truck truck) {
        return new TruckInput(
                truck.getId().toString(),
                truck.getCurrentCity(),
                truck.getVehicleType(),
                truck.getCapacityKg().doubleValue(),
                truck.getAvailableFrom(),
                truck.getAvailableUntil());
    }

    private Match toMatchEntity(Load load, MatchResult result) {
        return new Match(
                load.getId(),
                UUID.fromString(result.truckId()),
                BigDecimal.valueOf(result.score()),
                result.reasons());
    }
}
