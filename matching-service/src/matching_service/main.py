"""TAMP matching service.

Given a load and a set of candidate trucks, returns the eligible subset with a
human-readable reason per match. Currently scaffolding only.
"""

from fastapi import FastAPI

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
