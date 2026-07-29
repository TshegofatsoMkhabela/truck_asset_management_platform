"""Shared test fixtures.

The ``TestClient`` is built once here rather than at module scope in each test
file, so adding a test module does not mean repeating the app wiring.
"""

import pytest
from fastapi.testclient import TestClient

from matching_service.main import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)
