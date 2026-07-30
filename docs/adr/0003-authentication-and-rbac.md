# ADR 0003: Authentication is Spring Security plus a stateless JWT, not a hand-rolled scheme

**Status:** Accepted · **Date:** 30 Jul 2026 · **Issue:** [#9](https://github.com/TshegofatsoMkhabela/truck_asset_management_platform/issues/9)

## Context

The brief's security row requires role-based access control (RBAC), password hashing where
passwords are persisted, and no secrets committed to Git. Issue #9 states the same reasoning
directly: security is the one area where "build it ourselves to save time" is a bad trade,
because a vetted library gets password hashing and session handling right by default, while
hand-rolled auth is a common source of real vulnerabilities.

Every feature issue from #10 onward (Users, Loads, Trucks, Matching, Receipts, Tracking,
Ratings, Admin) depends on this issue landing first, so the shape chosen here is load-bearing
for the rest of the project, not a self-contained feature.

## Decisions

### Spring Security, not a hand-written filter and password check

**Chosen:** Spring Security's `PasswordEncoder` (BCrypt), `AuthenticationManager`, and
`SecurityFilterChain` handle hashing, credential checking, and request-level authorization.

**Rejected:** a hand-written registration/login flow comparing a manually computed hash.

**Why it lost here:** the issue's own stated reasoning holds: getting salting, hash cost
factor, and timing-safe comparison right by hand is exactly the class of mistake a mature
library has already had adversarial review against. There is no requirement here that a
vetted library doesn't already satisfy.

**Cost accepted:** less visibility into exactly what happens inside `BCryptPasswordEncoder`
or the filter chain's internals than a hand-written version would give; acceptable because
that opacity is the library doing its job, not a defect.

### Stateless JWT, not server-side sessions

**Chosen:** a signed JWT (JSON Web Token, a string containing the user id and role, signed so
the server can detect tampering) issued on login, verified on every request by a custom
`JwtAuthenticationFilter`, with no session state kept on the server.

**Rejected:** `HttpSession`-based authentication with a server-side session store.

**Why it lost here:** #8 (dockerized local deploy) runs the backend as a container that may be
restarted or, later, scaled to more than one instance; a session store shared across instances
would need its own infrastructure (e.g. a shared cache) that this one-week MVP has no use for
otherwise. A stateless token needs nothing shared between instances.

**Cost accepted:** a token cannot be revoked before it expires without adding infrastructure
(a denylist, a shorter-lived token plus refresh flow). Not built, since #9 explicitly excludes
production-scale session management and no requirement calls for early revocation.

### Deny-by-default filter chain, not allow-by-default with per-endpoint annotations

**Chosen:** `SecurityConfig` denies every request unless it matches an explicit allow-list
(`/health`, `/`, `/auth/register`, `/auth/login`, the OpenAPI/Swagger routes); everything else
requires authentication, with `@PreAuthorize` layered on top for role checks.

**Rejected:** authenticate nothing by default, and mark individual controller methods
`@PreAuthorize` as they are built.

**Why it lost here:** under the rejected approach, a new endpoint that forgets its annotation
ships unauthenticated by default, silently. Under the chosen approach, a new endpoint that
forgets to be added to the allow-list is unreachable until it is, a build-time inconvenience
rather than a runtime security gap. The failure mode of a fail-closed default is loud; the
failure mode of a fail-open default is silent, and silent is worse in security-relevant code.

**Cost accepted:** every future public endpoint (there are none currently planned beyond the
four already listed) needs a deliberate addition to the allow-list.

### One global exception handler and one authentication entry point, not per-endpoint error handling

**Chosen:** a `@RestControllerAdvice` (`GlobalExceptionHandler`) covers failures inside a
controller; a separate `RestAuthenticationEntryPoint` and `RestAccessDeniedHandler` cover
rejections Spring Security's filter chain makes before a request ever reaches a controller.
Both write the same `ApiError` shape.

**Rejected:** relying on `@RestControllerAdvice` alone.

**Why it lost here:** this was discovered during implementation, not planned in advance.
`@RestControllerAdvice` only intercepts exceptions thrown from within Spring MVC's dispatch,
and Spring Security's filter chain runs before `DispatcherServlet`; a request rejected there
(no token, wrong role) never reaches a controller and so never reaches the advice. The two
extra components exist because the documented error shape has to cover both layers, not
because a second mechanism was preferred in the abstract.

**Cost accepted:** two response-writing code paths (`GlobalExceptionHandler` and the two
Security-layer handlers) have to be kept consistent by hand, since they cannot share Spring
MVC's exception-handling machinery.

### springdoc-openapi 2.8.6, pinned above the version originally planned

**Chosen:** `springdoc-openapi-starter-webmvc-ui` 2.8.6.

**Rejected:** 2.6.0, the version named in the initial plan.

**Why it lost here:** 2.6.0 was compiled against an older Spring Framework internal API
(`ControllerAdviceBean`'s constructor) than the one Spring Boot 3.5.5's parent POM manages,
and failed at runtime with `NoSuchMethodError` the first time an OpenAPI-generating request
actually ran. This was a genuinely reasoned constraint discovered by running the test, not one
identified up front; the plan's original version choice was an unexamined guess.

**Cost accepted:** none beyond the version bump itself; 2.8.6 is the version springdoc's own
compatibility matrix lists for this Spring Boot line.

## Extensibility

The scope doc names no roadmap items beyond what #9 already builds for auth. The one structural
choice worth naming for a future team: `JwtAuthenticationFilter` and `SecurityConfig` isolate
where a token is verified from where roles are declared, so replacing the JWT issuer (for
example, delegating to an external identity provider) would touch `JwtService` and the filter,
not the `@PreAuthorize` annotations scattered across feature controllers. This is not something
requested; it is a property of the layering chosen for the reasons above, noted so a future
contributor does not assume the two are coupled.

## Consequences

Every endpoint built from #10 onward is authenticated and role-checked by default, not as an
afterthought bolted on per controller. A contributor adding a new endpoint must either add it
to `SecurityConfig`'s allow-list (rare, and should be questioned) or rely on the default
`authenticated()` rule plus an explicit `@PreAuthorize` for role restriction. The stateless JWT
choice means no server-side session store exists anywhere in this system; any future
requirement for token revocation before expiry (not currently required) would need new
infrastructure, not a configuration change.
