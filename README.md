# TAMP — Truck Asset Matchmaking Platform

A digital freight marketplace connecting **Freight Owners** (organisations with cargo to move) with **Transporters** (operators with available truck capacity), so loads are allocated faster, trucks run fuller, and every match leaves an auditable record.

> **Status:** early scaffolding. Services start and respond; no business logic yet.

## Services

| Path | Language | Role |
|---|---|---|
| [`backend/`](backend/) | Java 21 · Spring Boot · Maven | The **orchestrator** — fronts the public API, owns persistence, and calls out to matching |
| [`matching-service/`](matching-service/) | Python 3.11+ · FastAPI | Rule-based matching: given a load and candidate trucks, returns eligible matches with reasons |

Other documents refer to the Java service as *"the orchestrator"* — that is its **role**; `backend/` is its **path**.

## Quick start

Each service runs independently. Full containerised startup arrives with the Docker Compose work.

### Backend (Java)

```bash
cd backend
mvn spring-boot:run          # starts on http://localhost:8080
mvn clean verify             # run tests + the 80% coverage gate
```

Verify it is alive:

```bash
curl http://localhost:8080/
# {"status":"ok","message":"Hello from TAMP backend","service":"backend"}
```

Key order is not guaranteed — the response is built from `Map.of`, which is unordered. Match on keys, not on the literal string.

Liveness probe:

```bash
curl http://localhost:8080/health
# {"status":"UP","service":"backend"}
```

### Matching service (Python)

```bash
cd matching-service
python -m venv .venv
source .venv/Scripts/activate     # Windows (Git Bash);  macOS/Linux: source .venv/bin/activate
pip install -e ".[dev]"
pytest                            # run tests
uvicorn matching_service.main:app --reload --port 8000   # blocks; Ctrl-C to stop
```

Verify it is alive:

```bash
curl http://localhost:8000/
# {"service":"matching-service","status":"ok","message":"Hello from TAMP matching-service"}
```

Liveness probe:

```bash
curl http://localhost:8000/health
# {"status":"UP","service":"matching-service"}
```

Interactive API docs are served at <http://localhost:8000/docs>.

Both services expose `/health` with the same `{"status","service"}` shape, so container
healthchecks configure one contract rather than two. It is deliberately unauthenticated —
a probe requiring credentials is useless to the tool that must poll it.

## Cross-service call

The orchestrator reaches matching-service over HTTP. With both services running:

```bash
curl http://localhost:8080/integration/ping
# {"service":"matching-service","pong":true}
```

The response says `matching-service`, not `backend` — that is the point: it proves the
orchestrator really made the hop rather than answering for itself.

The target is configured by `MATCHING_SERVICE_URL` (default `http://localhost:8000`),
so Docker Compose in #8 can repoint it at a container hostname without a code change:

```bash
MATCHING_SERVICE_URL=http://localhost:8010 mvn spring-boot:run
```

`/integration/ping` and matching-service's `/ping` are temporary scaffolding for #5 and
are replaced by the real matching endpoint in #13. See
[Testing Summary](docs/testing-summary.md) for how to run the end-to-end test.

## Documentation

| Document | What it covers |
|---|---|
| [Solution Overview](docs/solution-overview.md) | Purpose, scope, stack and major components |
| [Requirements Traceability](docs/requirements-traceability.md) | FR-01–FR-12 mapped to the code that implements them |
| [Testing Summary](docs/testing-summary.md) | Test cases, results, and current coverage per service |
| [Known Limitations](docs/known-limitations.md) | What is mocked, deferred, or deliberately out of scope |
| [Architecture Decision Records](docs/adr/) | Why significant technical choices were made |
| [Diagrams](docs/diagrams/) | Service boundaries and data flow |

## Contributing

- Branch per issue; `main` takes changes by pull request only (branch protection enabled as part of #1).
- Tests are written before implementation. CI enforces **80% line coverage** per service and fails the job below that bar. Coverage is measured by JaCoCo (a Java agent that records which lines actually executed during a test run) and `pytest-cov` (its Python equivalent, built on `coverage.py`).
- `mvn clean verify`, not `mvn test`: the coverage gate is bound to Maven's `verify` phase, so `mvn test` runs the tests but silently skips the check. `clean` matters too — the JaCoCo agent appends to its data file by default, so a reused `target/` directory measures coverage accumulated across earlier runs.
- Commit messages: `<type>: <did this> to achieve <this> (#<issue>)`.

### Secret scanning

Once installed, this hook scans every commit for secrets **before** the commit is created.
It is not automatic — run this once per clone, before your first commit:

```bash
pip install pre-commit          # once per machine
pre-commit install              # installs the git hook into this clone
pre-commit install-hooks        # pre-builds the scanner — takes a few minutes, once
```

`install-hooks` is not optional in practice. The scanner is a Go program that gets compiled
on first use; if you skip this step, that build happens inside your first `git commit`,
which then appears to hang for several minutes. After it, the check adds well under a second
to each commit.

A commit containing a secret is rejected before it exists:

```
Detect hardcoded secrets.................................................Failed
RuleID:      aws-access-token
File:        config.txt
```

Fix it by removing the secret and reading it from an environment variable instead — the
values themselves belong in `.env`, which is gitignored.

If a finding is a **false positive**, add a narrow exception to [`.gitleaks.toml`](.gitleaks.toml)
with a comment explaining why it isn't a real secret. Do not reach for `git commit --no-verify`:
it bypasses the check silently and leaves no record that a scan was skipped.

Why the check runs here rather than in CI, and what it does *not* cover:
[ADR 0001](docs/adr/0001-local-secret-scanning.md) and
[Known Limitations](docs/known-limitations.md).
