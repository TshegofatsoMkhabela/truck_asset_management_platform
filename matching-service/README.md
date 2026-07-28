# TAMP Matching Service

Python service holding the rule-based matching logic: given a cargo load and a set of candidate trucks, it returns the eligible subset with a human-readable reason per match.

**Stack:** Python 3.11+ · FastAPI · pytest

> **Status:** scaffolding. Starts and responds; no matching rules yet.

## Prerequisites

- Python 3.11 or later

```bash
python --version    # expect 3.11+
```

## Setup

```bash
python -m venv .venv

# Windows (Git Bash)
source .venv/Scripts/activate
# macOS / Linux
source .venv/bin/activate

pip install -e ".[dev]"
```

Installing with `-e` (editable) puts the package on the import path while keeping the source in place, so tests import `matching_service` the same way production code will.

## Run

```bash
uvicorn matching_service.main:app --reload --port 8000
```

Starts on <http://localhost:8000>. Interactive API docs are generated automatically at <http://localhost:8000/docs>.

## Test

```bash
pytest
```

## Verify

```bash
curl http://localhost:8000/
```

Expected:

```json
{"service":"matching-service","status":"ok","message":"Hello from TAMP matching-service"}
```

## Layout

```
src/matching_service/
  __init__.py
  main.py            FastAPI app and routes
tests/
  test_hello.py      Mirrors the source structure
pyproject.toml       Dependencies, packaging and pytest configuration
```

The `src/` layout keeps source out of the repository root so tests exercise the **installed** package rather than loose local files — the arrangement that prevents "passes locally, fails in CI" import differences.
