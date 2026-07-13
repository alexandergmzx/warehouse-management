# Phase 3 design decision packet

**Status:** Phase 3 decisions approved; implementation opened 2026-07-13 (see the validation log §8)
**Prepared:** 2026-07-12; updated 2026-07-13
**Authority:** `PLAN.md` remains the approval source
**Validation evidence:** `docs/research/phase-3-validation-log.md`
**MVP recommendation:** `docs/research/phase-3-mvp-recommendation.md`

## How to use this packet

This document converts the open questions in
`docs/research/phase-2-research.md` into reviewable proposals. A proposal is not
a decision until the project owner records **Approve**, **Approve with the
listed change**, or **Reject** and then explicitly instructs the project to
enter implementation.

Approval of individual rows does not open Phase 4. The gate opens only after
all Phase 3 items are resolved and the owner gives an explicit instruction such
as **“approve the design and begin implementation.”**

## Precondition validation status

Detailed evidence and limitations are recorded in
`docs/research/phase-3-validation-log.md`.

- [x] Record the owner-installed Git, JDK, Maven, IDE, WSL/Docker, and optional
  PostgreSQL-client versions using the Phase 1 evidence commands.
- [x] Recheck every live source in the Phase 2 register and record the review
  date; all 36 registered URLs returned HTTP 200 on 2026-07-12.
- [x] Confirm the supported Spring Boot candidates and their managed dependency
  versions from official Spring documentation and Maven Central metadata.
- [x] Confirm the exact PostgreSQL image's availability and documentation-level
  compatibility with candidate driver, Flyway, and Testcontainers versions.
- [ ] Run Docker engine, disposable-container, image, and component runtime
  validation only after the owner's separate explicit request.
- [x] Record the owner's declaration that no retained or shared database needs
  the provisional `V1__create_schema.sql` history.
- [x] Record the owner's confirmation that Docker Desktop is acceptable under
  workstation policy, resources, virtualization support, and current terms.
- [x] Approve or change the candidate exact baseline, including the proposed
  pgJDBC security override documented in the validation log.

ADRs that depend on these findings remain proposed. Completed factual checks do
not approve the remaining decisions or open Phase 4.

## Decision summary

| ID | Topic | Proposed direction | Owner response |
|---|---|---|---|
| D-01 | Technology baseline | Temurin Java 21.0.11+10; Maven 3.9.16; Spring Boot 4.0.7; PostgreSQL 17.10; Flyway 11.14.1; pgJDBC 42.7.13; Testcontainers 2.0.5 | Approve with listed change — 2026-07-12 |
| D-02 | Development database route | Docker Compose primary; native PostgreSQL documented fallback | Approve — 2026-07-12 |
| D-03 | JVM language | Java only | Approve — 2026-07-13 |
| D-04 | Persistence | JPA for ordinary persistence; `JdbcTemplate`/native SQL for locking-heavy paths | Approve with MVP recommendation — 2026-07-13 |
| D-05 | Allocation model | Availability derived from on-hand minus unfinished task allocations | Approve with MVP recommendation — 2026-07-13 |
| D-06 | Transactions and locks | `READ COMMITTED`, explicit row locks, path-specific canonical lock order | Approve with MVP recommendation — 2026-07-13 |
| D-07 | Administrative recovery | Audited admin block/resume; no HHT skip and no direct SQL repair | Approve with MVP recommendation — 2026-07-13 |
| D-08 | Authentication | Argon2id passwords and hashed opaque, revocable, expiring tokens | Approve with MVP recommendation — 2026-07-13 |
| D-09 | API and idempotency | Versioned REST, RFC 9457 errors, retry-safe scans and final confirmation | Approve with MVP recommendation — 2026-07-13 |
| D-10 | Domain and audit model | Approve the proposed entities/invariants plus append-only task transitions | Approve with MVP recommendation — 2026-07-13 |
| D-11 | Logging and correlation | Structured JSON; request correlation ID carried into movements/events | Approve with MVP recommendation — 2026-07-13 |
| D-12 | Configuration and secrets | Profile model from research; explicit typed startup validation | Approve with MVP recommendation — 2026-07-13 |
| D-13 | Testing and evidence | Layered Maven tests plus numbered functional evidence and retained reports | Approve with MVP recommendation — 2026-07-13 |
| D-14 | Provisional migration | No retained database reported; replace provisional V1 with the approved clean baseline only after the design gate | Approve — 2026-07-12 |
| D-15 | Labels and dashboard | Existing QR prefixes; deterministic generation; server-rendered admin view with polling | Approve with MVP recommendation — 2026-07-13 |
| D-16 | MFC seam | Immutable completion message and no-op adapter only; no transport/retry code | Approve with MVP recommendation — 2026-07-13 |
| D-17 | Scope exclusions | Preserve the exclusions listed below | Approve with MVP recommendation — 2026-07-13 |

## Detailed proposals

### D-01 — Supported technology baseline

**Owner response (2026-07-12): Approve with the listed change.** Use the
conservative validated candidate and override the Boot-managed pgJDBC 42.7.11
because the vendor identifies 42.7.12 as the fix for CVE-2026-54291.

**Proposal**

- Java language/runtime: Eclipse Temurin Java 21.0.11+10 LTS.
- Build: Maven 3.9.16.
- Spring Boot: 4.0.7, selected as the conservative supported 4.x line rather
  than adopting the newly current provisional 4.1.0 solely because it is newer.
- PostgreSQL: 17.10 using the official `postgres:17.10-alpine` image; record an
  immutable image reference before implementation/runtime acceptance.
- Flyway: 11.14.1; Testcontainers: 2.0.5; Hibernate ORM: 7.2.19.Final, as
  managed by Boot 4.0.7.
- PostgreSQL JDBC: explicitly override Boot's 42.7.11 management with 42.7.13.
- GitHub Actions: pin reviewed actions to immutable commit SHAs and document the
  update process.

**Why:** This preserves the Java 21 portfolio target, deliberately selects a
supported patched line instead of the newest line by default, and excludes the
vendor-identified vulnerable driver version from the approved baseline.

**Alternative:** Select a newer supported Spring line for a demonstrated
feature. This requires an explicit compatibility reason and evidence, not
novelty alone.

**Approval evidence:** official version/support links, access date, managed
dependency report, workstation versions, and later Maven/CI verification.

### D-02 — Development PostgreSQL route

**Owner response (2026-07-12): Approve.** Runtime validation remains deferred
until the owner's separate explicit request.

**Proposal:** Use Docker Compose as the primary local database route if the
preconditions are satisfied. Keep PostgreSQL bound to loopback. Document native
Windows PostgreSQL as a fallback, not a simultaneously running second server.

**Why:** Compose offers a reproducible server/image and simpler disposable test
setup. The native route remains necessary for policy, licensing, resource, or
virtualization constraints.

**Approval condition:** The owner confirms the workstation route after
reviewing current Docker terms and system requirements.

### D-03 — JVM language

**Owner response (2026-07-13): Approve.** The owner explicitly confirmed that
Java only is acceptable for the MVP.

**Proposal:** Use Java only.

**Why:** Java directly supports the portfolio goal and avoids compiler-order,
nullability, JPA plugin, and CI complexity with no identified Kotlin-specific
benefit. Kotlin remains possible only through a later ADR with measurable
value.

### D-04 — Persistence approach

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Use Spring Data JPA for ordinary entity persistence and focused
reads. Use named `JdbcTemplate` operations or narrowly scoped native SQL for
task claims, allocation availability, explicit row locking, reconciliation,
and other PostgreSQL-specific paths. Keep transaction boundaries in application
services.

**Why:** The hybrid keeps routine mapping concise while making lock-sensitive
SQL visible and testable. It avoids introducing jOOQ code generation into this
small PoC.

**Rejected default:** Hiding `SKIP LOCKED` and multi-row allocation entirely
behind generic repository methods.

### D-05 — Availability and reservation model

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Define:

```text
available(article, location)
    = stock.quantity
    - sum(requested quantity of unfinished tasks for that article/location)
```

Order creation locks candidate stock rows in canonical
`(article_id, location_id)` order, recalculates this value while locks are held,
and either creates every line/task or creates nothing. Do not add a second
mutable `allocated_quantity` balance initially.

**Why:** Released tasks are already durable reservation records. Avoiding a
second balance reduces dual-write reconciliation, while stock locks prevent two
order-creation transactions from accepting the same availability.

**Required proof:** concurrent order creation, complete rollback on shortage,
multi-bin ordering, and query-plan/index evidence.

### D-06 — Transactions, isolation, and canonical lock order

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Use PostgreSQL `READ COMMITTED` with explicit locks. Publish these
path rules:

1. Allocation: lock stock rows by `(article_id, location_id)`, then create the
   order, lines, and tasks.
2. Claim: lock one available task through the complete FIFO query with
   `FOR UPDATE OF task SKIP LOCKED`, then assign before commit.
3. Confirmation: lock the task, validate ownership/state/idempotency, lock its
   stock row, then update line and order progression in child-to-parent order.
4. Adjustment/receipt: lock stock rows by `(article_id, location_id)`.
5. Recovery: lock the task first; lock stock only if the approved operation
   changes a stock reservation, then update line/order in the same order as
   confirmation.

No path may acquire a task after holding a stock row when another approved path
can acquire that same task before stock. Retries are bounded and limited to
explicitly approved transient SQL states.

**FIFO definition:** oldest currently claimable work. `SKIP LOCKED` may bypass
a locked older task, so stuck/age monitoring is mandatory.

### D-07 — Administrative recovery

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Add an auditable `BLOCKED` task state and append-only transition
history.

- An administrator may block an active task with a required reason, releasing
  its user/device assignment without changing stock.
- An administrator may resume a blocked task to `AVAILABLE` after the cause is
  resolved; scan confirmations are cleared and the original FIFO fields remain.
- Recovery cannot alter requested quantity, fabricate a confirmation, decrement
  stock, or delete history.
- The HHT exposes no skip/block/resume operation.
- Direct SQL state repair is prohibited.

**Alternative requiring owner direction:** admin reassignment to a named
picker/device. It adds operational value but also assignment and authorization
complexity; it is not proposed initially.

### D-08 — Authentication and token lifecycle

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Hash passwords with Argon2id using parameters measured and
recorded on the target workstation. On login, issue a cryptographically random
opaque token, store only its SHA-256 hash, bind it to one active user/device,
and apply a configurable absolute expiry. Logout is idempotent and records
revocation. Inactive users/devices and revoked/expired tokens fail closed.

Never log clear passwords, password hashes, or bearer tokens. Demo credentials
exist only in development fixtures.

**Alternative:** bcrypt if the approved dependency/performance review rejects
Argon2id; record the work factor and reason.

### D-09 — HHT API, errors, and idempotency

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Retain `/api/v1`, opaque bearer authentication, correlation IDs,
and the compact workflow. Replace the provisional custom error envelope with
RFC 9457 Problem Details plus stable `code`, `correlationId`, and safe detail
extensions.

- Repeating a successful location scan with the same expected value after an
  ambiguous response returns success/current state without regressing state.
- Repeating a successful article scan behaves the same way.
- A wrong scan always fails without transition.
- Final confirmation retains an HHT-generated UUID. Same ID and same canonical
  payload returns the original task/movement result without another decrement;
  same ID with different task/payload returns `CONFIRMATION_ID_REUSED`.
- Exact quantity remains mandatory; partial confirmation is rejected.
- Recovery endpoints are admin-only and follow D-07.

**Required follow-up:** Rewrite `API.md` only after approval and define every
problem type, status, retry instruction, allowed detail field, log event, and
test case.

### D-10 — Domain, state, and audit model

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Approve the entity outline and INV-01 through INV-17 in the Phase
2 synthesis, amended by D-07 and an append-only `task_transition` record for
claim, scan, block, resume, and completion transitions.

The approved task states would be:

```text
AVAILABLE -> ASSIGNED -> LOCATION_CONFIRMED -> ARTICLE_CONFIRMED -> COMPLETED
                 |                |                   |
                 +----------------+-------------------+-> BLOCKED
BLOCKED -> AVAILABLE  (admin resume only)
```

Order and line state names, timestamps, and database checks must be specified
before revising the migration. Every movement reference must remain consistent
with task, line, order, article, and location.

### D-11 — Logging, event fields, and correlation

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Emit one-line structured JSON to standard output. Use MDC for
request correlation and the Phase 2 event catalogue as the minimum field set.
For a stock-changing HTTP transaction,
`stock_movement.correlation_id` equals the request correlation ID. Give the
movement and task transition their own durable primary IDs as well.

Apply allow-list logging: never serialize authorization headers, tokens,
passwords, hashes, full exception internals in client responses, or arbitrary
request bodies. Define retention outside the application.

### D-12 — Configuration and secret handling

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Approve the Phase 2 configuration matrix as the starting model.
Bind `wms.*` values to typed validated properties and fail startup on missing,
malformed, or unsafe required preproduction values. Record for each property:
owner, default, environment, sensitivity, allowed range, and restart need.

Development may use ignored `.env` values and known fixtures. Preproduction
must not scan development fixture locations or have committed credentials.
Binding the API to a LAN interface and opening a firewall rule require an
explicit runbook step and approved subnet/profile scope.

### D-13 — Testing, diagnostics, CI, and evidence

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Approve the Phase 2 traceability draft and use:

- JUnit unit tests through Surefire;
- focused Spring slice tests where they add boundary evidence;
- Failsafe plus pinned PostgreSQL Testcontainers for migration, repository,
  transaction, API, concurrency, and idempotency tests;
- numbered manual cases for Windows, LAN, firewall, backup/restore, rollback,
  and incident diagnosis;
- read-only SQL diagnostics validated against deterministic fixtures;
- immutable build/config identifiers and retained machine-readable and human
  reports.

CI uses least privilege, immutable action references, Maven dependency caching,
quality gates, and approved artifact retention. Compilation alone is never
acceptance evidence.

### D-14 — Provisional V1 migration policy

**Owner response (2026-07-12): Approve.** The owner reports no retained or
shared database requiring the provisional V1 history. Replace it only with the
approved clean baseline after the design gate; no migration edit is authorized
by this decision alone.

**Proposal:** First inventory all databases:

- If no shared or retained database has applied provisional V1, record that
  evidence, destroy only disposable databases, and replace V1 with an approved
  clean baseline.
- If any meaningful database has applied it, preserve V1 unchanged and add
  forward migrations.
- Do not use Flyway `repair` to conceal checksum drift or an incorrect schema.

**Owner input recorded:** no existing database/volume history must be retained.
The stopped Docker engine prevented independent volume inspection; runtime
validation remains explicitly deferred.

### D-15 — QR labels and dashboard

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Retain exact case-sensitive payloads `LOC:<location-code>` and
`ART:<sku>`. Approve deterministic QR dimensions, margin, error correction,
PNG encoding, page geometry, and an embedded redistributable font before Phase
8 implementation. Record the chosen library versions/licences in the technology
ADR.

Use a server-rendered administration dashboard with a small authenticated JSON
polling endpoint rather than a separate frontend build. Propose a configurable
two-second development polling interval and admin-only access; finalize browser
token/cookie handling with the authentication ADR.

### D-16 — MFC extension seam

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Define an application port receiving an immutable message with
`eventId`, `orderId`, `orderNumber`, and `completedAt`. The event ID is the
idempotency identifier. Select a no-operation adapter by configuration and
verify with a test fake that one completion is published.

No socket, TCP library, telegram class, scheduler, retry loop, transport queue,
or delivery claim is included. Future transport ownership and reliability are
documented only.

### D-17 — Explicit exclusions

**Owner response (2026-07-13): Approve with the MVP recommendation.**

**Proposal:** Exclude from the initial PoC:

- wave planning and route optimization;
- partial picks, short picks, HHT skip, and automatic timeout release;
- FEFO/lot/serial handling and replenishment;
- a separate SPA/mobile build;
- Kotlin without a later ADR;
- cloud deployment and a log aggregation platform;
- direct HHT database access;
- robot control, sockets, TCP telegram delivery, schedulers, and transport
  retries;
- destructive database reset as rollback or incident recovery.

## ADRs to write after owner decisions

Do not mark these accepted in advance. Consolidate related decisions rather
than creating one ADR per minor parameter:

1. Supported build, runtime, database, and local-development baseline
   (D-01 through D-03 and D-14).
2. Persistence, allocation, transaction, and lock model (D-04 through D-06).
3. Domain states, administrative recovery, and audit model (D-07 and D-10).
4. Authentication, API errors, and idempotency (D-08 and D-09).
5. Configuration, observability, testing, and evidence standards
   (D-11 through D-13).
6. Dashboard, label, and MFC extension contracts (D-15 and D-16).
7. Scope exclusions (D-17), either as an ADR or an approved specification
   section referenced by all ADRs.

## Owner review record

Recorded decisions:

```text
Decision ID: D-01
Response: Approve with change
Change/reason: Use Spring Boot 4.0.7 as the conservative supported line and
  override managed pgJDBC 42.7.11 with 42.7.13 because 42.7.12 contains the
  vendor-documented fix for CVE-2026-54291.
Evidence or constraint: docs/research/phase-3-validation-log.md; runtime proof
  and immutable PostgreSQL image reference remain pending.
Reviewer: Project owner
Date: 2026-07-12

Decision ID: D-02
Response: Approve
Change/reason: None.
Evidence or constraint: Docker is acceptable as the primary route; runtime
  validation requires a separate explicit request.
Reviewer: Project owner
Date: 2026-07-12

Decision ID: D-03
Response: Approve
Change/reason: Use Java only for the MVP; do not introduce Kotlin.
Evidence or constraint: Explicit owner response on 2026-07-13; a future Kotlin
  change would require its own ADR and verification.
Reviewer: Project owner
Date: 2026-07-13

Decision ID: D-14
Response: Approve
Change/reason: Follow the no-retained-database branch.
Evidence or constraint: Owner reports no retained/shared database or volume;
  replace provisional V1 only after the complete design gate is approved.
Reviewer: Project owner
Date: 2026-07-12

Decision IDs: D-04 through D-13, D-15 through D-17
Response: Approve with change
Change/reason: Approve the Phase 3 MVP recommendation and its documented
  conservative defaults for persistence, allocation, locking, recovery,
  authentication, API semantics, domain states, logging, configuration,
  testing, labels, dashboard, MFC seam, and scope exclusions.
Evidence or constraint: `docs/research/phase-3-mvp-recommendation.md`; the
  recommendation authorizes ADR and specification preparation but does not
  authorize implementation. Runtime validation and acceptance evidence remain
  pending.
Reviewer: Project owner
Date: 2026-07-13
```

The owner approved D-04 through D-13 and D-15 through D-17 on 2026-07-13,
using `docs/research/phase-3-mvp-recommendation.md`. The owner also authorized
preparation of the consolidated ADRs and specifications. This authorization
does not authorize implementation.

For future decision changes, record:

```text
Decision ID:
Response: Approve | Approve with change | Reject
Change/reason:
Evidence or constraint:
Reviewer:
Date:
```

## Gate checklist

- [ ] Preconditions and live-source checks are complete.
- [x] D-01 through D-17 have owner responses.
- [x] Exact technology versions are recorded.
- [x] Workflow/state model and entity relationships are approved.
- [x] Stock/allocation/task/movement/idempotency invariants are approved.
- [x] Transaction and lock boundaries are approved.
- [x] Administrative recovery and authentication are approved.
- [x] API/error/idempotency semantics are approved.
- [x] Configuration, logging, testing, evidence, and runbook standards are
  approved.
- [x] Migration V1 policy and database inventory are recorded.
- [x] Explicit exclusions are approved.
- [x] Required ADRs are written with statuses matching the owner's decisions.
- [x] Accepted API, requirements-traceability, and functional-test specifications
  are prepared.
- [ ] The project owner gives a separate explicit instruction to begin
  implementation.

Until every item above is satisfied, implementation remains blocked.
