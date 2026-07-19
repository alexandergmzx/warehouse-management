# Architecture and package layout

**Status: accepted design; the database foundation is implemented, and the
application modules are delivered phase by phase.**

The design decisions are consolidated in `docs/decisions/0002` through
`docs/decisions/0008`. Artifacts belonging to phases that have not been
implemented and evidenced remain provisional.

The proposed application is a modular monolith. A single deployment would keep the PoC operable while module boundaries demonstrate where responsibilities and future integrations belong.

| Module/package | Responsibility | May depend on |
|---|---|---|
| `catalog` | Articles, locations, QR identity | shared primitives only |
| `inventory` | Stock balances and append-only movements | catalog, identity identifiers |
| `orders` | Orders, lines, release/completion rules | catalog, inventory application ports |
| `picking` | Allocation, atomic task claim, scan state machine, confirmation | orders, inventory, identity |
| `identity` | Users, devices, opaque tokens | shared primitives only |
| `operations` | Dashboard reads, health, diagnostics-facing projections | read-only ports from all modules |
| `mfc` | Outbound completion adapters, mission outbox/dispatch, WCS confirmation endpoints | orders completion port, catalog (location lookup), identity (WCS auth) |

If approved, REST controllers will call application services and never repositories directly. Transaction boundaries will belong to application services. Domain code will not depend on HTTP DTOs, JPA repositories, PostgreSQL classes, or the future TCP adapter.

## Database transaction boundaries

- Order creation locks candidate stock, checks total unallocated availability, creates all lines/tasks, and releases the order atomically.
- Next-task claim uses a transaction and `FOR UPDATE SKIP LOCKED`, ordered by order creation time, order ID, line number, task sequence, and task ID.
- Confirmation locks the task and stock row, verifies ownership/state/idempotency, updates stock/task/line/order, and inserts the movement before commit.
- Every stock-changing transaction updates `stock` before inserting the movement. The database validates that movement `resulting_quantity` equals the current locked stock row.

## Configuration boundaries

The schema migration location is common to all profiles. The development profile adds a second Flyway location containing demo credentials and fixtures. Preproduction never scans that location and requires externally supplied database credentials.

## MFC extension seam

**Status: implemented and evidenced (Phase 10 seam; MFC work package real
adapter, ADR 0011).** `orders.OrderCompletionPublisher` is the application
port; `orders.OrderCompletionEvent` (`eventId`, `orderId`, `orderNumber`,
`completedAt`) is the immutable message (ADR 0007). Order-domain code
(`picking.PickingService`, at the point `CustomerOrder` transitions to
`COMPLETED`) depends on only these two types — neither imports anything from
`mfc`, a socket, or a telegram class, and none of those classes exist
anywhere in this codebase to leak in. Two adapters exist, selected by
`wms.mfc.adapter` (`docs/configuration-matrix.md`): `mfc.NoopOrderCompletionPublisher`
(default, `noop`) logs one structured line and does nothing else;
`mfc.TelegramOrderCompletionPublisher` (`telegram`) queues a real MFC
mission — see below. `orders.FakeOrderCompletionPublisher` (test-only)
proves the port fires exactly once per completed order
(`OrderCompletionSeamApiIT`).

A real `mfc.TelegramOrderCompletionPublisher` adapter is implemented (ADR
0011, MFC work package, `PLAN.md`), selected by `wms.mfc.adapter=telegram`
(the `noop` adapter above remains the default). Order-domain code is
unchanged by its existence — it still depends on nothing beyond the port and
`OrderCompletionEvent`. The telegram contract itself (`TELEGRAMS.md`) is
authored and owned in this repository per `../ECOSYSTEM.md`'s contract
rules; `agv-fleet-controller` pins a version. How the real adapter resolves
each boundary the no-op seam left open:

- **Serialization.** Order-domain code still emits the plain
  `OrderCompletionEvent` record; `mfc.TelegramPayload` (the wire shape,
  `TELEGRAMS.md`) is built entirely inside `mfc`, from `MfcMission` plus a
  location-code lookup, at dispatch time — not from the event directly.
- **Timeout/transport.** Owned by `mfc.MissionDispatcher`, via Spring's
  built-in `RestClient` — no new dependency, per ADR 0011.
- **Result/delivery outcome.** Still not observable through the port
  (`void publish(...)` is unchanged); delivery outcome is tracked instead on
  the `mfc_mission` row's `state` (`PENDING → DISPATCHED → ACCEPTED →
  COMPLETED | FAILED`, `TELEGRAMS.md`) and its append-only
  `mfc_mission_transition` ledger — a separate, queryable record rather than
  a port-contract change.
- **Retry ownership.** `MissionDispatcher` owns a fixed-interval retry with a
  capped attempt count (`wms.mfc.telegram.max-attempts`), using `eventId` as
  the telegram's idempotency field; it never blocks the transaction that
  calls `publish()`, since that transaction only inserts the outbox row.
- **Transaction boundary.** Resolved in favor of the transactional-outbox
  option ADR 0007 raised: `publish()` still runs synchronously pre-commit,
  but it now only inserts a `PENDING` `mfc_mission` row (fast, no network
  I/O) — the actual delivery happens later, out of that transaction, via
  `MissionDispatcher`'s own poll-and-dispatch transaction per mission. See
  ADR 0011 for the comparison against post-commit dispatch.
- **Observability.** `TelegramOrderCompletionPublisher` and
  `MissionDispatcher` both log structured lines (`docs/log-analysis-guide.md`)
  correlated by `missionId`/`eventId`; the no-op adapter's single log line is
  unchanged when it remains selected.
