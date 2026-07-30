# Requirements Traceability

Maps each functional requirement to the code that implements it. Every row starts as **Not implemented** and is updated by the issue that builds it — so a gap here is visible as a gap, never as a missing line.

**Status values:** `Not implemented` · `Partial` · `Complete`

| ID | Requirement | Priority | Implemented by | Tests | Status |
|---|---|---|---|---|---|
| FR-01 | Role-based registration/login for Freight Owner, Transporter and Admin | Must | — | — | Not implemented |
| FR-02 | Basic identity/compliance information and simulated document upload or metadata | Must | `POST /users`, `GET /users/{id}`, `PATCH /users/{id}` (#10) | `UserControllerTest` | Complete |
| FR-03 | Freight Owner can create and view cargo loads | Must | POST/GET/PATCH /loads (`LoadController`, #11) | `LoadControllerTest.createsAndListsLoadForItsOwner`, `getByIdReturnsTheCreatedLoad`, `updatesStatusOnlyWithoutResendingFields`, `postingALoadWritesAnAuditEvent`, `returns404ForUnknownLoadId` | Complete |
| FR-04 | Transporter can create and view available trucks | Must | — | — | Not implemented |
| FR-05 | Rule-based matching using compatibility, location and availability | Must | `matching_service.rules.find_eligible_matches` (matching-service), `POST /loads/{loadId}/matches` (`MatchController`, orchestrator) | `test_rules.py` (6 tests), `test_match.py` (2 tests), `MatchingServiceClientTest`, `MatchingCoordinatorTest` (2 tests), `MatchingTimingE2ETest` | Complete |
| FR-06 | Users can accept or reject a match and the decision is logged | Must | `POST /matches/{matchId}/decision` (`AcceptanceController`) | `AcceptanceCoordinatorTest` (3 tests), `AcceptanceControllerTest` (4 tests) | Complete |
| FR-07 | Accepted match produces a digital confirmation receipt | Must | `GET /matches/{matchId}/receipt` (`AcceptanceController`), issued by `AcceptanceCoordinator.decide(...)` | `ReceiptTest` (2 tests) | Complete |
| FR-08 | Trip tracking simulated using mock coordinates or status progression | Must | `POST /matches/{matchId}/tracking`, `GET /matches/{matchId}/tracking` (`TrackingController`, #15) | `TrackingControllerTest.advancesStatusAndReadsItBackInOrder`, `acceptsAPositionOnlyEvent`, `rejectsAnEventWithNeitherPositionNorStatus`, `refusesTrackingForAMatchThatIsNotAccepted`, `returns404ForAnUnknownMatch` | Complete |
| FR-09 | Parties can rate/review one another after completion | Should | POST/GET /matches/{id}/ratings, GET /users/{id}/ratings (`RatingController`, #16) | `RatingControllerTest.submitsRatingForCompletedMatch`, `listRatingsForMatch` | Complete |
| FR-10 | Admin can manage users, compliance status and flagged/disputed items | Must | `GET /admin/users`, `GET /admin/disputes` (`AdminController`, #17; read-only oversight — action-taking is out of #17's scope) | `AdminControllerTest.adminListsUsersWithComplianceStatus`, `adminListsDisputes`, `nonAdminIsRejectedFromEveryAdminEndpoint` | Complete |
| FR-11 | Admin can view basic platform metrics | Must | `GET /admin/metrics` (`AdminController`, #17) | `AdminControllerTest.adminReadsMetricsMatchingSeededCounts`, `unknownAdminIdIsRejected` | Complete |
| FR-12 | Key actions are available in an audit trail | Must | `GET /admin/audit-logs` (`AdminController`, #17); rows written by #11/#13's controllers | `AdminControllerTest.adminViewsAuditLogEntries` | Complete |

## How to update this table

When an issue implements a requirement, update its row in the **same pull request** as the code:

- **Implemented by** — the endpoint or component, e.g. `POST /api/loads` (`LoadController`)
- **Tests** — the test that proves it, e.g. `LoadControllerTest.rejectsLoadWithoutWeight`
- **Status** — `Complete` only when every part of the requirement works; `Partial` otherwise, with a note in [Known Limitations](known-limitations.md) explaining what is missing
