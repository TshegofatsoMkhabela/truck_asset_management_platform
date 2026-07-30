"""TAMP matching service.

Given a load and a set of candidate trucks, returns the eligible subset with a
human-readable reason per match.
"""

from fastapi import FastAPI

from matching_service.rules import Load, MatchResult, Truck, find_eligible_matches
from matching_service.schemas import MatchRequest, MatchResponse, MatchResultOut

app = FastAPI(
    title="TAMP Matching Service",
    description="Rule-based matching of cargo loads to available trucks",
    version="0.1.0",
)


@app.get("/")
def hello() -> dict[str, str]:
    """Minimal liveness response proving the service starts and serves traffic.

    A dedicated ``/health`` endpoint replaces this as the monitoring surface.
    """
    return {
        "service": "matching-service",
        "status": "ok",
        "message": "Hello from TAMP matching-service",
    }


@app.get("/health")
def health() -> dict[str, str]:
    """Public liveness probe for monitoring tools and container healthchecks.

    Deliberately unauthenticated: a probe that requires credentials is useless to
    the orchestrator that needs to poll it.
    """
    return {
        "status": "UP",
        "service": "matching-service",
    }


@app.post(
    "/match",
    summary="Return eligible trucks for a load, ranked by capacity headroom",
    description=(
        "Applies the brief section 3.1 rules (capacity, cargo/vehicle "
        "compatibility, availability overlap, location) to the supplied "
        "candidate trucks and returns only the eligible subset, each with the "
        "reasons it was recommended. No database access: the orchestrator "
        "supplies every candidate truck in the request, since this service "
        "stays stateless."
    ),
)
def match(request: MatchRequest) -> MatchResponse:
    load = Load(
        id=request.load.id,
        origin_city=request.load.origin_city,
        cargo_type=request.load.cargo_type,
        weight_kg=request.load.weight_kg,
        pickup_window_start=request.load.pickup_window_start,
        pickup_window_end=request.load.pickup_window_end,
    )
    trucks = [
        Truck(
            id=truck.id,
            current_city=truck.current_city,
            vehicle_type=truck.vehicle_type,
            capacity_kg=truck.capacity_kg,
            available_from=truck.available_from,
            available_until=truck.available_until,
        )
        for truck in request.trucks
    ]

    results: list[MatchResult] = find_eligible_matches(load, trucks)

    return MatchResponse(
        matches=[
            MatchResultOut(truck_id=result.truck_id, score=result.score, reasons=result.reasons)
            for result in results
        ]
    )
