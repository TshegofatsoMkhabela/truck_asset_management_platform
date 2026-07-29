"""Proves ``/health`` answers 200 with a status payload.

Defends against a misrouted or erroring probe: an orchestrator polling a broken
health endpoint restart-loops a service that is actually serving traffic fine.

Asserting the ``service`` key as well as ``status`` defends against a second,
quieter failure — both services answer ``/health`` with the same shape, so a
misconfigured port mapping could route this probe to the backend and still look
healthy. The service name is what distinguishes them.
"""


def test_returns_healthy(client):
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["service"] == "matching-service"
