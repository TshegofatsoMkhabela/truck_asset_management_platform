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
| T-08 | All 11 migrations apply in order to an empty PostgreSQL 18 database | backend (db) | Every table created, no error | As expected | ✅ Pass | #6 |
| T-09 | `users` constraints: case-insensitive unique email, role and compliance CHECKs, blank name, time-ordered UUID keys (6 tests) | backend (db) | Each invalid write rejected by its named constraint | As expected | ✅ Pass | #6 |
| T-10 | **A user, a load and a truck persist with intact foreign keys** (issue #6 Minimum Integration Test) | backend (db) | Rows stored, `owner_id` and `transporter_id` read back correctly | As expected | ✅ Pass | #6 |
| T-11 | `loads` and `trucks` constraints: dangling owner, non-positive weight/capacity/volume, backwards time windows, unknown vehicle type, status defaults (10 tests) | backend (db) | Each invalid write rejected by its named constraint | As expected | ✅ Pass | #6 |
| T-12 | `matches` and `receipts`: dangling load/truck, duplicate match pair, decided match with no timestamp, second receipt for one match, distinct contract IDs, queryable score/reasons (8 tests) | backend (db) | Each invalid write rejected; reasons readable as JSON | As expected | ✅ Pass | #6 |
| T-13 | `ratings` and `audit_logs`: score range, one rating per rater per match, self-rating, and audit rows surviving deletion of what they describe plus rejecting UPDATE/DELETE (8 tests) | backend (db) | Invalid ratings rejected; audit entries immutable and durable | As expected | ✅ Pass | #6 |
| T-14 | `tracking_events` and `disputes`: impossible coordinates, event with neither position nor status, status-only event accepted, user-level flags, resolution consistency (10 tests) | backend (db) | Each invalid write rejected by its named constraint | As expected | ✅ Pass | #6 |
| T-15 | `compliance_documents`: unknown document type, dangling user, pending default, review consistency (6 tests) | backend (db) | Each invalid write rejected; reviewer and timestamp recorded on approval | As expected | ✅ Pass | #6 |
| T-16 | `updated_at` advances on modification and equals `created_at` on insert (3 tests) | backend (db) | `updated_at > created_at` after an UPDATE | As expected | ✅ Pass | #6 |
| T-17 | `spring.jpa.hibernate.ddl-auto: validate` rejects a deliberately mismatched entity | backend (persistence) | Application context fails to start | As expected | ✅ Pass | #6 |
| T-18 | Every one of the 10 JPA entities round-trips through its repository: generated id, `JSONB` (`matches.reasons`, `audit_logs.details`), `CITEXT` (case-insensitive `findByEmail`), `INET` (`receipts.ip_address`), and each derived query method (12 tests) | backend (persistence) | Save then read-back matches; derived queries return only the matching rows | As expected | ✅ Pass | #6 |
| T-19 | `docker compose up` on an empty volume creates all 10 schema tables via `docker-entrypoint-initdb.d` | db (docker) | `\dt` lists all 10 tables | As expected | ✅ Pass | #7 |
| T-20 | The seed script populates all 9 seedable tables with a complete demo journey (one accepted match, receipt, 3 tracking events, 2 ratings, 1 dispute, 1 compliance document) | db (docker) | Row counts: users 3, loads 2, trucks 2, matches 1, receipts 1, tracking_events 3, ratings 2, disputes 1, compliance_documents 1 | As expected | ✅ Pass | #7 |
| T-21 | Seeded password hashes are genuine, verifiable bcrypt (`pgcrypto`'s `crypt()`/`gen_salt('bf')`), not placeholders | db (docker) | `password_hash = crypt('TampDemo2026!', password_hash)` is `true` for all 3 demo accounts | As expected | ✅ Pass | #7 |
| T-22 | **The application starts against the dockerized local DB using only the `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` config flags, and a basic read/write against it succeeds** (issue #7 Minimum Integration Test) | backend + db (docker) | App boots, Hibernate schema validation passes, `/health` returns 200; a direct `INSERT`/`SELECT`/`DELETE` against the same live database succeeds while the app remains connected | As expected | ✅ Pass | #7 |
| T-23 | **Clean clone → `docker compose up --build -d` → `/health` on both backend and matching-service return 200** (issue #8 Minimum Integration Test) | backend + matching-service + db (docker) | All 3 containers `Up` (db `Healthy`); `curl :8080/health` and `curl :8000/health` both 200 | As expected, after one fix (see below) | ✅ Pass | #8 |
| T-24 | matching-service's generated API docs page loads after `docker compose up` | matching-service (docker) | `curl :8000/docs` → 200 | As expected | ✅ Pass | #8 |
| T-25 | orchestrator's Swagger UI loads after `docker compose up` | backend (docker) | 200 once #9 merges | Currently 404: `springdoc` is not on `main` yet | ⏳ Pending #9 | #8 |
| T-26 | **Freight owner creates a load and retrieves the owner's complete list** (issue #11 Minimum Integration Test) | backend (web) | POST /loads returns 201 with full response; GET /loads?ownerId=X returns list containing the created load | As expected | ✅ Pass | #11 |
| T-27 | `GET /loads/{id}` fetches a single load by ID | backend (web) | Fetch a created load by its ID, verify status (OPEN), origin, destination, weight in response | As expected | ✅ Pass | #11 |
| T-28 | `PATCH /loads/{id}` updates load status only, without requiring re-entry of fields | backend (web) | Update status to MATCHED without sending originCity/destination; response includes original fields unchanged | As expected | ✅ Pass | #11 |
| T-29 | `GET /loads/{id}` with unknown ID returns 404 | backend (web) | Random UUID in path returns 404 with error message | As expected | ✅ Pass | #11 |
| T-30 | Creating a load writes an `audit_logs` row with action=LOAD_POSTED | backend (web) | POST /loads triggers insert into audit_logs with actorId=ownerId, action="LOAD_POSTED", entityType="load" | As expected | ✅ Pass | #11 |
| T-31 | `POST /loads` rejects invalid cargo type, non-positive weight/volume, blank cities | backend (web) | DTO validation (Jakarta Bean Validation) rejects GENERAL+INVALID, weightKg=0, blank originCity (HTTP 400) | As expected | ✅ Pass | #11 |

### Evidence for T-19–T-22

```
$ docker compose exec -T db psql -U tamp -d tamp -c '\dt'
 public | audit_logs           | table | tamp
 public | compliance_documents | table | tamp
 public | disputes             | table | tamp
 public | loads                | table | tamp
 public | matches              | table | tamp
 public | ratings              | table | tamp
 public | receipts             | table | tamp
 public | tracking_events      | table | tamp
 public | trucks               | table | tamp
 public | users                | table | tamp
(10 rows)
```

```
$ docker compose exec -T db psql -U tamp -d tamp -c "SELECT ... row counts ..."
users 3 | loads 2 | trucks 2 | matches 1 | receipts 1
tracking_events 3 | ratings 2 | disputes 1 | compliance_documents 1
```

```
$ docker compose exec -T db psql -U tamp -d tamp -c \
  "SELECT email, password_hash = crypt('TampDemo2026!', password_hash) FROM users;"
 admin@tamp.example       | t
 owner@tamp.example       | t
 transporter@tamp.example | t
```

```
$ DB_URL=jdbc:postgresql://localhost:55432/tamp DB_USERNAME=tamp DB_PASSWORD=tamp mvn spring-boot:run
...HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
...Database version: 18.4
...Started BackendApplication in 16.93 seconds
$ curl http://localhost:8080/health
{"status":"UP","service":"backend"}
```

```
$ docker compose exec -T db psql -U tamp -d tamp -c "
INSERT INTO users (full_name, email, password_hash, role) VALUES ('MIT Check', 'mit-check@tamp.example', 'x', 'ADMIN');
SELECT full_name, email FROM users WHERE email = 'mit-check@tamp.example';
DELETE FROM users WHERE email = 'mit-check@tamp.example';
"
INSERT 0 1
 MIT Check | mit-check@tamp.example
DELETE 1
```

T-22's read/write is demonstrated directly against the database, not through an HTTP
endpoint: no route in the application touches persistence yet (that begins at #10). The
app boot and the direct database check were run concurrently against the *same* live
container, which is what proves the config-flag connection and the read/write both hold
at once, rather than proving two unrelated things.

### Evidence for T-23–T-24

Run from a genuinely clean state, existing images removed first, to prove the documented
commands work from a fresh clone rather than only against an already-built cache:

```
$ docker rmi truck-matching-backend truck-matching-matching-service truck-matching-db
$ cp .env.example .env
$ docker compose up --build -d
 Container truck-matching-db-1 Healthy
 Container truck-matching-backend-1 Started
 Container truck-matching-matching-service-1 Started

$ curl http://localhost:8080/health
{"status":"UP","service":"backend"} [HTTP 200]

$ curl http://localhost:8000/health
{"status":"UP","service":"matching-service"} [HTTP 200]

$ curl -o /dev/null -w "%{http_code}\n" http://localhost:8000/docs
200
```

One real defect was caught and fixed by this run, not by inspection: the first attempt
had `matching-service` build successfully but crash on start with `Could not import
module "matching_service.main"`. A plain (non-editable) `pip install .` copies whatever
is in `src/` into site-packages at that point in the build, and at that point only an
empty stub package existed (written to satisfy the build backend's package discovery
before the real source is copied in a later layer). The later `COPY src ./src` updated
the build context but not the already-installed copy. Fixed by switching to
`pip install -e .` (editable install), which references `./src` instead of copying it,
so the later `COPY` is what Python actually imports. See `matching-service/Dockerfile`.

A second pass (`/simplify`) changed both services' port bindings from all-interfaces
(`"8080:8080"`) to loopback-only (`"127.0.0.1:8080:8080"`), matching `db`'s own existing
convention. T-23 was re-run after that change to confirm it didn't silently break the
smoke test:

```
$ docker compose up -d
$ curl http://localhost:8080/health
{"status":"UP","service":"backend"} [HTTP 200]
$ curl http://localhost:8000/health
{"status":"UP","service":"matching-service"} [HTTP 200]
```

The first retry returned `HTTP 000` (connection refused) after only a 2-second wait —
not a regression from the binding change, Spring Boot's own startup time, confirmed by
retrying after 15 seconds and getting 200. Loopback and all-interfaces bindings both
resolve `localhost` identically from the same machine; only reachability from other
machines on the network differs, which nothing here tests or needs.

### A near-miss caught before commit, not after

An earlier version of `docker-compose.yml` bind-mounted `db/migrations` directly as
`/docker-entrypoint-initdb.d` (read-write, since Docker cannot create a second bind
mount's mountpoint inside a directory mounted read-only). It worked: T-19 through T-22
above all passed against it, but `git status` before committing showed an untracked
0-byte file, `db/migrations/Z01__dev_seed.sql`, sitting in the actual git-tracked source
folder. Because the read-write bind mount is a live passthrough to the host directory,
not a copy, creating the second mount's mountpoint stub inside it wrote that stub
directly onto the host filesystem, and it survived `docker compose down`.

Every green test run above was run *before* this was noticed. The tests proved the schema
and seed data landed correctly; they said nothing about a side effect on the host outside
the container. Fixed by moving to `db/Dockerfile`, which `COPY`s both directories into the
image at build time, with no bind mount into `db/` so nothing to leak. Re-verified against the
rebuilt image: same 10 tables, same row counts, `git status` clean afterward.

The transferable point: a passing integration test proves the thing it was written to
check, not the absence of every side effect. Checking `git status` after running
infrastructure changes is cheap and would have caught this regardless of which specific
mechanism caused it.

T-08 to T-18 run against a real PostgreSQL 18 container started by **Testcontainers**, a
library that starts and disposes of Docker containers around a test run, rather than an
in-memory substitute. These tests exist to prove *constraint behaviour*, and in-memory
databases only approximate PostgreSQL's constraint semantics, so a green result against
one would be evidence about the wrong database. They need a running Docker daemon.

T-17 and T-18 additionally prove the JPA persistence layer (#6) never diverges from that
schema: `ddl-auto: validate` boots the real application against the migrated container, and
because the application now has a real datasource, every other full-context test in the
module (including #1/#2's `HelloControllerTest` and `HealthControllerTest`) also boots
against this same container rather than a separate one, so there is exactly one schema the
whole test suite is honest about.

Each row above assumes on the **named** constraint, not merely that an exception was
thrown. During #6 this mattered: `relation "loads" does not exist` is itself a
`SQLException`, so seven tests asserting only the exception type would have passed
against a database with no tables in it at all.

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
| backend | JaCoCo 0.8.12 | **93.8%** (220/234 lines) | 80% | ✅ Pass |
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

The backend figure was recorded as 100% (4/4 lines) when #2 landed, then 95% (19/20) once
#6's schema tests were measured against the merged state of #2/#4/#5 (the drop was
`IntegrationController`'s `/integration/ping` handler, reachable only through the e2e-tagged
T-07). It is now **93.5% (200/214)**, remeasured again after #6 grew to include the JPA persistence
layer: 10 entities, 10 repositories, and their accessor and mapping code. 14 lines remain
uncovered per `target/site/jacoco/jacoco.csv`: 1 in `IntegrationController` (the same e2e-only
line as above), 3 in `InetAddressConverter`, and 1–4 each in `Dispute`, `ComplianceDocument`,
`Truck`, `User`, `Match` and `Load` — small enough per class that no single test is missing,
this is the ordinary residue of entity accessors no test path happens to touch. All figures
here are taken from the JaCoCo CSV directly, not estimated.

## Known defects

| # | Defect | Severity | Status |
|---|---|---|---|
| *(none)* | | | |

### Fixed after PR #24's first CI run

`CrossServiceIntegrationE2ETest` (#5, T-07) failed in CI with the same cause as
`HelloControllerTest`/`HealthControllerTest`: it boots the full application context, and
once #6 gave the application a real datasource, every full-context test needs one to reach.
Fixed the same way, by extending `JpaTestBase` so it points at the same migrated
Testcontainers Postgres. Verified locally against a real `uvicorn` instance
(`mvn verify -Pe2e`, `BUILD SUCCESS`) before pushing, not just inferred from the CI log.

Separately, PR #24's `dependency-review` check failed with "Dependency review is not
supported on this repository. Please ensure Dependency graph is enabled." This was a
repository setting, not a code defect: Dependabot alerts (`vulnerability-alerts`) were
disabled, which the dependency-review feature depends on. Enabled via
`gh api -X PUT repos/.../vulnerability-alerts`; the job passed on rerun with no code change.

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
