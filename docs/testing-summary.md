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
| T-19 | `GlobalExceptionHandler` maps a validation failure, an authentication failure and an access-denial to the documented `ApiError` shape with the correct status (3 tests) | backend | 400/401/403 with `status`, `error`, `message`, `fieldErrors` populated as appropriate | As expected | ✅ Pass | #9 |
| T-20 | `AuditService.record` writes an `AuditLog` with the actor, action, entity type/id and details it was given | backend | Saved entity's fields match the call arguments exactly | As expected | ✅ Pass | #9 |
| T-21 | `POST /auth/register` hashes the password (never stores or returns it) and writes a `REGISTERED` audit event; a blank field returns the documented validation shape (3 tests) | backend | Stored hash starts with `$2` (BCrypt) and differs from the raw password; exactly one audit row; 400 with `fieldErrors.fullName` on a blank name | As expected | ✅ Pass | #9 |
| T-22 | `JwtService` issues a token that parses back to the same user id and role, and rejects a token signed with a different key (2 tests) | backend | Round-trip matches; cross-key token throws `SignatureException` | As expected | ✅ Pass | #9 |
| T-23 | `POST /auth/login` returns a token and the user for valid credentials, returns the documented 401 shape for a wrong password, and writes a `LOGGED_IN` audit event (3 tests) | backend | 200 with `token`/`user`; 401 `UNAUTHENTICATED` for a wrong password; one `LOGGED_IN` row after login | As expected | ✅ Pass | #9 |
| T-24 | `JwtAuthenticationFilter` authenticates a request carrying a valid bearer token and leaves the security context empty for a missing or malformed header (3 tests) | backend | Context holds the token's user id and `ROLE_<role>` only when the header is a genuinely valid bearer token | As expected | ✅ Pass | #9 |
| T-25 | The filter chain keeps `/health`, `/`, `/auth/register` and `/auth/login` public while rejecting an unauthenticated request to any other path with 401 (4 tests) | backend | Public routes 200/400; unlisted route 401 with the documented shape | As expected | ✅ Pass | #9 |
| T-26 | **`GET /audit` allows an Admin token, rejects a non-Admin token with 403, and rejects a missing token with 401** (issue #9 Minimum Integration Test, 3 tests) | backend | 200 for Admin; 403 `ACCESS_DENIED` for a Transporter; 401 `UNAUTHENTICATED` for no token | As expected | ✅ Pass | #9 |
| T-27 | The generated OpenAPI description lists `/auth/register`, `/auth/login` and `/audit` and declares the `bearerAuth` scheme; Swagger UI's page loads, both without a token (2 tests) | backend | 200 for `/v3/api-docs` and `/swagger-ui/index.html`; `components.securitySchemes.bearerAuth.scheme` is `bearer` | As expected | ✅ Pass | #9 |

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
| backend | JaCoCo 0.8.12 | **93.1%** (326/350 lines) | 80% | ✅ Pass |
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
T-07). It reached 93.5% (200/214) once #6 grew to include the JPA persistence layer. It is
now **93.1% (326/350)**, remeasured after #9 added the auth, RBAC and OpenAPI documentation
code: registration, login, the JWT filter, and the two Security-layer error handlers. 24 lines
remain uncovered per `target/site/jacoco/jacoco.csv`: the same pre-existing entity-accessor
residue in `Dispute`, `ComplianceDocument`, `Truck`, `Match`, `Load` and `InetAddressConverter`,
plus `IntegrationController`'s e2e-only line, and a small number of unexercised branches in
`RestAccessDeniedHandler` and `JwtAuthenticationFilter` (defensive catch/write paths that only
the request shapes covered by T-19 through T-27 currently reach). Each of these is a path no
test happens to exercise, not a gap in what the acceptance criteria require. All figures here
are taken from the JaCoCo CSV directly, not estimated.

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
