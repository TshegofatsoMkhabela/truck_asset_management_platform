# Requirements Traceability

Maps each functional requirement to the code that implements it. Every row starts as **Not implemented** and is updated by the issue that builds it — so a gap here is visible as a gap, never as a missing line.

**Status values:** `Not implemented` · `Partial` · `Complete`

| ID | Requirement | Priority | Implemented by | Tests | Status |
|---|---|---|---|---|---|
| FR-01 | Role-based registration/login for Freight Owner, Transporter and Admin | Must | — | — | Not implemented |
| FR-02 | Basic identity/compliance information and simulated document upload or metadata | Must | `POST /users`, `GET /users/{id}`, `PATCH /users/{id}` (#10) | `UserControllerTest` | Complete |
| FR-03 | Freight Owner can create and view cargo loads | Must | — | — | Not implemented |
| FR-04 | Transporter can create and view available trucks | Must | — | — | Not implemented |
| FR-05 | Rule-based matching using compatibility, location and availability | Must | — | — | Not implemented |
| FR-06 | Users can accept or reject a match and the decision is logged | Must | — | — | Not implemented |
| FR-07 | Accepted match produces a digital confirmation receipt | Must | — | — | Not implemented |
| FR-08 | Trip tracking simulated using mock coordinates or status progression | Must | — | — | Not implemented |
| FR-09 | Parties can rate/review one another after completion | Should | — | — | Not implemented |
| FR-10 | Admin can manage users, compliance status and flagged/disputed items | Must | — | — | Not implemented |
| FR-11 | Admin can view basic platform metrics | Must | — | — | Not implemented |
| FR-12 | Key actions are available in an audit trail | Must | — | — | Not implemented |

## How to update this table

When an issue implements a requirement, update its row in the **same pull request** as the code:

- **Implemented by** — the endpoint or component, e.g. `POST /api/loads` (`LoadController`)
- **Tests** — the test that proves it, e.g. `LoadControllerTest.rejectsLoadWithoutWeight`
- **Status** — `Complete` only when every part of the requirement works; `Partial` otherwise, with a note in [Known Limitations](known-limitations.md) explaining what is missing
