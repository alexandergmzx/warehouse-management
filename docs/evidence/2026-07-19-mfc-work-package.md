# MFC work package evidence — telegram contract, sender, mission endpoints

**Build/configuration identifier:** git HEAD `e01b88c` (the committed ADR
0011 + TELEGRAMS.md baseline) plus the MFC implementation working tree; the
identifier becomes immutable when this change is committed.

## Scope

Objective evidence for the MFC work package (`PLAN.md`, owner approval
2026-07-18 per `../ECOSYSTEM.md` v3): TRANSPORT telegram emission behind the
ADR 0007 `OrderCompletionPublisher` seam, a transactional-outbox dispatcher
with retry/backoff, WCS-facing mission confirmation endpoints, the SORT stub,
and the fail-fast configuration checks ADR 0011 specifies. This also closes
the FT-19 scope re-review the new scheduler/retry loop requires (see below).

## What was added

| Area | Artifacts |
|---|---|
| `docs/decisions/0011-…` | Transport decision: transactional outbox + HTTP push (Spring `RestClient`, `@Scheduled`), compared against WCS-pull, a broker, and raw TCP |
| `TELEGRAMS.md` | v1.0.0 contract: TRANSPORT (implemented) and SORT (specified, stubbed) telegrams, mission lifecycle/transitions, confirmation semantics, the WCS-originated-mission open question recorded as reserved |
| `src/test/resources/telegrams/` | Four example payloads, doubling as test fixtures (`TelegramFixturesIT`) |
| `db/migration/V2__create_mfc_missions.sql` | `mfc_mission` (transactional outbox) + append-only `mfc_mission_transition`; additive `WCS` value on `app_user.role`'s check constraint |
| `db/devdata/V2_1__seed_wcs_client.sql` | Dev-only `wcs01`/`AGV-FC-01` WCS credentials, `MFC-90-01`/`MFC-90-02` handover locations |
| `identity.UserRole` | `WCS` role added |
| `mfc` package (new production classes) | `MissionType`, `MissionState`, `MfcMission`, `MfcMissionTransition`, `MfcMissionRepository`, `MfcMissionTransitionRepository`, `MfcMissionJdbcRepository` (FOR UPDATE SKIP LOCKED claim, mirroring `picking.PickingJdbcRepository`), `TelegramOrderCompletionPublisher` (the real `OrderCompletionPublisher` adapter), `MissionDispatcher` (`@Scheduled` poller + `@Transactional` per-mission dispatch), `TelegramPayload`, `MissionConfirmationRequest/Response`, `MissionConfirmationService`, `MissionConfirmationController`, `MissionDetailResponse`, `MissionQueryService` |
| `admin.MfcAdminController` | `GET /api/v1/admin/mfc/missions/{id}` diagnostic read |
| `api.ProblemCode` | `MISSION_NOT_FOUND`, `INVALID_MISSION_STATE`, `SORT_NOT_IMPLEMENTED` |
| `security.SecurityConfiguration` | `POST /api/v1/mfc/missions/*/confirmations` restricted to `ROLE_WCS` |
| `configuration.MfcProperties`, `application.yml`, `docs/configuration-matrix.md` | `wms.mfc.adapter=telegram`, `.telegram.base-url/.retry-interval/.max-attempts`, `.transport.source-location/.destination-location` |
| `WarehouseManagementApplication` | `@EnableScheduling` |
| tests | `MfcTelegramLifecycleIT` (5 methods), `TelegramFixturesIT` (2), `MissionDispatcherFailFastIT` (2) — 9 new integration tests |

### Design decisions (recorded)

- **Fail-fast lives in `@PostConstruct`, not the constructor.** `MissionDispatcher.dispatchNextOnce()` needs `@Transactional`, which requires Spring to CGLIB-proxy the class — incompatible with making it `final` (the SpotBugs-recommended fix for `CT_CONSTRUCTOR_THROW`). Validating required configuration in `@PostConstruct` instead keeps the class proxyable, avoids a partially-constructed object on failure, and still fails application startup exactly as ADR 0011 requires — proved by `MissionDispatcherFailFastIT`, not just asserted.
- **`MissionDetailResponse.transitions()` defensively copies** (`List.copyOf`) rather than exposing the constructor argument directly (`EI_EXPOSE_REP`).
- **Claiming the dispatch queue reuses the exact `FOR UPDATE SKIP LOCKED` pattern** `picking.PickingJdbcRepository` already uses for FIFO task claims, rather than inventing a second locking idiom.
- **Every test method that creates an `mfc_mission` row drains it to a non-`PENDING` terminal state before returning** (`MfcTelegramLifecycleIT`). Discovered by running the suite: a `PENDING` row left behind by one test method was claimed by a *later* method's `dispatchNextOnce()` call (FIFO claim order is global, not scoped per test), corrupting its assertions. The fix — self-cleanup per method — is the same principle the existing `incompleteOrderQueuesNoMission` test already had to apply to picking-task FIFO pollution (never leave a task `AVAILABLE`), now applied to the mission queue too.
- **Retry-interval doubles as the `@Scheduled` tick period and the individual mission's backoff duration.** Tests set it deliberately long (`PT600S`) so the background scheduler cannot race explicit `dispatchNextOnce()` calls, then fast-forward `next_attempt_at` directly via JDBC (the same technique `backdateOrder()` already uses for FIFO control) to exercise retries deterministically without a real 600-second wait.
- **Devdata seed renumbered `V1_3` → `V2_1`.** Flyway merges `db/migration` and `db/devdata` into one version-ordered sequence when both locations are active (dev/test); a `V1_3` seed depending on `V2`'s schema (the `WCS` role) would run *before* `V2` and fail. Caught by running the suite, not assumed — `V1__create_schema.sql` and the two already-applied devdata files are untouched.

## Verification

`./mvnw -B verify` on this working tree (PostgreSQL `17.10-alpine`, digest-pinned, via Testcontainers):

```
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
Checkstyle: 0 violations
SpotBugs: 0 bugs, 0 errors (effort=Max, threshold=Low)
BUILD SUCCESS
```

42 = the previously-evidenced 33 (`docs/evidence/2026-07-15-hht-loopback-integration.md`'s baseline) plus 9 new: `MfcTelegramLifecycleIT` (5), `TelegramFixturesIT` (2), `MissionDispatcherFailFastIT` (2). Two pre-existing integration tests needed count updates for the new schema, not behavior changes: `FlywayMigrationIT` (3→5 applied migrations, 3→4 Argon2 credentials — `wcs01` added) and `PersistenceLayerIT` (5→7 seeded locations — `MFC-90-01`/`MFC-90-02` added).

## FT-19 re-scope

FT-19's expected result previously excluded "a scheduler" and "transport
retry" outright — accurate on 2026-07-14, before any MFC package was
approved. The approved MFC package legitimately adds both
(`@EnableScheduling`, `MissionDispatcher`'s retry/backoff loop), so FT-19 is
re-scoped, not re-run against its old wording: the exclusions that still
hold are a raw TCP telegram socket and a message broker (both confirmed
absent by the same code-inspection technique as the 2026-07-14 pass — no
`java.net.Socket`/`ServerSocket`, no `spring-rabbit`/`spring-kafka`/broker
dependency anywhere in `pom.xml` or the source tree). `docs/functional-test-specification.md` and `docs/executed-test-report.md` carry the corrected wording.

## What was not exercised here

Gate 5 (`PLAN.md`) — an end-to-end run against a scripted `agv-fleet-
controller` stand-in — is separate evidence
(`docs/evidence/2026-07-19-mfc-transport-loop.md`), not covered by the
Testcontainers suite above (which uses an in-process JDK stub receiver, not
a standalone process authenticating over the real LAN-shaped flow).

**Addendum:** that gate-5 run found a real defect the 42/42 pass above could
not have caught — a `@Scheduled`/`@Transactional` self-invocation bug in
`MissionDispatcher.dispatchPending()` (every test here called
`dispatchNextOnce()` directly on the proxy, never exercising the scheduled
entry point itself). Fixed by self-injecting the proxy
(`@Lazy MissionDispatcher self`); a new regression test,
`MfcTelegramLifecycleIT.scheduledDispatchLoopRunsEachMissionInATransaction`,
drives the actual `@Scheduled` path. Full suite after the fix: **43/43**,
0 Checkstyle, 0 SpotBugs. Details in
`docs/evidence/2026-07-19-mfc-transport-loop.md`.
