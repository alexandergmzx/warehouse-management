# Phase 7 Step 2 evidence — authentication slice on the pinned baseline

**Build/configuration identifier:** `5254286+phase7-step2-auth / 2026-07-13T21:15:39-06:00`
(git HEAD `5254286` — the committed Step 1 baseline — plus the Step 2 working
tree; the identifier becomes immutable when Step 2 is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 2 (`PLAN.md`), the
authentication slice that unblocks every later endpoint:

- `POST /api/v1/auth/login` and `POST /api/v1/auth/logout` (API.md);
- opaque bearer tokens (≥256 bits; only the SHA-256 hash stored; bound to one
  user/device pair; 8 h absolute expiry; revoked on logout) per ADR 0005;
- a stateless bearer-token security filter chain;
- the global RFC 9457 `application/problem+json` handler, a stable
  {@code ProblemCode} catalogue, and per-request correlation IDs.

Functional cases covered: **FT-01** (login + idempotent logout), **FT-03**
(malformed request, missing token, insufficient role, validation), **FT-14**
(expired, revoked, inactive-user, inactive-device fail closed).

This record does not claim any picking-workflow behavior (next task, scans,
confirm) or admin endpoints; those are Steps 3–5.

## What was added

| Area | Artifacts |
|---|---|
| `api` (web infra) | `ProblemCode`, `ProblemException`, `ProblemDetailFactory`, `ProblemResponseWriter`, `CorrelationIdFilter`, `GlobalExceptionHandler` |
| `security` | `SecurityConfiguration` (stateless chain), `BearerTokenAuthenticationFilter`, `WmsAuthenticationEntryPoint`, `WmsAccessDeniedHandler` |
| `identity` | `TokenService`, `AuthenticatedUser`, `TokenAuthentication`, `TokenFailure`, `IssuedToken`; `AuthToken` gains an `issue`/`revoke` API; `PickingTaskRepository` gains the device-active-task query |
| `auth` (application/web) | `AuthController`, `AuthenticationService`, `LoginRequest`, `LoginResponse` |
| build | `config/spotbugs/spotbugs-exclude.xml` broadened (see deviations) |

### Design decisions (recorded)

- **Auth orchestration lives in a new `auth` package**, not `identity`. The
  device-assignment-conflict check reads `PickingTaskRepository`, and `picking`
  already depends on `identity`; putting the service in `auth` (which may depend
  on both) avoids an `identity ↔ picking` cycle.
- **Token-time inactive user/device fail closed as `INVALID_TOKEN`.** The bearer
  problem catalogue is `INVALID_TOKEN` / `TOKEN_EXPIRED` / `TOKEN_REVOKED`; a
  token whose user or device was deactivated after issuance is no longer
  acceptable, so it maps to `INVALID_TOKEN` (FT-14).
- **Logout is `permitAll` and idempotent.** It revokes the presented token if it
  matches a live one and always returns `204`, so a repeated logout with an
  already-revoked token still returns `204` rather than a `401` from the filter.
- **Login error order** follows API.md: credentials → user active → device
  registered → device active → device-assignment conflict.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B clean verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13T21:15:39-06:00, total time 55.7 s |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `AuthApiIT` (FT-01 / FT-03 / FT-14 + contract errors) | Tests run 11, failures 0, errors 0 |
| `PersistenceLayerIT` | Tests run 4, failures 0, errors 0 |
| `FlywayMigrationIT` | Tests run 5, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` |

`AuthApiIT` drives the running HTTP server against the migrated container and
asserts, end to end:

- valid picker login returns a `wms_`-prefixed token, `Bearer` type, expiry,
  and user/device summaries; logout twice returns `204` each time (FT-01);
- invalid credentials → `401 INVALID_CREDENTIALS`; admin on the seed's busy
  device → `409 DEVICE_ASSIGNMENT_CONFLICT`;
- malformed JSON → `400 MALFORMED_REQUEST` and the `X-Correlation-Id` is echoed
  in both the body and the response header; missing token → `401 INVALID_TOKEN`;
  picker on an admin path → `403 FORBIDDEN`; blank field → `422 VALIDATION_FAILED`
  with a `fields` object (FT-03);
- expired, revoked, inactive-user, and inactive-device tokens each fail closed
  with the expected stable code (FT-14).

## Deviations and environment notes (Spring Boot 4)

- **Jackson 3 is the Boot 4 default** (`tools.jackson`, 3.1.4), so the injected
  `ObjectMapper` bean is the Jackson 3 type; `ProblemResponseWriter` and the
  test use it. (Jackson 2 remains transitively present via logstash.)
- **`TestRestTemplate` was removed in Boot 4.** `AuthApiIT` uses a plain
  `RestTemplate` with `@LocalServerPort`, and the JDK `HttpClient` request
  factory — the default `HttpURLConnection` does not expose the response body
  for a POST that returns `401`, which the login-failure test asserts on. This
  is a test-client artifact; the server returns the problem body correctly.
- **SpotBugs.** The `EI_EXPOSE_REP2` exclusion was broadened from stereotype-name
  suffixes to the `com.alexandergomez.wms` package (still limited to that one
  Spring dependency-injection false positive; domain/value types use immutables
  or defensive copies). Two other Low findings were fixed in code, not
  suppressed: `ProblemException.extensions` is now a concrete `LinkedHashMap`
  (Serializable, restored on deserialization), and `SecurityConfiguration`
  wraps the builder locally instead of declaring `throws Exception`.
- **Mockito self-attach warning** persists (transitive via
  `spring-boot-starter-test`); no mocks are used yet; benign.

## Residual risk

- `last_used_at` on `auth_token` is not updated on each authenticated request
  (avoids a write on every read); deferred as a future observability refinement.
- The browser-dashboard cookie/CSRF story (ADR 0005 consequences) is a later
  phase; the `v1` REST surface is bearer-only and CSRF-exempt by design.
