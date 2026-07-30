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
| T-05 | ~~`GET /ping` returns the fixed cross-service target payload~~ | matching-service | n/a | Removed (#13): `/ping` and its caller (`IntegrationController`) existed only as a target for T-07's dummy round trip, superseded by real matching (see T-36) | ➖ Superseded | #5 |
| T-06 | ~~The client calls `GET {base}/ping` with the right verb and parses the response~~ | backend | n/a | Removed (#13): `MatchingServiceClient.ping()` was replaced by `requestMatches(...)`, see T-25 | ➖ Superseded | #5 |
| T-07 | ~~Orchestrator reaches matching-service over a real network call~~ | both (e2e) | n/a | Removed (#13): superseded by T-36, the real matching round trip against #7's seeded data | ➖ Superseded | #5 |
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
| T-25 | orchestrator's Swagger UI loads after `docker compose up` | backend (docker) | 200 | 200, confirmed once #10's and #13's independent `springdoc-openapi` additions were merged into one dependency | ✅ Pass | #8 |
| T-26 | `PasswordEncoder` hashes rather than passing input through unchanged, and salts each call differently (2 tests) | backend | Encoded value never equals raw input; two encodings of the same input differ | As expected | ✅ Pass | #10 |
| T-27 | `CreateUserRequest` bean validation rejects a blank name and a malformed email, and accepts a well-formed request (3 tests) | backend | Violations reported on the right field; a valid request produces none | As expected | ✅ Pass | #10 |
| T-28 | **A user is created via `POST /users`, then fetched via `GET /users/{id}` and the data matches** (issue #10 Minimum Integration Test) | backend | 201 with `complianceStatus: PENDING`; subsequent GET returns the same `fullName`/`email`; stored `password_hash` is never the raw password | As expected | ✅ Pass | #10 |
| T-29 | `GET /users/{id}` for an id with no matching row | backend | 404, not an unhandled 500 | As expected | ✅ Pass | #10 |
| T-30 | `PATCH /users/{id}` with only `complianceStatus` set leaves `fullName` unchanged | backend | 200, `complianceStatus` updated, `fullName` untouched | As expected | ✅ Pass | #10 |
| T-31 | `POST /users` with an email differing only in case from an existing user | backend | 409, not the raw `DataIntegrityViolationException` | As expected | ✅ Pass | #10 |
| T-32 | Rule engine rejects each disqualifying condition independently: capacity, cargo/vehicle incompatibility, non-overlapping availability, different city (4 tests), accepts and explains a valid match, and ranks by capacity headroom | matching-service | Each disqualified case returns no match; the valid case returns all 4 reasons; two eligible trucks are ordered by headroom, not tied | As expected | ✅ Pass | #13 |
| T-33 | `POST /match` correctly serialises the rule engine's decision to and from HTTP, including the empty-result case | matching-service | 200 with the eligible truck and its reasons; 200 with an empty list when nothing qualifies | As expected | ✅ Pass | #13 |
| T-34 | `MatchingServiceClient.requestMatches(...)` sends snake_case field names matching-service expects and parses the response | backend | Request body contains `origin_city`, `weight_kg`, `vehicle_type`; response parses to `truckId`, `score`, `reasons` | As expected | ✅ Pass | #13 |
| T-35 | `MatchingCoordinator` fetches the load and available trucks, persists every eligible match, and writes an audit event naming the actor, the load, and the match count, including when zero trucks are eligible | backend | 1 match persisted and 1 audit event with `matchCount: 1`; on the zero-match path, 0 matches persisted and 1 audit event with `matchCount: 0` | As expected | ✅ Pass | #13 |
| T-36 | **Real HTTP call from the orchestrator to matching-service returns the correct match within 2 seconds on #7's seeded data** (replaces T-05–T-07's dummy round trip; issue #13's required performance evidence) | both (e2e) | The seeded open load matches exactly the one eligible seeded truck, with real reasons, in under 2000ms | 706ms, eligible truck found | ✅ Pass | #13 |
| T-37 | **Freight owner creates a load and retrieves the owner's complete list** (issue #11 Minimum Integration Test) | backend (web) | POST /loads returns 201 with full response; GET /loads?ownerId=X returns list containing the created load | As expected | ✅ Pass | #11 |
| T-38 | `GET /loads/{id}` fetches a single load by ID | backend (web) | Fetch a created load by its ID, verify status (OPEN), origin, destination, weight in response | As expected | ✅ Pass | #11 |
| T-39 | `PATCH /loads/{id}` updates load status only, without requiring re-entry of fields | backend (web) | Update status to MATCHED without sending originCity/destination; response includes original fields unchanged | As expected | ✅ Pass | #11 |
| T-40 | `GET /loads/{id}` with unknown ID returns 404 | backend (web) | Random UUID in path returns 404 with error message | As expected | ✅ Pass | #11 |
| T-41 | Creating a load writes an `audit_logs` row with action=LOAD_POSTED | backend (web) | POST /loads triggers insert into audit_logs with actorId=ownerId, action="LOAD_POSTED", entityType="load" | As expected | ✅ Pass | #11 |
| T-42 | `POST /loads` rejects invalid cargo type, non-positive weight/volume, blank cities | backend (web) | DTO validation (Jakarta Bean Validation) rejects GENERAL+INVALID, weightKg=0, blank originCity (HTTP 400) | As expected | ✅ Pass | #11 |
| T-43 | **Advance a match's tracking status via the API, fetch it back, and confirm it persisted** (issue #15 Minimum Integration Test) | backend (web) | Two POSTs (IN_TRANSIT then DELIVERED) to `/matches/{id}/tracking` each return 201; GET returns both events oldest-first | As expected | ✅ Pass | #15 |
| T-44 | A position-only event (coordinates, no status) is accepted, matching the schema's "position or status" rule | backend (web) | 201 with the coordinates echoed and `status` null | As expected | ✅ Pass | #15 |
| T-45 | An event carrying neither a status nor a coordinate pair is rejected before reaching the database | backend (web) | 400 from DTO validation, not a database-constraint 500 | As expected | ✅ Pass | #15 |
| T-46 | Tracking a match that exists but is not ACCEPTED is refused | backend (web) | 409 with a message naming the match's actual status | As expected | ✅ Pass | #15 |
| T-47 | Tracking endpoints with an unknown match id return 404 on both POST and GET | backend (web) | 404, not an unhandled 500 | As expected | ✅ Pass | #15 |
| T-48 | **Admin reads `/admin/metrics` and the counts cover the rows seeded in the test; a non-admin is rejected from the same endpoint** (issue #17 Minimum Integration Test, with T-49) | backend (web) | 200 with `users`/`loads`/`trucks`/`matches` counts at least covering the seeded rows | As expected | ✅ Pass | #17 |
| T-49 | Every `/admin/*` endpoint refuses a TRANSPORTER's id | backend (web) | 403 on metrics, users, audit-logs and disputes | As expected | ✅ Pass | #17 |
| T-50 | An unknown `adminId` gets the same 403 as a non-admin (no user-enumeration hint) | backend (web) | 403, indistinguishable from the non-admin case | As expected | ✅ Pass | #17 |
| T-51 | `GET /admin/users` lists users with role and compliance status | backend (web) | Seeded FREIGHT_OWNER shows `complianceStatus: PENDING`; the admin shows `role: ADMIN` | As expected | ✅ Pass | #17 |
| T-52 | `GET /admin/audit-logs` returns the audit trail; `GET /admin/disputes` returns a seeded dispute with its OPEN status | backend (web) | Audit listing is a well-formed array; the seeded dispute appears with description and status | As expected | ✅ Pass | #17 |
| T-53 | `AcceptanceCoordinator` persists a decision with its actor and audit event, refuses a second decision on an already-decided match, and refuses an unknown match (3 tests) | backend | Status, `decidedBy` and `decidedAt` all set; `MATCH_ACCEPTED` audit event written; second decision throws rather than writing | As expected | ✅ Pass | #14 |
| T-54 | The decision endpoint returns the right status for each outcome a caller can trigger: success, unknown match, already decided, an invalid decision value, and an `actorId` naming no existing user (5 tests) | backend | 200, 404, 409, 400, 400 respectively. The last was added by this issue's own adversarial review, which found it returned 500 | As expected | ✅ Pass | #14 |
| T-55 | **A match is accepted via the API, then its receipt is fetched and contains the right match, actor and a generated contract ID** (issue #14 Minimum Integration Test), and a rejection issues no receipt (2 tests) | backend | Receipt returns `contractId` starting `TAMP-`, plus the captured IP and user-agent; a rejected match returns 404 from the receipt endpoint | As expected | ✅ Pass | #14 |
| T-56 | The full post-acceptance journey runs against the seeded database over real HTTP: accept, fetch receipt, advance a trip through three stages, read it back, and every guard refuses correctly | backend + db (docker) | Real contract ID issued; 3 tracking events returned in order; 409 on re-decide, 409 on tracking a proposed match, 400 on an unknown status, 404 on a missing receipt | As expected, see evidence below | ✅ Pass | #14/#15 |

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

### Evidence for T-56

Re-run against the merged tree, so this records the code that actually ships: acceptance and
receipts from this PR, tracking from #33. Port 55433 because 5432 was held by an unrelated
container, the conflict the README documents.

```
$ curl -X POST http://localhost:8080/matches/$MATCH/decision     -H "Content-Type: application/json"     -d '{"decision": "ACCEPTED", "actorId": "00000000-0000-7000-8000-000000000001"}'
{"matchId":"019fb19d-...","status":"ACCEPTED",
 "decidedBy":"00000000-0000-7000-8000-000000000001","decidedAt":"2026-07-30T08:47:39.05+02:00"}

$ curl http://localhost:8080/matches/$MATCH/receipt
{"id":"019fb1c7-...","contractId":"TAMP-2026-20D29C52FB","matchId":"019fb19d-...",
 "decision":"ACCEPTED","actorId":"00000000-0000-7000-8000-000000000001",
 "ipAddress":"::1","userAgent":"curl/8.17.0","issuedAt":"2026-07-30T06:47:38.39Z"}

$ curl http://localhost:8080/matches/$MATCH/tracking          # after 2 POSTs, both 201
[{"id":"019fb1c7-573e-...","matchId":"019fb19d-...","latitude":null,"longitude":null,
  "status":"DISPATCHED","occurredAt":"2026-07-30T06:47:39.070202Z"},
 {"id":"019fb1c7-580f-...","matchId":"019fb19d-...","latitude":-29.858680,
  "longitude":31.021840,"status":"DELIVERED","occurredAt":"2026-07-30T06:47:39.279192Z"}]

$ # guards owned by this PR
  re-decide an accepted match                    HTTP 409
  decision value "MAYBE"                         HTTP 400
  actorId naming no existing user                HTTP 400
    "The request referenced a user that does not exist. Check actorId against a real user id."
  GET receipt for a match with none              HTTP 404
    "A receipt is issued only on acceptance."
```

The `contractId` is real, not illustrative: it is generated by the database default on
`receipts.contract_id`, so a `TAMP-` value in the response is what proves the receipt was read
back after insert rather than returned from the persistence context that wrote it. It differs
from the value recorded in earlier drafts of this section because each run issues a new one.

Two manual checks in this section first appeared to fail, and in both cases the check was wrong
rather than the code. Tracking a match returned 201 where 409 was expected, and deciding with a
bogus `actorId` returned 409 where 400 was expected. Both used ids from #7's seed data, which
creates matches already in an `ACCEPTED` state, so in the first case tracking was legitimately
allowed and in the second the already-decided guard correctly fired before the actor was ever
examined. Re-run against genuinely `PROPOSED` matches, both returned what was expected.

The transferable point, since this happened twice: **seeded fixtures carry state**, so a manual
check of a guard has to assert the precondition it depends on, or it silently tests a different
branch than intended. The automated tests never had this problem, because `MatchFixture` builds
a match in a known state for every case.

## Coverage

Line coverage, gated at **80% per service**. CI fails the job below that bar.

Coverage is measured by **JaCoCo** — a Java agent that attaches to the test run and
records which lines actually executed — and **`pytest-cov`**, its Python counterpart,
built on the `coverage.py` library. Both report *line* coverage: the percentage of
executable lines a test run touched at least once.

| Service | Tool | Line coverage | Gate | Status |
|---|---|---|---|---|
| backend | JaCoCo 0.8.12 | **97.9%** (559/571 lines) | 80% | ✅ Pass |
| matching-service | pytest-cov | **100%** (165/165 lines) | 80% | ✅ Pass |

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
#6's schema tests were measured against the merged state of #2/#4/#5, then 93.5% (200/214)
once #6 grew to include the JPA persistence layer. It is now **92.6% (249/269)**, remeasured
after #13 added the matching coordinator, controller and DTOs and removed
`IntegrationController` (the e2e-only line that accounted for one previous gap disappeared
with the class it was in). 20 lines remain uncovered per `target/site/jacoco/jacoco.csv`: 3
in `InetAddressConverter`, 1–4 each across `Dispute`, `ComplianceDocument`, `Truck`, `User`,
`Match` and `Load` (unused accessors, as before), and 1, 3 and 3 in `GenerateMatchesRequest`,
`MatchController` and `MatchSummary` respectively. The last of these is a genuine, honestly
recorded gap rather than an accessor: `MatchController.generateMatches(...)` itself is never
called by any test. `MatchingCoordinatorTest` calls the coordinator directly, and the real
curl in this PR's Testing Guide exercises the controller manually but is not an automated
test. A `MockMvc`-based controller test would close this; not written here because the
coordinator (where the actual logic lives) is already fully covered and the controller
method is a two-line pass-through. All figures here are taken from the JaCoCo CSV directly,
not estimated.

It is now **97.9% (559/571)**, remeasured on the merged tree carrying #14's acceptance and receipts, #33's tracking, #11's loads, #12's trucks and #17's admin endpoints. Measured on the merge rather than carried over from either side, since the bundle is neither one alone.

**Correction to a figure this table carried for four issues.** #13's row read "91.2% (936/1026 lines)", #11 carried the same shape forward as "91.1% (936/1026 lines)", and #17 recorded "96% (1919/1992 instructions)", which is correctly labelled but still sat under a column headed "Line coverage" next to a gate configured on lines. That number was *instruction* coverage, not line coverage: it was summed from columns 4 and 5 of `jacoco.csv` (`INSTRUCTION_MISSED`/`INSTRUCTION_COVERED`) rather than columns 8 and 9. The JaCoCo gate itself is configured on `<counter>LINE</counter>`, so the build was always measuring the right thing and nothing was ever passed that should have failed. Only the hand-written summary read the wrong columns, which is exactly why the error stayed invisible: a wrong number in a document fails no test. Every figure in this table is now taken from columns 8 and 9. The identical 936/1026 appearing under two different percentages across separate issues is the tell that it was being copied forward rather than remeasured.

The superseded #13 paragraph, kept for the record: measured after merging #10's user/profile work
(`UserController`, `CreateUserRequest`, `UpdateUserRequest`, `UserResponse`,
`PasswordEncoderConfig`, `UserNotFoundException`) and #8's Dockerfiles into #13's branch.
The percentage moved because the two feature sets landed independently and the bundle
measured here is neither one alone; `mvn clean verify` was rerun against the merged tree
rather than assuming the two issues' figures would simply add up. It is now
**96% (1919/1992 instructions)**, remeasured after merging #11's load postings, #12's trucks
endpoint, #15's tracking endpoints and #17's admin console into that same tree.

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

### Found during #13: a real network call the mock test could not catch

`MatchingTimingE2ETest`'s first run against a genuinely running matching-service failed
with FastAPI reporting the entire request body missing, even though Spring's own trace
logging confirmed a correct body had been built and handed to the HTTP layer. Root cause:
`MatchingServiceConfig` used the JDK's default `java.net.http.HttpClient`, which sends
`Expect: 100-continue` for POST requests with a body; uvicorn does not answer that
negotiation, so the client gave up waiting and the body never reached the server.
`MatchingServiceClientTest`'s `MockRestServiceServer`-based test passed throughout this,
because a mock server has no transport layer to disagree with the client about. Fixed by
switching to `SimpleClientHttpRequestFactory`. This is the reason #13's plan insisted on a
real network call for the timing test rather than a mock: the bug was only visible there.

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

### The cross-service end-to-end test (T-36)

T-36 needs **both** services running, plus a Docker daemon for its own Testcontainers
Postgres, so it is tagged `e2e` and excluded from the default Maven run. Without that
exclusion the `backend (Java)` CI job would depend on a live Python process, destroying
the per-service independence that #2 exists to provide: a backend failure would no
longer tell you which service actually broke.

```bash
# terminal 1
cd matching-service && uvicorn matching_service.main:app --port 8000

# terminal 2
cd backend && mvn verify -Pe2e
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`, with `Matching round trip took
NNNms` printed (706ms when last measured).

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
