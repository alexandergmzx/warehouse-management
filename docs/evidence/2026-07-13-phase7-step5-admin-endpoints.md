# Phase 7 Step 5 evidence — admin endpoints on the pinned baseline

**Build/configuration identifier:** `180dbe0+phase7-step5-admin / 2026-07-14T05:30:04Z`
(git HEAD `180dbe0` — the committed Step 4 baseline — plus the Step 5 working
tree; the identifier becomes immutable when Step 5 is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 5 (`PLAN.md`), the
largest remaining slice: every `/api/v1/admin/**` endpoint (API.md).

- `POST /admin/orders` — order creation with atomic multi-bin allocation
  (availability = `stock.quantity − Σ unfinished task quantity`, ascending
  `(article_id, location_id)` lock order, ascending location-code draw order);
- `GET /admin/orders/{orderNumber}` — order/line/task detail read;
- `GET /admin/tasks` — dashboard-facing task list with `state`/`orderNumber`/
  `assignedUsername`/`stuckOnly` filters;
- `POST /admin/tasks/{taskId}/block` and `.../resume` — administrative
  recovery (ADR 0004);
- `POST /admin/articles`, `POST /admin/locations` — catalog master data;
- `POST /admin/stock/adjustments` — signed manual stock corrections.

Functional cases covered: **FT-02** (two-bin allocation succeeds in ascending
location order; a shortage rolls back the entire order), **FT-09** (block
audits the required reason and releases assignment; resume returns
`AVAILABLE`; neither changes stock), **FT-10** (a line completes only when all
its tasks complete, and the order only when all its lines complete — proved
with an order that must NOT complete after its first of two lines finishes),
**FT-13** (the reconciliation diagnostic, already built in Step 1, correctly
flags an injected discrepancy — the "clean fixture" half was already evidenced
in Step 1).

## What was added

| Area | Artifacts |
|---|---|
| `admin` (new package) | `OrderAdminController` + `OrderAdminService` (create/allocate + detail read); `TaskAdminController` + `AdminTaskQueryService` + `AdminTaskJdbcRepository` (list) — block/resume delegate to `PickingService`; `CatalogAdminController` + `CatalogAdminService` (articles/locations); `StockAdminController` + `StockAdminService` (adjustments); DTOs for all of the above |
| `picking` | `PickingService` gains `block`/`resume`; new DTOs `BlockTaskRequest`, `BlockTaskResponse`, `ResumeTaskResponse`; `PickingTask` gains `block`/`resume` mutators and an `available(...)` factory |
| `inventory` | `StockJdbcRepository` gains `candidateBinsForArticle` (bins by ascending location code) and `unfinishedTaskReservation` (the reservation term, exposed standalone); new `CandidateBin` record; `Stock` gains a general `applyDelta` (with `decrease` now delegating to it) and an `initial(...)` factory; `StockMovement` gains an `adjustment(...)` factory |
| `orders` | `CustomerOrder.open(...)`, `OrderLine.create(...)` factories |
| `catalog` | `Article.create(...)`, `Location.create(...)` factories; `LocationRepository.findByPickSequence` |
| `api` | Nine new `ProblemCode`s: `ORDER_ALREADY_EXISTS`, `ORDER_NOT_FOUND`, `INSUFFICIENT_AVAILABLE_STOCK`, `ARTICLE_NOT_FOUND`, `ARTICLE_ALREADY_EXISTS`, `LOCATION_NOT_FOUND`, `LOCATION_ALREADY_EXISTS`, `PICK_SEQUENCE_ALREADY_EXISTS`, `NEGATIVE_RESULTING_STOCK` |
| tests | `OrderAllocationApiIT` (FT-02), `TaskRecoveryApiIT` (FT-09), `OrderLifecycleApiIT` (FT-10) — three separate classes, each with its own Testcontainers-backed database; `PersistenceLayerIT` gains the FT-13 injected-discrepancy test |

### Design decisions (recorded)

- **Block/resume live on `PickingService`, not a new admin service.** They
  are administrator-*triggered*, but they mutate the same picking-task state
  machine `PickingService` already owns (ADR 0004 treats block/resume as part
  of that one state machine, not a separate domain).
- **Order creation lives in a new `admin` package**, not `orders`, because it
  orchestrates `catalog` + `inventory` + `orders` + `picking` together — the
  same reasoning that put `auth` above `identity`/`picking` in Step 2.
- **`GET /admin/tasks` is a dedicated JDBC projection in `admin`**, not folded
  into `picking`'s single-purpose JDBC repository. It joins six tables purely
  for display and belongs with the other admin-only reads, keeping
  `PickingJdbcRepository` focused on the one FIFO-claim query it was built
  for.
- **Article/location creation pre-checks uniqueness with a `findBy...`
  lookup before insert**, giving a clean `409` rather than surfacing a raw
  constraint-violation exception — consistent with how `ORDER_ALREADY_EXISTS`
  and Step 2's `login` errors are already handled.
- **The three new admin test classes each get their own container**, per the
  policy established in Step 4's evidence. This step adds a concrete second
  reason beyond avoiding JUnit method-order fragility: a test's own
  order-creation call can leave *stray, never-completed `AVAILABLE` tasks*
  behind (e.g., `OrderAllocationApiIT`'s successful two-bin order is verified
  by reading it back, not by picking it), and those stray tasks would race
  FIFO order against a *different* test's own backdated order if they shared
  a database.
- **`created_at` cannot be backdated through the real API** (correctly — a
  real admin cannot forge history), so `TaskRecoveryApiIT` and
  `OrderLifecycleApiIT` create the order through the actual endpoint first,
  assert on its real response, and only *afterward* backdate it out-of-band
  via direct SQL so a picker's `GET /next` reaches it deterministically ahead
  of the shared seed fixture's own older tasks. `OrderAllocationApiIT` needs
  no backdating since it never involves a picker.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B clean verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13T23:29:55-06:00, total time 2:01 min |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `OrderAllocationApiIT` (FT-02) | Tests run 1, failures 0, errors 0 |
| `TaskRecoveryApiIT` (FT-09) | Tests run 1, failures 0, errors 0 |
| `OrderLifecycleApiIT` (FT-10) | Tests run 1, failures 0, errors 0 |
| `AuthApiIT` | Tests run 11, failures 0, errors 0 |
| `FlywayMigrationIT` | Tests run 5, failures 0, errors 0 |
| `PersistenceLayerIT` (incl. FT-13 discrepancy test) | Tests run 5, failures 0, errors 0 |
| `PickingApiIT` | Tests run 1, failures 0, errors 0 |
| `PickingNegativePathApiIT` | Tests run 2, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` (27 tests total) |

`OrderAllocationApiIT` proves, end to end: a 10-unit line against bins holding
6 and 10 units splits into two tasks (6 then 4) in ascending location-code
order, both `AVAILABLE`; a `201` response with the correct `Location` header,
`lineCount`, and `taskCount`; a subsequent shortage request (100 units against
6 remaining) returns `409 INSUFFICIENT_AVAILABLE_STOCK`, and the failed
order is entirely absent (`404` on read, zero stray `picking_task` rows).

`TaskRecoveryApiIT` proves: blocking an `ASSIGNED` task returns `BLOCKED` with
the given reason, releases `assigned_user_id`/`device_id` to `NULL` in the
database, audits the exact reason string in `task_transition`, and leaves
stock untouched; a picker attempting to block gets `403` (security-layer, not
even reaching the handler); resuming returns `AVAILABLE` with no stock
change; resuming an already-`AVAILABLE` task is `409 INVALID_TASK_STATE`.

`OrderLifecycleApiIT` proves: after completing line 1 of a two-line order, the
order is `IN_PROGRESS` and line 1 is `COMPLETED` while line 2 is still `OPEN`
— explicitly disproving premature order completion; after completing line 2,
both lines and the order read back as `COMPLETED`.

`PersistenceLayerIT.reconciliationDetectsAnInjectedDiscrepancy` corrupts one
stock row's quantity directly (bypassing the movement ledger) and confirms
`StockJdbcRepository.reconcileStockAgainstMovements()` returns exactly that
one bin with the correct stock/ledger values (`11` vs. `8`).

## Deviations and notes

- **A real allocation bug was found and fixed during this step, not a test
  artifact.** `OrderLifecycleApiIT`'s first run failed a *confirm* call with
  `409 INSUFFICIENT_STOCK` — not where the bug was, but where it surfaced.
  Root cause: the allocation walk computed each candidate bin's availability
  with a *fresh* `unfinishedTaskReservation` database query per line, but a
  planned task from an earlier line in the *same* order-creation request is
  never persisted until after every line's walk succeeds — so a second line
  referencing the same article could not see the first line's in-flight draw,
  and both lines could plan against the same bin's full stock simultaneously.
  With a two-line order each needing 5 units from a 5-unit bin, line 1 quietly
  "took" it in-memory while line 2's query still reported 5 available, so both
  planned to draw from the same bin, and only line 1's task actually had
  enough stock at confirm time. Fixed in `OrderAdminService.createOrder` by
  taking one reservation snapshot per locked bin before the walk starts, then
  tracking in-memory consumption across lines as the walk proceeds (see the
  code comment at the fix site). This is a correctness fix to the actual
  allocation algorithm, not a workaround in the test. Recorded per the
  failed-experiment rule.
- **SpotBugs `EI_EXPOSE_REP` (not the earlier `EI_EXPOSE_REP2`).** This is the
  first step to introduce `List`-typed record fields (`CreateOrderRequest`,
  `OrderDetailResponse`, `OrderDetailResponse.LineDetail`). SpotBugs correctly
  flagged that returning the field directly exposes the caller's mutable list.
  Fixed with compact constructors calling `List.copyOf(...)` (guarding for
  `null` on the request type, since Jackson can construct it with a missing
  field before Bean Validation's `@NotEmpty` runs) — not suppressed, since
  this is a legitimate finding distinct from the DI pattern the existing
  exclusion filter targets.
- No other deviations from the established patterns (Jackson 3, `RestTemplate`
  + JDK `HttpClient` factory, `saveAndFlush` before any trigger-validated
  insert).

## Residual risk

- The allocation fix is proved by `OrderLifecycleApiIT`'s two-line-same-article
  scenario; a *concurrent* two-admin-simultaneously-creating-orders-against-
  the-same-bin race is not separately tested here (the ascending lock order
  and `FOR UPDATE` already make it deadlock-safe and serializes the two
  transactions, consistent with the same guarantee Step 4 proved for picking
  claims, but no dedicated concurrency test exists for order creation).
- `GET /admin/tasks`'s filters (`state`, `orderNumber`, `assignedUsername`,
  `stuckOnly`) are implemented per API.md but not individually tested here;
  API.md documents no error cases for this endpoint, and Phase 8's dashboard
  work will exercise it from the consuming side.
