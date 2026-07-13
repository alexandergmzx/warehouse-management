# Phase 3 MVP recommendation

**Status:** Recommended for owner approval; not an implementation authorization  
**Prepared:** 2026-07-13  
**Authority:** `PLAN.md` and `docs/research/phase-3-decision-packet.md`

## Purpose

Define the smallest credible WMS deliverable that demonstrates Java, SQL,
configuration, functional testing, diagnostics, and operational documentation
without weakening the confirmed workflow or introducing enterprise-scale
features.

The owner has approved Java only (D-03). All other recommendations below remain
proposals until the owner records a response in the Phase 3 decision packet.
Approval of this recommendation still requires the separate Phase 3 gate
instruction before implementation starts.

## Recommended MVP boundary

### Include

1. Article, location, stock, user, device, order, line, picking-task, movement,
   task-transition, and authentication-token records.
2. Administrative creation and viewing of required master data, stock receipts
   and adjustments, order creation, task visibility, and task block/resume.
3. Atomic deterministic order allocation across bins.
4. HHT login/logout, next-task claim, location scan, article scan, and exact
   quantity confirmation through `/api/v1`.
5. Retry-safe scans and idempotent final confirmation.
6. Stock and movement reconciliation diagnostics.
7. A minimal authenticated server-rendered dashboard with polling.
8. Deterministic location/article QR PNG and A4 PDF labels.
9. Structured operational logs, validated configuration, automated tests,
   numbered functional tests, retained evidence, and Windows runbooks.
10. An order-completion publisher interface with a no-operation adapter only.

### Exclude

Adopt D-17 unchanged: no wave planning, route optimization, partial/short picks,
HHT skip, automatic timeout release, FEFO, lot/serial processing,
replenishment, SPA/mobile build, Kotlin, cloud deployment, log platform, direct
HHT database access, robot control, TCP/telegram implementation, transport
retries, or destructive reset as operational recovery.

## Recommended decisions

| Decision | MVP recommendation | Reason |
|---|---|---|
| D-04 | Approve unchanged | JPA keeps ordinary persistence concise; named JDBC/native SQL makes locking and reconciliation explicit. |
| D-05 | Approve unchanged | Unfinished tasks are the durable reservations; avoid a second mutable allocation balance. |
| D-06 | Approve unchanged | `READ COMMITTED` with explicit locks is proportionate and testable. |
| D-07 | Approve unchanged | Admin block/resume is the minimum safe recovery path; reassignment is deferred. |
| D-08 | Approve with the defaults below | Opaque revocable tokens fit a LAN PoC and are simpler than JWT. |
| D-09 | Approve with the response rules below | RFC 9457 and explicit idempotency produce a stable HHT contract. |
| D-10 | Approve with the states and audit fields below | Exact states remove migration ambiguity. |
| D-11 | Approve JSON | One-line JSON provides machine-readable diagnostics without requiring a log platform. |
| D-12 | Approve with the Phase 2 matrix and ranges below | Typed startup validation prevents unsafe preproduction defaults. |
| D-13 | Approve unchanged with retention below | The portfolio requires concurrency, migration, functional, and operational evidence. |
| D-15 | Approve with deterministic defaults below | Server rendering avoids a second toolchain; fixed QR/PDF settings make output reproducible. |
| D-16 | Approve unchanged | Preserves the future seam without pretending to deliver transport. |
| D-17 | Approve unchanged | Keeps the PoC focused and consistent with `PLAN.md`. |

## Smart defaults that close specification gaps

### Domain states and progression

- Order: `OPEN`, `IN_PROGRESS`, `COMPLETED`.
- Order line: `OPEN`, `IN_PROGRESS`, `COMPLETED`.
- Task: `AVAILABLE`, `ASSIGNED`, `LOCATION_CONFIRMED`,
  `ARTICLE_CONFIRMED`, `BLOCKED`, `COMPLETED`.
- Order and line become `IN_PROGRESS` when their first task is claimed.
- A line becomes `COMPLETED` only when all its tasks are complete; an order
  becomes `COMPLETED` only when all its lines are complete.
- Blocking preserves the task reservation and physical stock, releases its
  assignment, and does not complete or cancel work.
- Resuming returns the task to `AVAILABLE`, clears scan-confirmation timestamps,
  preserves allocation/FIFO fields, and appends an audit transition.
- Cancellation and deletion of released work are outside the MVP.

Each mutable business record uses `created_at` and `updated_at`; task records
also use `assigned_at`, `location_confirmed_at`, `article_confirmed_at`,
`blocked_at`, and `completed_at` where applicable. A task transition records its
own ID, task ID, previous/new state, timestamp, actor user, device when
applicable, required reason for administrative recovery, correlation ID, and
confirmation ID when applicable. Transition and movement records are
append-only.

### Authentication defaults

- Argon2id is the preferred password hash. Measure its parameters on the target
  workstation before fixture generation; target approximately 250 ms and at
  least the current OWASP minimum. Record the selected memory, iteration, and
  parallelism values in the authentication ADR and configuration evidence.
- Tokens contain at least 256 random bits, are returned only once, and only a
  SHA-256 token hash is stored.
- Absolute token lifetime defaults to eight hours with no sliding extension.
- One active token is allowed per user/device pair; a new login revokes the
  previous token for that pair. Logout and repeated logout are idempotent.
- Preproduction transport requires HTTPS at the deployment boundary; exposing
  bearer authentication over unencrypted non-loopback HTTP is not accepted.

The measured Argon2id cost is deliberately not invented before performance
evidence exists. If the accepted minimum cannot meet the target workstation
budget, record an explicit security decision rather than silently weakening it.

### API and retry defaults

- Use RFC 9457 `application/problem+json` with `type`, `title`, `status`, stable
  `code`, `correlationId`, and only endpoint-approved safe extensions.
- A repeated correct scan returns HTTP 200 with the current task representation
  and `replayed: true`; it never moves state backward.
- A wrong scan returns the mapped 4xx problem and changes no state.
- A confirmation request requires a client-generated UUID.
- Same UUID plus the same canonical task, quantity, and payload returns HTTP 200
  with the original task and movement IDs and `replayed: true`.
- Reusing the UUID with different canonical content returns HTTP 409 and
  `CONFIRMATION_ID_REUSED`.
- Quantity mismatch, assignment conflict, invalid state, and insufficient stock
  are non-retryable business responses. Ambiguous network failure and server
  5xx responses may be retried with the same confirmation UUID.

Exact endpoint schemas and the complete error/status catalogue must be written
into `API.md` after approval and before endpoint implementation.

### Configuration and logging defaults

- Adopt the Phase 2 configuration matrix.
- Validate ports in `1..65535`, positive durations, token TTL in
  `PT5M..PT24H`, and stuck-task threshold in `PT1M..P7D`.
- The `preprod` profile requires externally supplied database credentials and
  auth/security values and must not load development fixtures.
- Hibernate performs schema validation only; Flyway owns schema changes.
- Emit one JSON object per line to standard output with UTC ISO-8601 timestamps,
  event name, level, build/config ID, correlation ID, and the applicable IDs
  from the Phase 2 event catalogue.
- Use allow-listed fields. Never log authorization headers, bearer tokens,
  passwords, password hashes, database secrets, arbitrary bodies, or unsafe
  exception details.

### Testing and evidence defaults

- Unit tests run with Surefire; integration tests ending in `IT` run with
  Failsafe against the pinned PostgreSQL Testcontainer.
- Required evidence includes positive, negative, migration, constraint,
  concurrency, idempotency, recovery, reconciliation, profile-isolation, and
  API-contract coverage.
- Every requirement maps to numbered automated or manual tests.
- CI retains test and diagnostic reports for 30 days; final portfolio acceptance
  evidence is retained under an immutable build/configuration identifier.
- Compilation alone is never acceptance.

### Labels and dashboard defaults

- QR payloads remain exact and case-sensitive: `LOC:<location-code>` and
  `ART:<sku>`.
- Use ZXing directly with error correction M, a four-module quiet zone, UTF-8,
  black on white, and deterministic 300-by-300-pixel PNG output without
  timestamps or variable metadata.
- Use PDFBox for A4 PDF output with fixed page geometry and an embedded,
  licence-reviewed redistributable font. Record the exact font file and licence
  before implementation; do not depend on a workstation font.
- The dashboard is admin-only, server-rendered, and uses a configurable
  two-second polling default. Browser authentication uses an `HttpOnly`,
  `SameSite=Strict` cookie, `Secure` outside loopback development, and CSRF
  protection for mutating administration requests.

## Delivery slices after gate approval

1. **Rebaseline:** write ADRs, classify provisional artifacts, update the Maven
   baseline, replace the unapplied provisional V1, and establish configuration.
2. **Database foundation:** schema, fixtures, migration/integrity tests, and SQL
   diagnostics.
3. **Core workflow:** allocation, claim, scans, confirmation, movements,
   idempotency, and recovery.
4. **Administration and presentation:** master-data/order endpoints, dashboard,
   and deterministic labels.
5. **Operational acceptance:** logging, CI, functional evidence, backup/restore,
   installation, rollback, LAN, and incident runbooks.
6. **Extension seam:** no-op order-completion publisher and test fake.

Each slice must satisfy its applicable `PLAN.md` acceptance gate before the next
slice is considered complete.

## Approval path

If the recommendations are acceptable, the owner should explicitly:

1. approve D-04 through D-13 and D-15 through D-17 using this document's listed
   defaults and constraints;
2. authorize the consolidated ADRs and specifications to be written;
3. review those records and the completed Phase 3 gate checklist; and only then
4. issue the separate instruction: **“Approve the design and begin
   implementation.”**

No source, migration, configuration, API, build, container, or runtime change is
authorized by this recommendation alone.

