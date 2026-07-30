"""Rule-based matching: given a load and candidate trucks, return the eligible
subset with a ranking score and a human-readable reason per rule passed.

Brief section 3.1 sets the four rules applied here: reject if truck capacity is
below load weight, cargo/vehicle compatibility fails, or the availability window
does not overlap; location may be represented by matching cities rather than a
distance value. No machine learning or scoring model, per section 4.1's
exclusion of AI/ML matching: the score below is a transparent function of how
much capacity headroom a truck has, not a learned weight.
"""

from dataclasses import dataclass, field
from datetime import datetime


@dataclass
class Load:
    id: str
    origin_city: str
    cargo_type: str
    weight_kg: float
    pickup_window_start: datetime
    pickup_window_end: datetime


@dataclass
class Truck:
    id: str
    current_city: str
    vehicle_type: str
    capacity_kg: float
    available_from: datetime
    available_until: datetime


@dataclass
class MatchResult:
    truck_id: str
    score: float
    reasons: list[str] = field(default_factory=list)


# Cargo type and vehicle type are two separate enumerations describing different
# things (what the cargo is, versus what the truck is built for), not the same
# type under two names. A same-name shortcut ("vehicle_type == cargo_type") only
# coincidentally works for REFRIGERATED and CONTAINER and has no sensible answer
# for GENERAL cargo, since no vehicle type is named "GENERAL". This table is the
# explicit, maintained mapping instead.
_COMPATIBLE_VEHICLES: dict[str, set[str]] = {
    "GENERAL": {"FLATBED", "CURTAIN_SIDE", "CONTAINER", "TIPPER"},
    "REFRIGERATED": {"REFRIGERATED"},
    "HAZARDOUS": {"TANKER", "CONTAINER"},
    "LIQUID": {"TANKER"},
    "CONTAINER": {"CONTAINER", "FLATBED"},
    "BULK": {"TIPPER", "FLATBED"},
}


def find_eligible_matches(load: Load, trucks: list[Truck]) -> list[MatchResult]:
    """Returns the eligible subset of ``trucks`` for ``load``, ranked by score.

    A rejected truck is dropped entirely rather than returned with a zero score:
    the brief asks for the eligible subset, not a judged list of every truck
    including the ones that cannot legally carry this cargo at all.
    """
    results = [_evaluate(load, truck) for truck in trucks]
    eligible = [result for result in results if result is not None]
    return sorted(eligible, key=lambda result: result.score, reverse=True)


def _evaluate(load: Load, truck: Truck) -> MatchResult | None:
    if truck.capacity_kg < load.weight_kg:
        return None

    if truck.vehicle_type not in _COMPATIBLE_VEHICLES.get(load.cargo_type, set()):
        return None

    windows_overlap = (
        truck.available_from <= load.pickup_window_end
        and truck.available_until >= load.pickup_window_start
    )
    if not windows_overlap:
        return None

    if truck.current_city != load.origin_city:
        return None

    reasons = [
        f"truck capacity {truck.capacity_kg}kg sufficient for {load.weight_kg}kg load",
        f"{truck.vehicle_type} is compatible with {load.cargo_type} cargo",
        "availability windows overlap the pickup window",
        f"truck is already in the origin city ({load.origin_city})",
    ]

    # Score rewards spare capacity (headroom), not a learned or opaque figure: a
    # truck with more room to spare ranks above one that only just fits, since a
    # snug fit leaves no margin if the load estimate is even slightly off. This
    # is a transparent function of the two input numbers, not a model, per
    # section 4.1's exclusion of AI/ML matching.
    headroom_kg = truck.capacity_kg - load.weight_kg
    score = round(min(headroom_kg / load.weight_kg, 1.0) * 100, 2)

    return MatchResult(truck_id=truck.id, score=score, reasons=reasons)
