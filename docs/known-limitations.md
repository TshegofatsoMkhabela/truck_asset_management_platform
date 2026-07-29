# Known Limitations

What is mocked, deferred, or deliberately excluded — and why. Added to as work lands, so that anything incomplete is a **recorded decision** rather than a discovered gap.

## Deliberately simulated

Behaviour that is intentionally faked because a real integration is out of scope.

| Area | What is simulated | Why |
|---|---|---|
| Compliance documents | `compliance_documents` stores metadata only: document type, file name, status and reviewer. No file is uploaded, stored or scanned, and the file name is a label rather than a path to anything | FR-02 asks for "simulated document upload **or** metadata", and section 4.1 keeps real document handling out of scope. An admin can approve or reject paperwork that does not physically exist, which is what the demo needs |
| Trip tracking coordinates | `tracking_events` stores plain latitude/longitude numbers written by the application, with no live source behind them. The schema constrains them to physically possible ranges but cannot tell a real position from an invented one | Brief section 4.1 excludes live telematics and GPS providers. FR-08 asks only for tracking "simulated using mock coordinates or status progression" |

## Deferred to roadmap

Work that is specified but not built, so a team picking this up has the design ready.

| Area | Current state | What a team would need to do |
|---|---|---|
| Cloud deployment | Not built. The brief requires a reproducible **local** setup only, and section 4.1 places production-scale infrastructure out of scope. Images are stateless and env-var driven, so no application change is needed to host them elsewhere | Choose a container host, supply the same environment variables from a managed secrets store, and point `DB_HOST` at a managed Postgres |
| Single entry point / reverse proxy | Not built. Each service is reached directly on its own port, which is sufficient for a local demo | Add a proxy in front of both services if the system is ever exposed beyond localhost — at which point TLS becomes a hard requirement, not an option |
| Location proximity in matching | Not built. Locations are city name strings, so matching compares cities for equality. A depot 20km outside Johannesburg does not match a Johannesburg load | Brief section 3.1 explicitly permits "matching cities/areas" instead of a distance value, and section 4.1 excludes production maps. Adding PostGIS, the geospatial extension for PostgreSQL, would put an extension dependency in every container to serve a requirement nobody made |
| Asset ownership by role | Not enforced by the database. `trucks.transporter_id` and `loads.owner_id` are guaranteed to reference a real user, but not a user holding the matching role. Writing directly to the database can create a truck owned by a Freight Owner | PostgreSQL cannot express "reference a row whose other column equals X" without a trigger or a denormalised copy of the role. Both were rejected in [ADR-1](adr/0001-data-model-and-database-architecture.md); the check lives in the service layer (#12) instead |
| Cross-table business rules | Not enforced by the database. A rating can be recorded against a match that was never accepted, by a user who was not party to it; a tracking event can be attached to a proposed match; a receipt can claim a decision its match does not have | A `CHECK` constraint cannot read another table, so each of these needs a trigger or service-layer validation. They are enforced by the issue that owns the behaviour: #14 (receipts), #15 (tracking), #16 (ratings). The schema guarantees the references exist and the values are in range, not that the workflow order was followed |
| Audit log immutability | Enforced against `UPDATE` and `DELETE` by trigger, but `TRUNCATE` bypasses row-level triggers entirely, and anyone able to drop the trigger can rewrite history | Full tamper-evidence needs either restricted database privileges or hash-chained entries, neither of which the brief asks for. The trigger stops accidental and casual modification, which is what an MVP audit trail needs to be trustworthy in a demo |
| Schema migration tooling | Not used. Migrations are numbered `.sql` files applied in filename order, with no record of what has already run and no rollback path | With one contributor, one environment and nothing deployed, a tool such as Flyway would buy consistency guarantees against a problem that cannot occur yet. This has a clear expiry: adopt a tool the first time the schema changes after something is deployed |
| Dynamic security scanning (DAST) | Not run. Requires a deployed environment that does not exist, and the brief's security expectation is basic RBAC, validation, password hashing and no committed secrets | Stand up a deployed environment, then add a baseline passive scan before any gating |

## Process limitations

Constraints arising from how the project is being built rather than from the code.

| Limitation | Detail | Recommended change on handoff |
|---|---|---|
| Pull requests require **0 approving reviews** | `main` is protected and takes changes by PR only, but GitHub does not permit approving your own pull request. On a single-contributor repository, requiring ≥1 approval would make every PR unmergeable except by admin override — which turns the protection into theatre, since the override becomes routine | Raise required approvals to **≥1** as soon as a second contributor exists. The gate structure is already in place; only the count changes |

## Technical debt

| Item | Impact | Suggested fix |
|---|---|---|
| *(none yet)* | | |
