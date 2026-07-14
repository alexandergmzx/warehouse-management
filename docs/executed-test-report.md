# Executed functional-test report

**Template use:** copy the row structure below for each execution pass (a
full regression before a release, a rehearsal after a runbook change, etc.);
keep prior passes rather than overwriting them, so the report shows history.
A case is **Passed** only with a retained, citable evidence artifact —
compilation, "it looked fine," or an unrecorded manual check do not count
(CLAUDE.md, `docs/functional-test-specification.md`).

**Statuses:** Passed / Failed / Blocked / Not Applicable. A case blocked by
an unavailable runtime is recorded as Blocked, never as Passed.

## Execution pass: 2026-07-14 (Phases 7–9)

**Build/configuration identifier:** git HEAD as of each cited evidence file
(see individual rows); toolchain: Maven 3.9.16, Eclipse Temurin 21.0.11,
Docker Desktop 29.6.1, `postgres:17.10-alpine@sha256:742f40ea…`.
**Operator:** Claude (Alexander Gomez's assistant), per the owner's
implementation authorization (`PLAN.md`).

| ID | Case | Status | Evidence |
|---|---|---|---|
| FT-01 | Login with valid picker/device and logout | Passed | `AuthApiIT`, `docs/evidence/2026-07-13-phase7-step2-auth.md` |
| FT-02 | Create an order requiring two stock bins | Passed | `OrderAllocationApiIT`, `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md` |
| FT-03 | Malformed request, missing token, insufficient role | Passed | `AuthApiIT`, `docs/evidence/2026-07-13-phase7-step2-auth.md` |
| FT-04 | Concurrent claim and one-active-task-per-user/device | Passed | `PickingNegativePathApiIT`, `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` |
| FT-05 | Scan wrong location and wrong article | Passed | `PickingNegativePathApiIT`, `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` |
| FT-06 | Scan correct location/article in order, replay-safe | Passed | `PickingApiIT`, `docs/evidence/2026-07-13-phase7-step3-picking-happy-path.md` |
| FT-07 | Confirm zero/over/partial quantity rejected | Passed | `PickingNegativePathApiIT`, `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` |
| FT-08 | Confirm exact quantity, repeat same UUID | Passed | `PickingApiIT`, `docs/evidence/2026-07-13-phase7-step3-picking-happy-path.md` |
| FT-09 | Block an assigned task and resume it | Passed | `TaskRecoveryApiIT`, `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md` |
| FT-10 | Line/order complete only when all approved work is done | Passed | `OrderLifecycleApiIT`, `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md` |
| FT-11 | Attempt to update/delete a stock movement | Passed | `FlywayMigrationIT.movementLedgerRejectsUpdatesAndDeletes`, `docs/evidence/2026-07-13-phase6-maven-verify.md` |
| FT-12 | Reuse confirmation UUID with different payload | Passed | `PickingNegativePathApiIT`, `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` |
| FT-13 | Reconcile stock against movement deltas | Passed | `PersistenceLayerIT` (clean fixture: Step 1; injected discrepancy: Step 5), `docs/evidence/2026-07-13-phase7-step1-persistence.md`, `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md` |
| FT-14 | Expired/revoked/inactive-user/inactive-device access | Passed | `AuthApiIT`, `docs/evidence/2026-07-13-phase7-step2-auth.md` |
| FT-15 | Preprod with missing/unsafe required configuration | Passed | `PreprodConfigurationValidatorIT`, `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md` |
| FT-16 | Stock-changing request and log inspection | Passed | `StructuredLoggingApiIT`, `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md` |
| FT-17 | Generate the same location/article labels twice | Passed | `LabelApiIT`, `docs/evidence/2026-07-14-phase8-steps0-2.md` |
| FT-18 | Open the admin dashboard and observe polling | Passed | `DashboardApiIT` + manual browser pass, `docs/evidence/2026-07-14-phase8-steps0-2.md` |
| FT-19 | Inspect the MVP surface for excluded features | See 2026-07-14 (Phase 10) pass below | — |

**Totals (this pass):** 18 Passed, 0 Failed, 0 Blocked, 0 Not Applicable
(FT-19 executed in the Phase 10 pass below rather than left blocked, since it
completed the same day).

### Notes

- Every Passed row above corresponds to an automated `mvn verify` execution
  (Testcontainers-backed for FT-01–FT-14/FT-16/FT-17/FT-18; no database for
  FT-15) — see the cited evidence file for the exact command, toolchain
  versions, and raw result counts for that step.
- FT-18's dashboard-refresh criterion additionally required a real browser
  pass beyond the HTTP-level integration test (`docs/evidence/2026-07-14-phase8-steps0-2.md`,
  "Manual browser verification" section) — the automated test alone missed a
  real defect (a `/default-ui.css` cross-chain 401) that the browser pass
  caught.

## Execution pass: 2026-07-14 (Phase 10 — MFC seam and FT-19 closure)

**Build/configuration identifier:** see `docs/evidence/2026-07-14-phase10-mfc-seam.md`.

| ID | Case | Status | Evidence |
|---|---|---|---|
| FT-19 | Inspect the MVP surface for excluded features | Passed | Code-inspection review: no HHT skip endpoint (`grep` across `picking`/`admin` finds only an unrelated SQL `SKIP LOCKED` clause and the explicit "no skip" doc comment on `TaskStatus`), no partial-pick path (`confirm()` still requires exact quantity, `QUANTITY_MISMATCH` otherwise), no direct HHT database access (the HHT talks only to `/api/v1/hht/**`, per `API.md`/`PickingController`), no scheduler (`@Scheduled`/`TaskScheduler`/`@EnableScheduling`: zero matches), no raw socket (`java.net.Socket`/`ServerSocket`: zero matches), no transport-retry library (`spring-retry`/`resilience4j`/`@Retryable`: zero matches). `docs/evidence/2026-07-14-phase10-mfc-seam.md` |

**Totals (this pass):** 1 Passed, 0 Failed, 0 Blocked, 0 Not Applicable.

**Combined FT-01–FT-19 status: 19 Passed, 0 Failed, 0 Blocked, 0 Not
Applicable.**
