"""Proves ``/ping`` returns a fixed, recognisable payload.

This endpoint exists only as a target for the orchestrator's cross-service call.
It is deliberately separate from ``/health``: ``/health`` is a monitoring contract
that #7/#8 will extend with database checks, and coupling the integration proof to
it would mean a future health change breaking the integration test for reasons that
have nothing to do with integration.

Defends against a broken or renamed route turning the later end-to-end failure into
a transport mystery — if this test passes and the round trip still fails, the fault
is in the network hop or the caller, not here.
"""


def test_ping_returns_pong(client):
    response = client.get("/ping")

    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "matching-service"
    assert body["pong"] is True
