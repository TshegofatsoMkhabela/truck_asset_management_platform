# Known Limitations

What is mocked, deferred, or deliberately excluded — and why. Added to as work lands, so that anything incomplete is a **recorded decision** rather than a discovered gap.

## Deliberately simulated

Behaviour that is intentionally faked because a real integration is out of scope.

| Area | What is simulated | Why |
|---|---|---|
| *(none yet — scaffolding only)* | | |

## Deferred to roadmap

Work that is specified but not built, so a team picking this up has the design ready.

| Area | Current state | What a team would need to do |
|---|---|---|
| Cloud deployment | Not built. The brief requires a reproducible **local** setup only, and section 4.1 places production-scale infrastructure out of scope. Images are stateless and env-var driven, so no application change is needed to host them elsewhere | Choose a container host, supply the same environment variables from a managed secrets store, and point `DB_HOST` at a managed Postgres |
| Single entry point / reverse proxy | Not built. Each service is reached directly on its own port, which is sufficient for a local demo | Add a proxy in front of both services if the system is ever exposed beyond localhost — at which point TLS becomes a hard requirement, not an option |
| Server-side secret scanning | Not built. Secrets are blocked locally at commit time by a Gitleaks pre-commit hook ([ADR 0001](adr/0001-local-secret-scanning.md)); nothing re-checks on the GitHub side, so a bypassed or uninstalled hook is not caught | Add a secret-scanning job to CI as a backstop for anyone who skipped local setup or used `--no-verify` |
| Dynamic security scanning (DAST) | Not run. Requires a deployed environment that does not exist, and the brief's security expectation is basic RBAC, validation, password hashing and no committed secrets | Stand up a deployed environment, then add a baseline passive scan before any gating |

## Process limitations

Constraints arising from how the project is being built rather than from the code.

| Limitation | Detail | Recommended change on handoff |
|---|---|---|
| Secret scanning has **no regression test** | The hook was verified by hand when it landed (a fake key rejected, the same file accepted once cleaned), but nothing re-checks it. A later config edit or tooling upgrade could disable it silently, because a passing commit and an unscanned commit look identical | Commit the verification as a script and run it in CI, so a broken scanner fails visibly instead of quietly |
| Secret scanning is **opt-in per clone** | The hook only runs after a contributor runs `pre-commit install`, and `git commit --no-verify` skips it. Nothing verifies that either happened, so the control protects the careless but not the determined | Pair with the CI-side backstop above; a server-side check is the only version of this that cannot be opted out of |
| Pull requests require **0 approving reviews** | `main` is protected and takes changes by PR only, but GitHub does not permit approving your own pull request. On a single-contributor repository, requiring ≥1 approval would make every PR unmergeable except by admin override — which turns the protection into theatre, since the override becomes routine | Raise required approvals to **≥1** as soon as a second contributor exists. The gate structure is already in place; only the count changes |

## Technical debt

| Item | Impact | Suggested fix |
|---|---|---|
| *(none yet)* | | |
