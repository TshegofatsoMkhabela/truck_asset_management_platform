# Solution Overview

> Seeded during scaffolding. Each section is filled in as the corresponding work lands, so this document always describes what exists rather than what was intended.

## Purpose

TAMP is a digital freight marketplace connecting Freight Owners (organisations with cargo to move) with Transporters (operators with spare truck capacity). It exists to cut the time taken to allocate a load, reduce empty return trips, and leave a transparent, auditable record of every match and acceptance.

## Scope

A demonstrable MVP covering one continuous journey: register and select a role → post a load or a truck → generate rule-based matches → accept or reject → produce a receipt → track the trip → rate the counterparty → administer and review.

Matching is **rule-based, not machine-learned**. Tracking, compliance verification, and document upload are **simulated**. See [Known Limitations](known-limitations.md) for the full list of what is mocked and why.

## Chosen stack

| Concern | Choice | Rationale |
|---|---|---|
| Orchestrator / API | Java 21, Spring Boot, Maven | Spring Security is the intended auth and RBAC mechanism, so the web layer is chosen to match it rather than be rewritten later |
| Matching service | Python 3.11+, FastAPI | Built-in request validation, generated OpenAPI docs, and a first-class test client — three separate add-ons in the alternatives considered |
| Database | PostgreSQL 18 | Constraints carry the data-integrity rules the rubric grades, so the engine is chosen for what it enforces rather than for storage alone. Version 18 specifically, because the schema generates primary keys with the built-in `uuidv7()` function introduced in that release. See [ADR-2](adr/0002-data-model-and-database-architecture.md) |
| Local runtime | Docker Compose | The database is dockerized (#7); the app containers themselves are added by #8 |

## Major components

| Component | Responsibility |
|---|---|
| `backend/` | Public API surface, authentication and RBAC, persistence, orchestration of calls to matching |
| `matching-service/` | Given one load and a set of candidate trucks, return the eligible subset with a human-readable reason per match |

Two services rather than one, because matching is the part most likely to be replaced later (rule-based today, potentially model-based tomorrow). Keeping it behind a network boundary means that replacement does not touch the API or persistence layers.

## Key design decisions

Recorded as ADRs in [`docs/adr/`](adr/) as they are made.

| ADR | Decision | Issue |
|---|---|---|
| [ADR-1](adr/0001-local-secret-scanning.md) | Secrets are blocked locally at commit time by a Gitleaks pre-commit hook rather than only being caught server-side | #4 |
| [ADR-2](adr/0002-data-model-and-database-architecture.md) | Data model and database architecture: time-ordered UUID keys, `CHECK` constraints over native enums, numbered SQL migrations with no migration tool, an append-only audit log with no foreign keys, and JPA for feature-code access with the schema still owned by SQL | #6 |
| [ADR-3](adr/0003-authentication-and-rbac.md) | Authentication and RBAC: Spring Security plus a stateless JWT rather than a hand-rolled scheme, deny-by-default filter chain, one shared error shape across `@RestControllerAdvice` and the Security layer's own handlers | #9 |

### Database entities

Eleven tables, covering every data object in brief section 3.2 plus the dispute/flag and
compliance-document metadata required by section 2.2.

| Table | Serves | Notes |
|---|---|---|
| `users` | FR-01, FR-02 | Role and compliance status, hashed password only |
| `compliance_documents` | FR-02 | Simulated verification paperwork, metadata only |
| `loads` | FR-03 | Cargo posting with route, weight, volume and pickup window |
| `trucks` | FR-04 | Capacity in kilograms and optional cubic metres, availability window |
| `matches` | FR-05, FR-06 | Score plus the reasons behind it, and the accept/reject decision |
| `receipts` | FR-07 | One immutable confirmation per decided match |
| `tracking_events` | FR-08 | Mock coordinates or status progression |
| `ratings` | FR-09 | One score per party per match |
| `disputes` | FR-10 | Raised against a match or directly against a user |
| `audit_logs` | FR-12 | Append-only, no foreign keys, survives what it describes |

FR-11's platform metrics are derived by query rather than stored, so there is no analytics table.

### Persistence layer

Feature code reads and writes through Spring Data JPA entities and repositories under
`backend/src/main/java/za/co/ice/tamp/backend/persistence/`, never through a hand-written SQL
string. `spring.jpa.hibernate.ddl-auto: validate` means the application refuses to start if an
entity does not match the schema in `db/migrations/`, which stays the schema's only author.
See [ADR-2](adr/0002-data-model-and-database-architecture.md) for why generation was rejected
and what it cost to map `CITEXT`, `JSONB` and `INET` columns correctly.

### Local database

`docker-compose.yml` at the repository root (#7) brings up a disposable PostgreSQL 18
container, built from `db/Dockerfile`, which bakes `db/migrations/` and
`db/seed/dev-seed.sql` into Postgres's own `docker-entrypoint-initdb.d` init mechanism at
image build time. So the schema and a full walk-the-journey sample dataset apply
automatically the first time the container starts: no separate migration or seeding step
to run by hand, and nothing is bind-mounted into the git-tracked `db/` folder at runtime. The database target is entirely environment-
variable driven (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`), with the local Docker values as
their defaults, so pointing the app at a different database is one variable, not a second
configuration file. See the README's Quick Start for the exact commands and demo
credentials.

### Matching (FR-05)

`matching_service/rules.py` applies the brief section 3.1 rules (capacity, cargo/vehicle
compatibility, availability overlap, city-level location) as pure functions with no HTTP or
persistence dependency, so the rules are testable without a running server. `POST /match`
exposes it over HTTP; the orchestrator's `MatchingCoordinator` fetches a load and its
available trucks, calls that endpoint, persists the eligible results, and writes an audit
event, all behind `POST /loads/{loadId}/matches`.

### Acceptance and receipts (FR-06, FR-07)

`AcceptanceCoordinator.decide(...)` turns a proposed match into a decided one exactly once:
`matches.status` moves to `ACCEPTED` or `REJECTED` with the deciding actor and timestamp, a
receipt is issued on acceptance with a generated `contractId` prefixed `TAMP-`, and an audit
event is written, all in one transaction. `AcceptanceController` exposes this behind
`POST /matches/{matchId}/decision` and `GET /matches/{matchId}/receipt`. A second decision on
an already-decided match returns 409 rather than overwriting the first.

### Tracking (FR-08)

`TrackingController` appends mock status and/or coordinate events to an `ACCEPTED` match's
trip history behind `POST /matches/{matchId}/tracking`, read back oldest-first via the same
path's `GET`. Coordinates and statuses are synthetic, per the brief's exclusion of live
GPS/telematics for this track. See [Known Limitations](known-limitations.md): there is
currently no check against posting stages out of order.

### Ratings and disputes (FR-09, FR-10)

`RatingController` records a one-to-five score and optional comment behind
`POST /matches/{matchId}/ratings`, readable back per match or per user.
`DisputeController` lets any party flag a match behind `POST /matches/{matchId}/disputes`,
writing a `DISPUTE_RAISED` audit event; `GET /disputes` lists the open queue for admin review.

### Admin console (FR-10, FR-11, FR-12)

`AdminController` exposes read-only oversight: `GET /admin/metrics` for basic platform counts,
`GET /admin/users` for every user with role and compliance status, `GET /admin/audit-logs` for
the full append-only trail, and `GET /admin/disputes` for the open-flags queue. Restricted to
a caller whose stored role is ADMIN, looked up by a caller-supplied `adminId`, not yet read
from an authenticated principal; see the authentication section below.

### Authentication (FR-01)

`AuthController` exposes `POST /auth/register` and `POST /auth/login`, the latter issuing a
signed JWT (JSON Web Token, verifiable from its signature alone with no database lookup)
naming the user's id and role, verified on every request by `JwtAuthenticationFilter`. This
half of FR-01 works. Every business controller now reads that identity through `CurrentUser`
and uses it in place of its caller-supplied id field (`ownerId`, `transporterId`, `actorId`,
and so on) whenever a real token is presented, which is what lets a Swagger UI demo log in
once and stop retyping ids at every step. The other half, role-based access control actually
requiring a token, does not yet exist: `SecurityConfig` currently `permitAll()`s every
business endpoint, so a caller presenting no token still gets through by supplying the id
directly, exactly as before. See [Known Limitations](known-limitations.md) for the full
accounting of what this leaves open, and [ADR 0003](adr/0003-authentication-and-rbac.md) for the design the enforcement
half was meant to follow.

### Demo journey (issue #18)

`DemoJourneyE2ETest` is the project's single top-level integration proof: registration
through to the admin view, run continuously over real HTTP against a real matching-service
round trip, not a persisted-directly match. `docs/demo-guide.md` is the same journey written
as a Swagger UI click-through for a non-developer to present live. Both were verified against
this repository's actual behaviour, including the authentication gap above, rather than
against what earlier issues predicted #9 would eventually close.

### API documentation

`springdoc-openapi-starter-webmvc-ui` (added #13) exposes `/swagger-ui/index.html` and
`/v3/api-docs` with zero configuration beyond the dependency itself, satisfying brief
section 7's OpenAPI submission requirement and section 2.2's stated demonstration vehicle.
Every feature issue from #10 onward inherits this rather than needing its own setup.
