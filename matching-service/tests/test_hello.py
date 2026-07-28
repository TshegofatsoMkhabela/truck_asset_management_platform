"""Proves the ASGI application is constructed and routes a request correctly.

Defends against the scaffolding failure of a package that imports cleanly but
whose app never assembles — a failure an import-only check would not catch.
"""

from fastapi.testclient import TestClient

from matching_service.main import app

client = TestClient(app)


def test_returns_greeting():
    response = client.get("/")

    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "matching-service"
    assert body["status"] == "ok"
