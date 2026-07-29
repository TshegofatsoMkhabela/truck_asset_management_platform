# ADR 0001 — Secrets are blocked at commit time, by a hook the repo carries

**Status:** Accepted · **Date:** 29 Jul 2026 · **Issue:** [#4](https://github.com/TshegofatsoMkhabela/truck_asset_management_platform/issues/4)

## Context

The assessment brief's security row requires that no secrets are committed to Git. That
requirement has an awkward property: it cannot be satisfied after the fact. Once a secret
reaches a commit, removing it does not un-expose it — the value must be treated as
compromised and rotated, because the blob may already exist in a clone, a fork, or a CI log.
Detection after the fact is therefore not a weaker version of prevention; it is a different,
and worse, thing.

So the control has to sit *before* the commit object exists.

This repository is about to grow exactly the material that gets leaked this way: #7 adds a
Dockerized database with credentials, #8 adds an `.env` approach for DB and JWT signing
keys, and #9 adds authentication. The hook is worth more installed before that work than
after it.

## Decision

Scan staged changes at commit time using **Gitleaks**, managed by the **`pre-commit`**
framework, configured by two committed files:

- `.pre-commit-config.yaml` — pins Gitleaks to `v8.30.1`
- `.gitleaks.toml` — extends the default ruleset and holds project exceptions

## Alternatives considered

**A hand-written `.git/hooks/pre-commit` shell script.** Rejected. `.git/hooks/` is not
version-controlled and is not cloned, so the script would have to be distributed to each
contributor by hand and could never be updated centrally. The mechanism that makes a hook
*shareable* is the entire reason it survives past its author.

**A `repo: local` hook calling a system-installed `gitleaks` binary.** Rejected. It avoids
the toolchain build described below, but only by moving the problem: every contributor must
then install the right Gitleaks version themselves, with nothing checking that they did.
Version drift in a security control is silent by nature — the hook still runs, just with
different rules than anyone reviewed.

**Server-side scanning in CI only.** Rejected as the *primary* control, for the reason in
Context: by the time CI sees the commit, the secret has been pushed. It remains a reasonable
second layer, and #4 explicitly defers it.

## Consequences

**Accepted cost — a Python dependency on a project that is half Java.** Contributors need
Python to install `pre-commit`. This is a genuine cost, not a nominal one, and it is accepted
because the rejected alternative's cost — hooks that cannot be distributed or updated — is
worse.

**Accepted cost — a slow first run.** The Gitleaks hook is a Go repository. On a machine
without Go, `pre-commit` downloads a Go toolchain and compiles Gitleaks, which takes minutes.
If that happens inside a `git commit`, it looks like a hang. The README therefore documents
`pre-commit install-hooks` as an explicit setup step so the cost is paid visibly, once.
Subsequent runs take roughly 100ms.

**Known gap — this is a local control only.** `git commit --no-verify` bypasses it, and a
contributor who never ran `pre-commit install` is not covered at all. This is recorded in
[`known-limitations.md`](../known-limitations.md). It is a deliberate boundary of #4, not an
oversight: the CI-side backstop that would close it is explicitly out of scope for this issue.

**Observed property — a broken config fails closed.** An invalid `.gitleaks.toml` causes the
hook to exit non-zero and the commit to be rejected, rather than passing unscanned. Verified
during #4 by writing an invalid config.
