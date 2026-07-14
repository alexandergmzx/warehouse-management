# Phase 7 Step 3 evidence — picking happy path on the pinned baseline

**Build/configuration identifier:** `5110c80+phase7-step3-picking / 2026-07-14T04:41:13Z`
(git HEAD `5110c80` — the committed Step 2 baseline — plus the Step 3 working
tree; the identifier becomes immutable when Step 3 is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 3 (`PLAN.md`): the
picking happy path — atomic FIFO claim, location/article scans, and the
exact-quantity confirm transaction (ADR 0003, ADR 0004, API.md).

- `GET /api/v1/hht/tasks/next` — returns the caller's current active task, or
  atomically claims the next `AVAILABLE` task in global FIFO order via
  `FOR UPDATE OF t SKIP LOCKED`;
- `POST /api/v1/hht/tasks/{taskId}/scan-location` and `.../scan-article` —
  advance the task on a correct scan; a repeated correct scan replays without
  regressing state;
- `POST /api/v1/hht/tasks/{taskId}/confirm` — the exact-quantity transaction
  that locks task then stock and atomically updates task, stock, one
  `stock_movement`, one `task_transition`, the order line, and the order.

Functional cases covered: **FT-06** (scan progression + replay-safety) and
**FT-08** (exact-quantity confirm creates one movement; repeating the same
confirmation UUID and quantity replays the original result without a second
decrement). This record does not claim the dedicated negative/concurrency/
idempotency-reuse test suite (FT-04, FT-05, FT-07, FT-12) — that is Step 4 —
though the production code implementing those checks (wrong scan, quantity
mismatch, confirmation-ID reuse, insufficient stock) is already in place,
since a correct confirm transaction cannot be written without it.

## What was added

| Area | Artifacts |
|---|---|
| `picking` | `PickingController`, `PickingService`, `PickingJdbcRepository` (FIFO claim), DTOs (`NextTaskResponse`, `ScanRequest`, `ScanLocationResponse`, `ScanArticleResponse`, `ConfirmRequest`, `ConfirmResponse`) |
| entity mutators | `PickingTask.assignTo/confirmLocation/confirmArticle/complete`; `OrderLine.markInProgress/addPickedQuantity/markCompleted`; `CustomerOrder.markInProgress/markCompleted`; `Stock.decrease`; static factories `TaskTransition.record`, `StockMovement.pick` |
| repositories | Pessimistic-write `findByIdForUpdate` on `PickingTaskRepository`, `OrderLineRepository`, `CustomerOrderRepository`, `StockRepository`; active-task lookup and completion-count queries |
| `api` | New `ProblemCode`s: `TASK_NOT_FOUND`, `TASK_NOT_ASSIGNED_TO_USER`, `INVALID_TASK_STATE`, `WRONG_LOCATION`, `WRONG_ARTICLE`, `TASK_ASSIGNMENT_CONFLICT`, `CONFIRMATION_ID_REUSED`, `INSUFFICIENT_STOCK`, `QUANTITY_MISMATCH`; `CorrelationIdFilter` gains a no-arg, MDC-based accessor for service-layer use |

### Design decisions (recorded)

- **Lock order: task → stock → order line → customer order.** ADR 0003
  explicitly mandates task-then-stock. The order-line and customer-order locks
  are this slice's addition: a single order line can be split across multiple
  tasks (multi-bin picking, already exercised by the seed fixture), and two
  different pickers can legitimately confirm two different tasks of the same
  line concurrently. Without a lock, the `picked_quantity` read-modify-write
  is a genuine lost-update race (unlike `markInProgress`, which is idempotent
  and would tolerate a lost update harmlessly). The order is fixed and
  hierarchical (detail → resource → parent → grandparent) so any future code
  touching the same four resources together can follow the same order without
  introducing a deadlock risk.
- **Scan replay is scoped to the exact prior state, not "any later state."**
  A correct location scan repeated while the task is still `ASSIGNED`
  transitions normally; repeated once already `LOCATION_CONFIRMED` with the
  *same* QR value replays (`replayed: true`, no timestamp change); any other
  combination (wrong value while already confirmed, or a scan attempted from
  `ARTICLE_CONFIRMED`/`COMPLETED`/`BLOCKED`) is `INVALID_TASK_STATE` rather
  than `WRONG_LOCATION`/`WRONG_ARTICLE`, since those codes describe a scan
  attempt at the *expected* step, not a scan repeated after the workflow has
  moved on. Same structure for article scans.
- **Token-time inactive user/device is not re-checked here.** That is already
  enforced by `TokenService.authenticate` (Step 2) on every request via the
  bearer filter, so picking code can assume an authenticated caller is active.
- **`confirmationId` replay detection relies on the schema, not extra state.**
  `picking_task.confirmation_id` is only ever non-null once `status =
  'COMPLETED'` (database check constraint), so "is this a replay" reduces to:
  task already `COMPLETED` and the presented ID matches the stored one. A
  matching ID with a different quantity is `409 CONFIRMATION_ID_REUSED`
  (FT-08's replay case is proved here; FT-12's dedicated reuse-with-different-
  payload evidence is Step 4). A `DataIntegrityViolationException` on the
  unique `confirmation_id` index (cross-task ID reuse) is also mapped to
  `CONFIRMATION_ID_REUSED` as defense in depth.
- **`remainingStock` and the nested order state are read fresh, not frozen at
  first confirmation.** API.md states the order state may have "advanced" by
  retry time; the same freshness principle is applied uniformly to
  `remainingStock`, since further movements (e.g. a future admin adjustment)
  could otherwise make a replay response misleading.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B clean verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13T22:41:05-06:00, total time 1:15 min |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `PickingApiIT` (FT-06 + FT-08) | Tests run 1, failures 0, errors 0 |
| `AuthApiIT` | Tests run 11, failures 0, errors 0 |
| `PersistenceLayerIT` | Tests run 4, failures 0, errors 0 |
| `FlywayMigrationIT` | Tests run 5, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` (21 tests total) |

`PickingApiIT` drives the running HTTP server against the migrated container
with a freshly inserted, isolated operator (the seed fixture's only free user
is `admin`, and its only device is already busy with `picker01`'s seeded
active task, so the test provisions its own picker/device via JDBC and reads
back whichever task `GET /next` actually returns rather than hard-coding a
task number — keeping the test independent of method execution order). It
asserts, end to end:

- claim: `GET /next` claims the oldest `AVAILABLE` task and returns `ASSIGNED`;
  a second `GET /next` with no state change returns the *same* task, proving
  "at most one active task per user";
- scan-location: wrong QR → `409 WRONG_LOCATION`, no state change; correct QR
  → `LOCATION_CONFIRMED`; repeating the same correct QR → `replayed: true`,
  same state, no regression;
- scan-article: same pattern against `WRONG_ARTICLE` / `ARTICLE_CONFIRMED`;
- confirm: exact quantity → `COMPLETED`, one movement, stock decremented by
  exactly the confirmed quantity, verified independently via direct SQL;
- idempotent retry: the same confirmation UUID and quantity returns the
  identical `movementId` and `remainingStock` with **no** second row in
  `stock_movement` for that task (verified via `SELECT count(*) ... WHERE
  picking_task_id = ?` before and after the retry).

## Deviations and notes

- **Flush-ordering bug found and fixed during this slice.** The first
  `mvn verify` run failed `ft06AndFt08_claimScanConfirmAndReplaySafely` with a
  `409` where `200` was expected; the server log showed the database trigger
  rejecting the `stock_movement` insert (`23514`, "movement resulting quantity
  does not match current stock"). Root cause: `stock.save(stockRow)` does not
  force an immediate `UPDATE`, while the subsequent `stockMovements.save(...)`
  *does* execute immediately (Hibernate must insert right away for an
  `IDENTITY`-strategy entity to obtain its generated ID) — so the movement
  insert could race ahead of the stock decrement becoming visible, tripping
  the trigger that validates `resultingQuantity` against the row's current
  value. Fixed by changing to `stock.saveAndFlush(stockRow)`, matching the
  already-correct pattern used for the task-completion write. Recorded per the
  failed-experiment rule; no schema or trigger change was needed — the trigger
  behaved exactly as designed and caught a real application-layer ordering bug.
- No other deviations from Steps 0–2's established patterns (Jackson 3,
  `RestTemplate` + JDK `HttpClient` factory, the broadened `EI_EXPOSE_REP2`
  SpotBugs exclusion).

## Residual risk

- `TASK_ASSIGNMENT_CONFLICT`'s concurrent-claim race (two simultaneous `GET
  /next` calls from the same user/device both attempting to claim different
  tasks) is implemented (catches the unique-index violation) but not yet
  proved under actual concurrent load — that dedicated concurrency evidence
  (FT-04) is Step 4's scope.
- `INSUFFICIENT_STOCK` and `QUANTITY_MISMATCH` are implemented per the
  contract but not yet exercised by a dedicated test — Step 4 (FT-07).
