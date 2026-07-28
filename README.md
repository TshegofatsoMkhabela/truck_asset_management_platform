# TAMP — Truck Asset Matchmaking Platform

A digital freight marketplace connecting **Freight Owners** (organisations with cargo to move) with **Transporters** (operators with available truck capacity), so loads are allocated faster, trucks run fuller, and every match leaves an auditable record.

> **Status:** early scaffolding. Services start and respond; no business logic yet.

## Services

| Path | Language | Role |
|---|---|---|
| [`backend/`](backend/) | Java 17 · Spring Boot · Maven | The **orchestrator** — fronts the public API, owns persistence, and calls out to matching |
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
# {"service":"backend","status":"ok","message":"Hello from TAMP backend"}
```

### Matching service (Python)

```bash
cd matching-service
python -m venv .venv
source .venv/Scripts/activate     # Windows (Git Bash);  macOS/Linux: source .venv/bin/activate
pip install -e ".[dev]"
uvicorn matching_service.main:app --reload --port 8000
pytest                            # run tests
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

- Branch per issue; `main` is protected and takes changes by pull request only.
- Tests are written before implementation. CI enforces **80% line coverage** per service.
- Commit messages: `<type>: <did this> to achieve <this> (#<issue>)`.
