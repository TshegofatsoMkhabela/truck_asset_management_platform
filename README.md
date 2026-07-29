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
mvn test                     # run tests
```

Verify it is alive:

```bash
curl http://localhost:8080/
# {"status":"ok","message":"Hello from TAMP backend","service":"backend"}
```

Key order is not guaranteed — the response is built from `Map.of`, which is unordered. Match on keys, not on the literal string.

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

Interactive API docs are served at <http://localhost:8000/docs>.

## Documentation

| Document | What it covers |
|---|---|
| [Solution Overview](docs/solution-overview.md) | Purpose, scope, stack and major components |
| [Requirements Traceability](docs/requirements-traceability.md) | FR-01–FR-12 mapped to the code that implements them |
| [Known Limitations](docs/known-limitations.md) | What is mocked, deferred, or deliberately out of scope |
| [Architecture Decision Records](docs/adr/) | Why significant technical choices were made |
| [Diagrams](docs/diagrams/) | Service boundaries and data flow |

## Contributing

- Branch per issue; `main` takes changes by pull request only (branch protection enabled as part of #1).
- Tests are written before implementation. CI **will** enforce **80% line coverage** per service — the pipeline and coverage tooling land in #2/#3; nothing enforces it yet.
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
