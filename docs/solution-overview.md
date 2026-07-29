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
| Orchestrator / API | Java 17, Spring Boot, Maven | Spring Security is the intended auth and RBAC mechanism, so the web layer is chosen to match it rather than be rewritten later |
| Matching service | Python 3.11+, FastAPI | Built-in request validation, generated OpenAPI docs, and a first-class test client — three separate add-ons in the alternatives considered |
| Database | PostgreSQL | *To be confirmed when the data model lands* |
| Local runtime | Docker Compose | *To be confirmed when containerisation lands* |

## Major components

| Component | Responsibility |
|---|---|
| `backend/` | Public API surface, authentication and RBAC, persistence, orchestration of calls to matching |
| `matching-service/` | Given one load and a set of candidate trucks, return the eligible subset with a human-readable reason per match |

Two services rather than one, because matching is the part most likely to be replaced later (rule-based today, potentially model-based tomorrow). Keeping it behind a network boundary means that replacement does not touch the API or persistence layers.

## Key design decisions

Recorded as ADRs in [`docs/adr/`](adr/) as they are made.
