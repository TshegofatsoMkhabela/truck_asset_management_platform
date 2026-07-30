"""Proves the HTTP layer around ``find_eligible_matches`` (rules.py) correctly
passes data to and from the rule engine.

The rules themselves are already proven in test_rules.py; this file exists to
catch a field-name mismatch between the request/response schema and what the
orchestrator will actually send, which unit tests on the pure function alone
cannot catch.
"""


def test_match_endpoint_returns_eligible_trucks_with_reasons(client):
    request_body = {
        "load": {
            "id": "load-1",
            "origin_city": "Johannesburg",
            "cargo_type": "GENERAL",
            "weight_kg": 10000,
            "pickup_window_start": "2026-08-03T06:00:00Z",
            "pickup_window_end": "2026-08-05T06:00:00Z",
        },
        "trucks": [
            {
                "id": "truck-1",
                "current_city": "Johannesburg",
                "vehicle_type": "FLATBED",
                "capacity_kg": 20000,
                "available_from": "2026-08-03T06:00:00Z",
                "available_until": "2026-08-05T06:00:00Z",
            },
            {
                "id": "truck-2",
                "current_city": "Cape Town",
                "vehicle_type": "FLATBED",
                "capacity_kg": 20000,
                "available_from": "2026-08-03T06:00:00Z",
                "available_until": "2026-08-05T06:00:00Z",
            },
        ],
    }

    response = client.post("/match", json=request_body)

    assert response.status_code == 200
    body = response.json()
    assert len(body["matches"]) == 1
    match = body["matches"][0]
    assert match["truck_id"] == "truck-1"
    assert match["score"] > 0
    assert len(match["reasons"]) == 4


def test_match_endpoint_returns_empty_list_when_no_truck_is_eligible(client):
    request_body = {
        "load": {
            "id": "load-1",
            "origin_city": "Johannesburg",
            "cargo_type": "LIQUID",
            "weight_kg": 10000,
            "pickup_window_start": "2026-08-03T06:00:00Z",
            "pickup_window_end": "2026-08-05T06:00:00Z",
        },
        "trucks": [
            {
                "id": "truck-1",
                "current_city": "Johannesburg",
                "vehicle_type": "FLATBED",
                "capacity_kg": 20000,
                "available_from": "2026-08-03T06:00:00Z",
                "available_until": "2026-08-05T06:00:00Z",
            }
        ],
    }

    response = client.post("/match", json=request_body)

    assert response.status_code == 200
    assert response.json()["matches"] == []
