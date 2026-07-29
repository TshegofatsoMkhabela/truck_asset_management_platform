# ADR-2: Data model and database architecture

**Status:** Accepted
**Date:** 2026-07-29
**Issue:** #6

## Context

Every feature issue from #9 onward writes to the same database. Deciding the schema once,
before any of them start, avoids the migration churn that comes from each feature inventing
the tables it happens to need. At the point this was written nothing was deployed and no data
existed, which matters more than it sounds: **no decision here is destructive**, so choices
could be made on correctness rather than on what was safe to change.

The shape is set by the assessment brief rather than by preference. Brief section 3.2 lists
eight minimum data objects with their core fields; section 2.2 adds a dispute/flag and
compliance-document metadata; section 3.1 requires that a match explains *why* it was
recommended. Where issue #6's own list disagreed with the brief (it omitted tracking events
and disputes), the brief won, and the extra tables were built here rather than deferred.

**One list was never enough.** The table set was drawn up three times: first from issue #6's
own "What" section, then corrected against section 3.2's data objects (which added
`tracking_events` and `disputes`), then corrected again against FR-02 and section 2.2 (which
added `compliance_documents`, a requirement that appears in neither issue #6 nor section 3.2).
Anyone extending this schema should assume the same: a requirement can be stated in exactly
one place in the brief and nowhere else.

A term used throughout: a **foreign key** is a column that must point at an existing row in
another table, so the database refuses to store a reference to something that isn't there.

## Decisions

### Primary keys are time-ordered UUIDs

**Chosen:** `UUID PRIMARY KEY DEFAULT uuidv7()` **to achieve** identifiers that cannot be walked
by an attacker counting upward, and that two services can hold without sharing a sequence.
A **UUIDv7** is a 128-bit identifier whose leading 48 bits are a timestamp, so newly generated
values sort after older ones.
**Rejected:** `BIGSERIAL`, an auto-incrementing 64-bit integer and the conventional default.
**Why it lost here:** identifiers appear in API paths (`/loads/{id}`), and sequential integers
let anyone enumerate every load on the platform by counting. Security is this submission's
stated differentiator, so shipping a trivially enumerable API undercuts it. The usual counter-
argument (that UUIDs fragment the index) applies to random UUIDv4, not to v7: because v7
values are time-ordered, inserts land at the end of the B-tree (the sorted structure PostgreSQL
keeps indexes in) exactly as an incrementing integer does.
**Cost accepted:** 16 bytes per key instead of 8, and identifiers that are unpleasant to type
by hand when debugging. It also pins **PostgreSQL 18**, since `uuidv7()` is built in from that
release; on an older server the default would have to be swapped for an extension or
application-side generation.
**Honesty note:** the brief does not require this. Section 3.2 says only "ID". This was a
judgement call informed by research, not a requirement traceable to the assessment.

### Fixed value sets are TEXT with a CHECK constraint

**Chosen:** `TEXT` columns constrained by `CHECK (col IN (...))` for every role, status and type
**to achieve** value sets that can be changed with an ordinary `ALTER TABLE` as the product
learns what states it actually needs.
**Rejected:** native PostgreSQL `ENUM` types.
**Why it lost here:** adding a value to an enum is an `ALTER TYPE`, and a newly added value
cannot be used in the same transaction that adds it, so every future status change becomes two
migrations. #17's admin work is the most likely source of exactly that change, and the cost
lands precisely where the deadline is tightest.
**Cost accepted:** weaker type safety. Application code sees a `String`, so the Java compiler
will not catch a bad literal; only the database will, at write time.

### Migrations are numbered SQL files with no migration tool

**Chosen:** plain zero-padded `V01__*.sql` … `V10__*.sql` under `db/migrations/`, applied in filename order,
**to achieve** a schema that the Java service, the dockerized database (#7) and any `psql`
session can all apply with no shared tooling.
**Rejected:** Flyway, a schema-migration tool that tracks which files have been applied.
**Why it lost here:** Flyway's value is consistency across environments and repeatable
application over time. This project has one contributor, one environment and one week, so it
would add a dependency to buy a guarantee against a problem that cannot occur yet. Issue #6's
own scope explicitly excludes migration tooling beyond standing the schema up.
**Cost accepted:** no checksums, no applied-migrations table, and no rollback support. Ordering
depends on a filename convention that a careless contributor can break. This is a decision with
a clear expiry date: the first time the schema changes after something is deployed, adopt a tool.
**Numbers are zero-padded for a reason, not for neatness.** Filenames sort as text, and `'0'`
sorts before `'_'`, so an unpadded `V10__` runs *before* `V1__`. This was not theoretical: it
happened while adding V10 and broke every test in the suite, because the trigger file ran against
a database with no tables in it. Padding is what makes the ordering correct under plain
lexicographic sorting, which matters because #7 applies these files through the PostgreSQL
container's init directory, where the sort order is the shell's and not this project's to control.

### Migrations live at the repository root, not inside the Java service

**Chosen:** `db/migrations/` at the repository root **to achieve** a schema owned by the system
rather than by one service.
**Rejected:** `backend/src/main/resources/db/`.
**Why it lost here:** #7's seed script and the Postgres container read the schema directly, and
the Python matching-service will eventually query the same tables. Nesting it under the Java
module would make the Java build the gatekeeper for a shared artifact, forcing #7 to reach
across a service boundary for a file that isn't Java's to own.
**Cost accepted:** the schema has no build tooling of its own, so nothing validates the SQL
except the tests in `backend`.

### The audit log has no foreign keys and cannot be modified

**Chosen:** `audit_logs` with plain `actor_id`/`entity_id` columns carrying **no** foreign keys,
plus a trigger that raises an exception on `UPDATE` or `DELETE`, **to achieve** a record that is
more durable than the data it describes.
**Rejected:** foreign keys to `users` and the audited entity, which is what every other table
in this schema uses; and, for immutability, revoking `UPDATE`/`DELETE` privileges from the
application's database user.
**Why they lost here:** a foreign key forces a choice between cascade (deleting a user erases
the evidence of what they did) and restrict (the audit trail blocks legitimate deletions). Both
are wrong for a record whose purpose is to outlive its subject. Privilege revocation lost for a
narrower reason: the whole system runs as a single database user for the demo, so there is
nothing to separate, and revoking the privilege would also block the seed script.
**Cost accepted:** `entity_id` can point at nothing and a typo'd `entity_type` will be stored
without complaint. This table trades referential integrity for durability, deliberately. And
the immutability rule lives in the database as a trigger, which is the same "logic in the
database" that was rejected for the transporter-role check below.
**Honesty note:** the line now drawn (*integrity properties belong in the database, domain rules
don't*) was articulated after making both decisions, not before. It holds up, but it was not a
principle being applied consistently at the time.

### Ownership roles are enforced in the service layer, not the schema

**Chosen:** `trucks.transporter_id` and `loads.owner_id` reference `users(id)` only, **to achieve**
a guarantee that assets belong to a real user, with the role requirement enforced by #12.
**Rejected:** a trigger validating the referenced user's role, or copying the role into each
asset table so a composite foreign key could enforce it.
**Why they lost here:** the trigger puts a domain rule where a Java team will not look for it;
the denormalised copy creates a second source of truth for a user's role that silently goes
stale the moment a role changes.
**Cost accepted:** writing directly to the database can create a truck owned by a Freight Owner.
Recorded in `known-limitations.md`.

### Aggregate values are derived, never stored

**Chosen:** a user's rating average and all platform metrics are computed by query **to achieve**
numbers that cannot disagree with the rows they summarise. Brief section 3.2 lists "rating" as a
user field and FR-11 asks for basic platform metrics; both are satisfied by aggregation over
`ratings`, `matches` and `loads`.
**Rejected:** a stored `users.rating` column and a dedicated analytics table.
**Why they lost here:** a stored aggregate needs a refresh mechanism that then has to be kept
correct, and the failure mode is silent: a wrong number that looks authoritative. Pre-
aggregation is worth that risk only when the query is too slow, and at demo scale it is not.
**Cost accepted:** every read of a user's rating runs an aggregate query. Irrelevant at this
scale, and the first thing to revisit if it ever isn't.

### Constraints are explicitly named

**Chosen:** named constraints (`loads_owner_id_fkey`, `matches_decision_consistency_check`, …)
**to achieve** tests that can assert on *which* rule rejected a write.
**Rejected:** letting PostgreSQL auto-generate constraint names.
**Why it lost here:** a test asserting only "an exception was thrown" passes against a database
with no tables at all. This was observed during development, where seven such tests would have
gone green against an empty schema had they not asserted on the constraint name.
**Cost accepted:** slightly noisier DDL.

## Extensibility

Only the two directions the project's scope document actually supports are covered here.
Documenting how to build something the brief excludes would be the first step toward building it.

**Multi-tenancy** (multiple companies, with company-to-company relationships). Nothing is built
for this now, and no placeholder column exists. A nullable `company_id` that nothing reads is
worse than no column, because it makes tenancy look handled while `#9`'s authorisation and #17's
admin queries silently fail to filter by it. Adding it later means: a `companies` table, a
`company_id` on `users`, and tenant filtering applied at the user level. Every asset already
reaches its tenant through its owner (`loads.owner_id`, `trucks.transporter_id`,
`ratings.rater_id`), so no per-table tenant column is needed and no existing constraint blocks it.

**Expanded ratings and analytics.** `ratings` already separates the score from its comment and
keys off a match rather than a user pair, so additional dimensions (punctuality, condition on
arrival) are new columns or a child table, not a restructure. Because no aggregate is stored,
new metrics are new queries: #17 can add a metric without a migration.

## Consequences

- The schema requires **PostgreSQL 18**. #7 and #8 must pin that image; an older server fails at
  `V1` on the unknown `uuidv7()` function.
- Constraint behaviour is verified against a real PostgreSQL container (Testcontainers), so the
  test suite requires a running Docker daemon, locally and in CI (#2).
- `matches`, `disputes` and any future decided-state table must write their status and their
  decision metadata in a **single statement**; updating status alone violates the consistency
  CHECK by design.
- `audit_logs` rows cannot be corrected. A mistaken entry is fixed by appending a correcting
  event, never by editing the original. This is intended and should not be "fixed."
- The role-ownership gap and the absence of proximity matching are recorded in
  `known-limitations.md` rather than left to be discovered.
