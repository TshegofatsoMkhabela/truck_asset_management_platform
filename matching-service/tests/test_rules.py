"""Proves the matching rules from brief section 3.1: capacity, cargo/vehicle
compatibility, availability overlap, and location, each rejecting exactly the
case it targets and explaining an accepted match rather than merely scoring it.
"""

from datetime import datetime, timedelta, timezone

from matching_service.rules import Load, Truck, find_eligible_matches

NOW = datetime(2026, 8, 3, 6, 0, tzinfo=timezone.utc)


def _load(**overrides) -> Load:
    defaults = dict(
        id="load-1",
        origin_city="Johannesburg",
        cargo_type="GENERAL",
        weight_kg=10000,
        pickup_window_start=NOW,
        pickup_window_end=NOW + timedelta(days=2),
    )
    defaults.update(overrides)
    return Load(**defaults)


def _truck(**overrides) -> Truck:
    defaults = dict(
        id="truck-1",
        current_city="Johannesburg",
        vehicle_type="FLATBED",
        capacity_kg=20000,
        available_from=NOW,
        available_until=NOW + timedelta(days=2),
    )
    defaults.update(overrides)
    return Truck(**defaults)


def test_rejects_truck_below_load_weight():
    # Defends against a silently-passing capacity rule: a truck lighter than the
    # load must never appear as eligible, since an inverted comparison here would
    # recommend a truck that cannot physically carry the cargo.
    load = _load(weight_kg=15000)
    truck = _truck(capacity_kg=10000)

    matches = find_eligible_matches(load, [truck])

    assert matches == []


def test_rejects_incompatible_cargo_vehicle_pairing():
    # LIQUID cargo may only travel on a TANKER; a FLATBED must be rejected even
    # though it has ample capacity, defending against a same-name shortcut that
    # only coincidentally works for cargo/vehicle types that share a name.
    load = _load(cargo_type="LIQUID")
    truck = _truck(vehicle_type="FLATBED")

    matches = find_eligible_matches(load, [truck])

    assert matches == []


def test_rejects_non_overlapping_availability_window():
    # Defends against an off-by-one or inverted overlap check silently matching
    # a truck that is nowhere near available when the cargo needs picking up.
    load = _load(
        pickup_window_start=NOW,
        pickup_window_end=NOW + timedelta(days=1),
    )
    truck = _truck(
        available_from=NOW + timedelta(days=5),
        available_until=NOW + timedelta(days=7),
    )

    matches = find_eligible_matches(load, [truck])

    assert matches == []


def test_rejects_different_city():
    # Brief section 3.1 permits city-level matching rather than distance math;
    # defends against a truck in an entirely different city being recommended
    # because no location check exists at all.
    load = _load(origin_city="Johannesburg")
    truck = _truck(current_city="Cape Town")

    matches = find_eligible_matches(load, [truck])

    assert matches == []


def test_accepts_and_explains_a_valid_match():
    load = _load(cargo_type="GENERAL", weight_kg=10000, origin_city="Johannesburg")
    truck = _truck(vehicle_type="FLATBED", capacity_kg=20000, current_city="Johannesburg")

    matches = find_eligible_matches(load, [truck])

    assert len(matches) == 1
    result = matches[0]
    assert result.truck_id == "truck-1"
    assert 0 <= result.score <= 100
    assert any("capacity" in reason for reason in result.reasons)
    assert any("compatible" in reason for reason in result.reasons)
    assert any("availability" in reason for reason in result.reasons)
    assert any("city" in reason for reason in result.reasons)


def test_ranks_higher_capacity_headroom_above_tighter_fit():
    # Two eligible trucks should be ranked, not merely both marked eligible, so
    # the "ranked or eligible" requirement (brief section 2.2) is not satisfied
    # by returning ties for every passing truck regardless of fit.
    load = _load(weight_kg=10000)
    snug_truck = _truck(id="snug", capacity_kg=10500)
    roomy_truck = _truck(id="roomy", capacity_kg=20000)

    matches = find_eligible_matches(load, [snug_truck, roomy_truck])

    assert [m.truck_id for m in matches] == ["roomy", "snug"]
