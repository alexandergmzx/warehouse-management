# PLAN — MFC work package

The single approved new-scope package for this repository (owner approval
2026-07-18, per `../ECOSYSTEM.md` v3 — ecosystem sequencing step 2). The ten
delivered PoC phases are closed and evidenced (`README.md`,
`docs/executed-test-report.md`); this plan governs only the material-flow-
control (MFC) extension. All repository rules in `CLAUDE.md` apply unchanged.

## Repo alignment (2026-07-19)

`CLAUDE.md` and this file carry the ECOSYSTEM v3 update. Alongside them,
`README.md`, `handheld-plan.md`, `docs/architecture.md`, and `API.md` were
brought current with the v3 ecosystem role and the approved MFC scope
(status refresh, transport-neutral seam wording, consumer notes) — no
behavioral change, docs only. `../HandheldPi` carries the matching update on
its side (its `PLAN.md` "Ecosystem role" section).

## Purpose

Close the WMS → WCS loop: when the WMS decides work needs a vehicle, it emits
an MFC mission telegram that `agv-fleet-controller` (the WCS) can dispatch;
mission outcomes return as confirmations via the existing REST surface. This
is what ecosystem demo #1 (fully virtual closed loop) depends on from this
repository.

## Work items

### 1. TELEGRAMS.md — the contract (first deliverable)

Authored and owned here; `agv-fleet-controller` pins a version.

- Mission types: **TRANSPORT** (tote, source location → destination location)
  and **SORT** (parcel, induction station → chute). TRANSPORT is specified
  and implemented first; SORT is specified but marked stubbed.
- Content per mission: ids, order/line references, mission states and the
  allowed transitions, and the confirmation semantics (delivered via REST).
- Ecosystem contract rules: semantic versioning, consumers pin, integration
  friction is a contract defect first, example payloads double as test
  fixtures.
- Open contract question (from `../ECOSYSTEM.md` v3's bin-full handling,
  where the WCS itself spawns a TRANSPORT mission to swap a full bin): can a
  mission originate WCS-side, or is mission creation always WMS-issued? v1
  answer may be "reserved, not implemented" — decide explicitly in
  `TELEGRAMS.md`, not by silent omission.

### 2. Telegram sender

- Implemented behind the existing ADR 0007 `OrderCompletionPublisher` seam —
  a real adapter beside the no-op one, selected by configuration (documented
  in `docs/configuration-matrix.md` per the parameter rules).
- Transport chosen by a new ADR (`docs/decisions/`); per the confirmed
  workflow baseline this is **not** a raw TCP telegram implementation.
- Delivery must respect the existing invariants: emission is consistent with
  the atomic stock/task/order commit (outbox-style dispatch or equivalent —
  design decision for the ADR), and no telegram contradicts the append-only
  ledgers.

### 3. Mission endpoints (TRANSPORT first)

- REST endpoints for the WCS side of the loop: mission status/confirmation
  ingestion correlated to orders/tasks, consistent with the existing
  `/api/v1` conventions (auth, problem+json, correlation ids, structured
  logging fields).
- SORT endpoints: specified in `API.md`/`TELEGRAMS.md`, implemented as
  documented stubs — visible, honest, and versioned, not silent.

## Constraints

- No schema change outside a new versioned Flyway migration; applied
  migrations stay immutable.
- The v1 HHT surface must remain untouched — two client generations
  (HandheldPi, `:app-picker`) depend on it unchanged.
- Proportionality rule stands: this remains a PoC-scale extension, not a
  message-broker showcase.

## Acceptance gates (evidence-based, per repository rules)

1. `TELEGRAMS.md` v1 published with example payloads that load as test
   fixtures.
2. New numbered functional test cases extending
   `docs/functional-test-specification.md`, covering: telegram emitted
   exactly once per qualifying commit, no emission on rollback, confirmation
   ingestion updates the correlated order/task, SORT stub behavior.
3. `docs/requirements-traceability.md` updated; evidence retained under
   `docs/evidence/` with a build identifier.
4. `mvn -B verify` green on the pinned toolchain; zero new Checkstyle/
   SpotBugs findings.
5. Consumer proof: `agv-fleet-controller` (or, until it exists, a scripted
   stand-in kept in that repo's name) consumes a TRANSPORT telegram and
   returns a confirmation end-to-end — the evidence entry cited from both
   repositories, as with the 2026-07-15 HHT loopback run.

## Status: all five gates satisfied (2026-07-19)

1. `TELEGRAMS.md` v1.0.0 published; fixtures load and parse (`TelegramFixturesIT`).
2. `docs/functional-test-specification.md` FT-20–FT-24 cover emission
   (exactly-once, none on an incomplete order), dispatch/retry/exhaustion,
   confirmation ingestion (including idempotent replay and illegal
   transitions), and the SORT stub.
3. `docs/requirements-traceability.md` R-21–R-25 added; evidence in
   `docs/evidence/2026-07-19-mfc-work-package.md` and
   `docs/evidence/2026-07-19-mfc-transport-loop.md`.
4. `mvn -B verify`: 43/43 tests, 0 Checkstyle, 0 SpotBugs
   (`docs/evidence/2026-07-19-mfc-work-package.md`).
5. Consumer proof executed against `scripts/wcs-standin/wcs_standin.py`
   (acting in `agv-fleet-controller`'s name, per this gate's own allowance):
   two orders completed end-to-end, telegram delivered, `ACCEPTED` →
   `COMPLETED` confirmed, idempotent replay and illegal-transition rejection
   verified live, zero stock/ledger discrepancies
   (`docs/evidence/2026-07-19-mfc-transport-loop.md`). That run also found
   and fixed a real defect (a `@Scheduled`/`@Transactional` self-invocation
   bug the Testcontainers suite could not have caught) — recorded in the
   evidence file per the "record failed experiments" rule, not folded in
   silently.

`agv-fleet-controller` itself remains out of ecosystem sequencing scope
(`../ECOSYSTEM.md` step 3) — the stand-in is the approved substitute until it
exists, per this gate's own wording.
