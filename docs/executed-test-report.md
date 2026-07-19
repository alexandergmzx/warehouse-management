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

## Final acceptance sweep: 2026-07-14

**Build/configuration identifier:** git HEAD `f3cdee0`. Full details:
`docs/evidence/2026-07-14-final-acceptance-sweep.md`.

Re-confirms `mvn -B verify` (33/33 tests, 0 Checkstyle, 0 SpotBugs) on the
same commit, then closes the two items still open after the Phase 10 pass:

| Item | Result |
|---|---|
| Phase 6 residual: SQL diagnostic pack executed against a running dev database | All four queries return results matching documented expectations (stuck task, zero stock/ledger discrepancy, full DEMO-1003 trace, integrity overview) |
| Final-acceptance item: runbook rehearsal, evidenced | Fresh `git clone` → `mvn package -DskipTests` → packaged jar started in `preprod` profile against a freshly created empty database → health `UP`; only the schema migration applied, zero demo users, no secret leakage. Recorded caveat: pre-installed toolchain (not a literal fresh machine); firewall/cross-machine LAN steps excluded as out of scope |

Also exercised, live, beyond the existing automated suite: HHT login/claim/
logout, dashboard session login and polling (including the `stuck` flag
matching diagnostic query 1), byte-identical label regeneration against the
retained Phase 8 sample, structured-log-to-ledger correlation-ID matching,
and preprod fail-fast on both a missing variable and the committed dev
password.

**No new FT status changes** (all 19 cases already Passed); this pass closes
the plan's open acceptance items rather than re-testing functional cases.

## Execution pass: 2026-07-15 (HHT loopback integration — first real client)

**Build/configuration identifier:** git HEAD `494a760` plus the
`V1_2__seed_hht_demo_picker.sql` working tree; HandheldPi repo HEAD `512ef48`
plus its Phase 3 working tree. Full details:
`docs/evidence/2026-07-15-hht-loopback-integration.md`.

The HandheldPi client (real `HttpWmsClient` + sqlite offline queue + picking
state machine) ran end-to-end against a live dev instance on this machine —
the first external client ever to exercise the `/api/v1` HHT surface. WiFi
loss simulated by killing a local TCP proxy; 44/44 driver checks passed;
post-run SQL reconciliation clean (0 stock/ledger mismatches, exactly one
`PICK` movement per completed task).

| ID | Case (re-exercised over a real client) | Status | Evidence |
|---|---|---|---|
| FT-01 | Login with valid picker/device and logout (badge+PIN → `picker02`/`HHT-DEV-01`) | Passed | evidence §3 case 1 |
| FT-04 | One-active-task and `DEVICE_ASSIGNMENT_CONFLICT` on a busy device | Passed | evidence §3 case 8 |
| FT-05 | Wrong location / wrong article / out-of-order scans | Passed | evidence §3 case 2 |
| FT-06 | Correct scans in order, replay-safe redelivery | Passed | evidence §3 cases 1, 6 |
| FT-07 | Non-exact quantity rejected (`422 QUANTITY_MISMATCH`) | Passed | evidence §3 case 3 |
| FT-08 | Confirm exact quantity; same-UUID retry idempotent; reuse rejected | Passed | evidence §3 case 4 |
| FT-10 | Admin block/resume recovery loop after a rejected offline replay | Passed | evidence §3 case 7 |
| — | New client-side coverage: Level 2 offline store-and-forward, dead-letter + SYNC_FAILED, token-revocation re-login, claim/logout guards, correlation-ID join device↔server | Passed | evidence §3 cases 4, 6, 7, 9, 10 |

Residual: Stage 3 (physical GamePi20 over WiFi, runbook §3–4 firewall
exercise) remains open — requires the device and the owner at the LAN.

## Execution pass: 2026-07-19 (MFC work package)

**Build/configuration identifier:** git HEAD `e01b88c` plus the MFC
implementation working tree. Full details:
`docs/evidence/2026-07-19-mfc-work-package.md`.

`./mvnw -B verify`: 42/42 tests, 0 Checkstyle violations, 0 SpotBugs
findings.

| ID | Case | Status | Evidence |
|---|---|---|---|
| FT-19 | Inspect the MVP surface for excluded features (re-scoped: a scheduler and a retry loop are now in-scope, MFC-package artifacts, not exclusions) | Passed | evidence "FT-19 re-scope" section: no `java.net.Socket`/`ServerSocket`, no message-broker dependency anywhere in the tree |
| FT-20 | Order completion queues exactly one PENDING TRANSPORT mission; an incomplete order queues none | Passed | `MfcTelegramLifecycleIT.completingAnOrderQueuesExactlyOnePendingTransportMission`, `.incompleteOrderQueuesNoMission` |
| FT-21 | Dispatch: immediate success, one failure then success, exhaustion to FAILED | Passed | `MfcTelegramLifecycleIT.dispatchDeliversRetriesAndEventuallyExhausts` |
| FT-22 | WCS confirmation lifecycle: ACCEPTED → COMPLETED, idempotent replay, illegal transition, unknown mission, unauthenticated call | Passed | `MfcTelegramLifecycleIT.wcsConfirmationLifecycle` |
| FT-23 | SORT mission confirmation | Passed | `MfcTelegramLifecycleIT.wcsConfirmationLifecycle` (SORT branch) |
| FT-24 | Telegram adapter refuses to start without base-url / without transport locations | Passed | `MissionDispatcherFailFastIT` (both methods) |

**Totals (this pass):** 6 Passed, 0 Failed, 0 Blocked, 0 Not Applicable.

**Combined FT-01–FT-24 status: 25 Passed, 0 Failed, 0 Blocked, 0 Not
Applicable.**

Residual: gate 5 (`PLAN.md`) — the consumer proof against a scripted
`agv-fleet-controller` stand-in — is tracked separately in
`docs/evidence/2026-07-19-mfc-transport-loop.md`.
