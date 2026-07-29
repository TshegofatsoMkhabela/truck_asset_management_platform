# Testing Summary

The running record of what has actually been tested. Each issue adds its own rows
as it lands, so this stays an accurate account of the work rather than something
reconstructed from memory at the end.

**Every number here is measured, not estimated.** If a figure isn't available yet,
the row says so rather than carrying a placeholder.

## Test cases

| # | Test case | Service | Expected | Actual | Pass/Fail | Added by |
|---|---|---|---|---|---|---|
| T-01 | `GET /health` returns 200 with `status: UP` and `service: backend` | backend | 200, `{"status":"UP","service":"backend"}` | As expected | ✅ Pass | #2 |
| T-02 | `GET /health` returns 200 with `status: UP` and `service: matching-service` | matching-service | 200, `{"status":"UP","service":"matching-service"}` | As expected | ✅ Pass | #2 |
| T-03 | `GET /` returns the service greeting (context loads and routes) | backend | 200, `service: backend`, `status: ok` | As expected | ✅ Pass | #1 |
| T-04 | `GET /` returns the service greeting (ASGI app assembles and routes) | matching-service | 200, `service: matching-service`, `status: ok` | As expected | ✅ Pass | #1 |
| T-05 | `GET /ping` returns the fixed cross-service target payload | matching-service | 200, `{"service":"matching-service","pong":true}` | As expected | ✅ Pass | #5 |
| T-06 | The client calls `GET {base}/ping` with the right verb and parses the response | backend | Request matches; body deserialises to `PingResponse` | As expected | ✅ Pass | #5 |
| T-07 | **Orchestrator reaches matching-service over a real network call** | both (e2e) | 200, body `service` is `matching-service`, `pong` true | As expected | ✅ Pass | #5 |

Each health test asserts the **`service`** key as well as `status`. Both services
answer `/health` with an identical shape, so a misconfigured port mapping could route
a probe to the wrong service and still return `{"status":"UP"}`. The service name is
what distinguishes them.

## Coverage

Line coverage, gated at **80% per service**. CI fails the job below that bar.

Coverage is measured by **JaCoCo** — a Java agent that attaches to the test run and
records which lines actually executed — and **`pytest-cov`**, its Python counterpart,
built on the `coverage.py` library. Both report *line* coverage: the percentage of
executable lines a test run touched at least once.

| Service | Tool | Line coverage | Gate | Status |
|---|---|---|---|---|
| backend | JaCoCo 0.8.12 | **100%** (4/4 lines) | 80% | ✅ Pass |
| matching-service | pytest-cov | **100%** (8/8 lines) | 80% | ✅ Pass |

Reports are uploaded as CI artifacts (`coverage-backend`, `coverage-matching-service`)
on every run, including failures — the number matters most when the gate trips.

### What is excluded, and why

`BackendApplication` is excluded from JaCoCo measurement. Its only statement is the
`SpringApplication.run` bootstrap, which tests never invoke — the Spring test context
loads the class directly rather than calling `main()`. Covering it would mean writing
a test that calls `main()` purely to move the number, which is the coverage theatre the
gate exists to prevent. The exclusion names that one class rather than a package glob,
so any real logic added elsewhere is still measured.

Nothing is excluded on the Python side.

## Known defects

| # | Defect | Severity | Status |
|---|---|---|---|
| *(none)* | | | |

## How to reproduce

Backend:

```bash
cd backend
mvn clean verify
```

`clean` is required, not cosmetic. The JaCoCo agent appends to `target/jacoco.exec`
by default, so a reused `target/` directory measures coverage accumulated across
earlier runs — which can report a passing percentage for code whose tests were
deleted. This was observed during #2 before the gate was corrected.

Matching service:

```bash
cd matching-service
pytest
```

The 80% threshold lives in `pyproject.toml`, so a local run gates exactly as CI does.

### The cross-service end-to-end test (T-07)

T-07 needs **both** services running, so it is tagged `e2e` and excluded from the
default Maven run. Without that exclusion the `backend (Java)` CI job would depend on
a live Python process, destroying the per-service independence that #2 exists to
provide — a backend failure would no longer tell you which service actually broke.

```bash
# terminal 1
cd matching-service && uvicorn matching_service.main:app --port 8000

# terminal 2
cd backend && mvn verify -Pe2e
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

If port 8000 is already in use — likely when several people or agents work on this
machine at once — start the service on another port and point the orchestrator at it:

```bash
cd matching-service && uvicorn matching_service.main:app --port 8010
cd backend && MATCHING_SERVICE_URL=http://localhost:8010 mvn verify -Pe2e
```

This is not hypothetical: during #5 a stale copy of matching-service on port 8000
silently served the test, which failed with a confusing "route not found" until the
port owner was identified. Separate working directories isolate files and git; they
do not isolate ports.

The `e2e` profile also skips the coverage gate. It runs a single round-trip test, so
its coverage figure would be meaningless, and failing the build on it would turn the
e2e job red for a reason unrelated to integration. The real gate stays on the default
build, where it measures the full unit suite.
