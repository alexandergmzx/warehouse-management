# Phase 7 Step 4 evidence — picking negative path, concurrency, and idempotency

**Build/configuration identifier:** `6f4da75+phase7-step4-negative / 2026-07-14T04:58:54Z`
(git HEAD `6f4da75` — the committed Step 3 baseline — plus the Step 4 working
tree; the identifier becomes immutable when Step 4 is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 4 (`PLAN.md`): the
dedicated negative-path, concurrency, and idempotency-reuse test suite —
"the core testing-competence evidence" for this portfolio.

Functional cases covered: **FT-04** (concurrent claims never duplicate; one
user/device holds at most one active task even under a claim race), **FT-05**
(wrong location/article leave task, stock, and movement state unchanged),
**FT-07** (zero, over-quantity, and partial quantity are all rejected by the
exact-quantity rule with no stock or movement change), **FT-12** (reusing a
confirmation UUID with a different quantity is `409 CONFIRMATION_ID_REUSED`
with no second stock change).

No production code changed in this step. Every check this suite exercises
(wrong-scan rejection, exact-quantity enforcement, confirmation-ID replay
detection, the claim-assignment unique-index guard) was already implemented in
Step 3, because a correct confirm/claim transaction cannot be written without
them — Step 4 is the dedicated proof that those paths behave as specified,
including under genuine concurrent load.

## What was added

| Area | Artifacts |
|---|---|
| `picking` (test) | `PickingNegativePathApiIT` — a new integration-test class with its own Testcontainers-backed database, independent of `PickingApiIT` |

### Design decisions (recorded)

- **A separate test class with its own container, not more methods added to
  `PickingApiIT`.** JUnit does not guarantee method execution order within a
  class, and both classes' tests mutate shared, scarce seed fixture rows
  (three seeded `AVAILABLE` tasks). Splitting by concern (happy path vs.
  negative/concurrency) avoids ordering fragility entirely, at the cost of a
  second container start.
- **FT-05, FT-07, and FT-12 are proved as one continuous operator journey on a
  single claimed task**, rather than three independent tests. This mirrors a
  realistic HHT session (wrong scan → correct scan → wrong scan → correct scan
  → rejected confirms → successful confirm → confirmation replay attempt) and
  avoids needing three separate task claims from an already-scarce fixture.
  Each step's negative assertion additionally re-reads the task's live state
  through `GET /next` (the API itself, not raw SQL) to prove no regression,
  and independently verifies via direct SQL that stock and
  `stock_movement` are untouched — the state assertion and the ledger
  assertion are deliberately checked through two different paths.
- **FT-04 uses its own synthetic, backdated order/line/task pool inserted via
  JDBC**, rather than the scarce seed fixture. A first attempt reused the
  fixture's remaining `AVAILABLE` tasks and failed non-deterministically: two
  genuine seed tasks (from the `DEMO-1001` order, created ~30 minutes before
  test run time) are *older* than a freshly inserted synthetic order, so
  global FIFO claimed the leftover seed tasks first, not the synthetic pool
  the assertions expected. Fixed by explicitly backdating the synthetic
  order's `created_at`/`released_at` to 100 days before any seed data,
  guaranteeing it is claimed first regardless of what the sibling test method
  consumed or method execution order. Recorded per the failed-experiment rule.
- **FT-04 Part B's expected outcome is a specific, non-vague pair of HTTP
  statuses** (`{200, 409}`), not just "the database ends up consistent."
  Reasoning back from PostgreSQL's unique-index behavior under concurrent
  transactions: `SKIP LOCKED` means the two racing claims lock *different*
  task rows (no blocking there), so the actual serialization point is the
  partial unique index on `assigned_user_id` — the second transaction's
  `UPDATE` blocks until the first commits, then fails with `23505` once the
  conflict becomes visible. Because that failure happens inside the request
  transaction (not after a partial commit), the losing transaction rolls back
  entirely, so its claimed task also reverts to `AVAILABLE` — a claim race
  never strands a task in limbo.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B clean verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13T22:58:46-06:00, total time 1:20 min |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `PickingNegativePathApiIT` (FT-04, FT-05, FT-07, FT-12) | Tests run 2, failures 0, errors 0 |
| `PickingApiIT` (FT-06, FT-08) | Tests run 1, failures 0, errors 0 |
| `AuthApiIT` | Tests run 11, failures 0, errors 0 |
| `PersistenceLayerIT` | Tests run 4, failures 0, errors 0 |
| `FlywayMigrationIT` | Tests run 5, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` (23 tests total) |

`ft05Ft07Ft12_wrongScansQuantityMismatchAndConfirmationReuseLeaveNoTrace`
asserts, end to end, for a single claimed task:

- wrong location while `ASSIGNED` → `409 WRONG_LOCATION`; task stays
  `ASSIGNED` (verified via `GET /next`); stock and movement count (`0`)
  unchanged;
- correct location scan → `LOCATION_CONFIRMED`;
- wrong article while `LOCATION_CONFIRMED` → `409 WRONG_ARTICLE`; task stays
  `LOCATION_CONFIRMED`; stock and movement count unchanged;
- correct article scan → `ARTICLE_CONFIRMED`;
- confirm with `0`, `requested+5`, and `requested-1` → all three
  `422 QUANTITY_MISMATCH`; task stays `ARTICLE_CONFIRMED`; stock and movement
  count (`0`) unchanged after all three attempts;
- confirm with the exact requested quantity → `200`, one movement, stock
  decremented by exactly that quantity;
- reusing the same confirmation UUID with `quantity - 1` →
  `409 CONFIRMATION_ID_REUSED`; stock and movement count (`1`) unchanged.

`ft04_concurrentClaimsNeverDuplicateAndOneUserGetsAtMostOneActiveTask` asserts,
against a dedicated, backdated pool of four synthetic `AVAILABLE` tasks:

- **Part A** — two different operators fire `GET /next` concurrently (aligned
  with a `CyclicBarrier` to maximize contention); both receive `200` with
  **different** task IDs, both drawn from the synthetic pool;
- **Part B** — the *same* operator fires two concurrent `GET /next` calls; the
  observed status pair is exactly `{200, 409}` (`TASK_ASSIGNMENT_CONFLICT` for
  the loser); after the race resolves, exactly **one** row in `picking_task`
  has this operator as `assigned_user_id` in an active state, verified by
  direct SQL against the `uq_picking_task_active_user` invariant.

The server log for this run shows the expected `23505` unique-constraint
warning from PostgreSQL during Part B, confirming the race was genuinely
exercised (not a false pass from insufficient contention).

## Residual risk

- Concurrency tests inherently depend on the scheduler achieving enough
  contention for the interesting code path to execute; the `CyclicBarrier`
  makes this highly likely but not physically guaranteed on every possible
  environment. The assertions (`{200, 409}`, exactly one active task) hold
  regardless of which thread "wins," so a pass is meaningful either way — if
  contention were completely absent, both would trivially succeed and the
  second-call check would fail differently (still a valid signal).
- FT-09 (block/resume recovery) and FT-13 (reconciliation diagnostics) remain
  Step 5's scope, along with the admin endpoints that create orders through
  the normal allocation path rather than direct JDBC test fixtures.
