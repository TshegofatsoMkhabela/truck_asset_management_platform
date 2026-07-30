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

### Full stack (Docker Compose) — the fastest way to a running system

`docker-compose.yml` at the repository root brings up all three pieces, orchestrator,
matching-service and PostgreSQL, with one command. No local Java, Maven, Python or
`pip install` needed for this path; only Docker.

```bash
cp .env.example .env      # once per clone; edit the placeholder values if you want to
docker compose up --build -d
```

Confirm both services are actually serving traffic, not just that their containers started:

```bash
curl http://localhost:8080/health
# {"service":"backend","status":"UP"}

curl http://localhost:8000/health
# {"status":"UP","service":"matching-service"}
```

Interactive API documentation:

- matching-service (FastAPI, generated automatically): <http://localhost:8000/docs>
- orchestrator (springdoc/Swagger UI): <http://localhost:8080/swagger-ui.html>

`.env` is gitignored and holds real local values; `.env.example` is the only one committed,
placeholders only, per the brief's "controlled configuration, no secrets committed" line
(section 4).

### Local database only (Docker), services run natively

Useful for active development on the backend or matching-service, where you want fast
in-process restarts rather than rebuilding a container on every change. The backend needs a
PostgreSQL 18 database; the same `docker-compose.yml` above can bring up just that piece.
The schema #6 defines is applied automatically the first time the container starts, via
Postgres's own `docker-entrypoint-initdb.d` mechanism (no separate migration step to
remember).

```bash
docker compose up -d db
```

Confirm it came up with the expected tables:

```bash
docker compose exec -T db psql -U tamp -d tamp -c '\dt'
```

If port 5432 is already taken by something else on your machine (a common conflict when
several projects run Postgres locally, and this repo hit exactly that with an unrelated
`wattwise_postgres` container during development), pick a different host port without
touching whatever already holds 5432:

```bash
DB_PORT=55432 docker compose up -d db
```

**The schema and seed data only apply once, on a genuinely empty volume.** If you change a
migration file after the first `up`, a plain restart will not re-run it. Remove the volume
first:

```bash
docker compose down -v && docker compose up -d db
```

### Environment switching: one set of values, not two codepaths

The backend reads its database target entirely from environment variables, with the local
Docker values as their defaults (`backend/src/main/resources/application.yml`):

```bash
DB_URL=jdbc:postgresql://localhost:5432/tamp
DB_USERNAME=tamp
DB_PASSWORD=tamp
```

Pointing the app at any other PostgreSQL (a different local port, or eventually a real
deployed database) is one env var change, not a second configuration file:

```bash
DB_URL=jdbc:postgresql://localhost:55432/tamp mvn -f backend spring-boot:run
```

There is deliberately no `application-local.yml` / `application-prod.yml` split: the issue
this setup exists to serve is "without maintaining two codepaths," and a second profile-
specific file is exactly that.

### Demo accounts

Seeded by `db/seed/dev-seed.sql`, one per role, all sharing one password for a single line
in this README rather than three:

| Role | Email | Password |
|---|---|---|
| Freight Owner | `owner@tamp.example` | `TampDemo2026!` |
| Transporter | `transporter@tamp.example` | `TampDemo2026!` |
| Admin | `admin@tamp.example` | `TampDemo2026!` |

The seed data walks a complete demo journey for these three accounts: an already-accepted
match between the Freight Owner's delivered load and the Transporter's truck, its receipt,
three tracking events, a rating from each party, one open dispute, and one pending
compliance document, so the admin console and each role's dashboard have something real to
show as soon as #10 onward add the screens to show it on. Login itself arrives with #9;
until then, the password hashes are verifiable directly:

```bash
docker compose exec -T db psql -U tamp -d tamp -c \
  "SELECT email, password_hash = crypt('TampDemo2026!', password_hash) AS password_matches FROM users;"
```

### Backend (Java)

```bash
cd backend
mvn spring-boot:run          # starts on http://localhost:8080, connects to the DB above
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

Interactive API docs are served at <http://localhost:8080/swagger-ui/index.html>.

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

## Matching (FR-05)

The orchestrator reaches matching-service over HTTP to generate rule-based matches for a
load. With both services running and the seeded database up:

```bash
curl -X POST http://localhost:8080/loads/00000000-0000-7000-8000-000000000011/matches \
  -H "Content-Type: application/json" \
  -d '{"requestedBy": "00000000-0000-7000-8000-000000000001"}'
```
```json
[{"id":"...","truckId":"00000000-0000-7000-8000-000000000021","score":25.0,
  "reasons":["truck capacity 10000.0kg sufficient for 8000.0kg load",
             "REFRIGERATED is compatible with REFRIGERATED cargo",
             "availability windows overlap the pickup window",
             "truck is already in the origin city (Cape Town)"]}]
```

The load and the expected matching truck are both from #7's seed data. The response says
`matching-service`, not `backend`: that is the point, it proves the orchestrator really
made the hop rather than answering for itself.

The target is configured by `MATCHING_SERVICE_URL` (default `http://localhost:8000`), so
Docker Compose (#8) repoints it at the container hostname `matching-service` with no code
change; see `docker-compose.yml`'s `backend` service.

```bash
MATCHING_SERVICE_URL=http://localhost:8010 mvn spring-boot:run
```

No role restriction yet: `requestedBy` stands in for an authenticated caller until #9
lands. See [Known Limitations](docs/known-limitations.md).

This replaced #5's dummy `/integration/ping` round trip, which existed only to prove the
network hop worked before there was any real logic behind it. See
[Testing Summary](docs/testing-summary.md) for how to run the end-to-end and timing tests.

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
