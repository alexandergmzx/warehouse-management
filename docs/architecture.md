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
| `mfc` | Future outbound completion adapter | orders completion port only |

If approved, REST controllers will call application services and never repositories directly. Transaction boundaries will belong to application services. Domain code will not depend on HTTP DTOs, JPA repositories, PostgreSQL classes, or the future TCP adapter.

## Database transaction boundaries

- Order creation locks candidate stock, checks total unallocated availability, creates all lines/tasks, and releases the order atomically.
- Next-task claim uses a transaction and `FOR UPDATE SKIP LOCKED`, ordered by order creation time, order ID, line number, task sequence, and task ID.
- Confirmation locks the task and stock row, verifies ownership/state/idempotency, updates stock/task/line/order, and inserts the movement before commit.
- Every stock-changing transaction updates `stock` before inserting the movement. The database validates that movement `resulting_quantity` equals the current locked stock row.

## Configuration boundaries

The schema migration location is common to all profiles. The development profile adds a second Flyway location containing demo credentials and fixtures. Preproduction never scans that location and requires externally supplied database credentials.

## MFC extension seam

**Status: implemented and evidenced (Phase 10).** `orders.OrderCompletionPublisher`
is the application port; `orders.OrderCompletionEvent` (`eventId`, `orderId`,
`orderNumber`, `completedAt`) is the immutable message (ADR 0007). Order-domain
code (`picking.PickingService`, at the point `CustomerOrder` transitions to
`COMPLETED`) depends on only these two types — neither imports anything from
`mfc`, a socket, or a telegram class, and none of those classes exist anywhere
in this codebase to leak in. `mfc.NoopOrderCompletionPublisher` is the only
adapter, selected by `wms.mfc.adapter=noop` (the only supported value; see
`docs/configuration-matrix.md`) — it logs one structured line and does
nothing else. `orders.FakeOrderCompletionPublisher` (test-only) proves the
port fires exactly once per completed order
(`OrderCompletionSeamApiIT`).

A future `mfc` TCP adapter implementing the same port would own everything
below; none of it is implemented, and none of it should leak into
order-domain code when it is:

- **Serialization.** Order-domain code emits the plain `OrderCompletionEvent`
  record; telegram framing/encoding is entirely the adapter's concern.
- **Timeout.** Connection/write/response timeouts against the MFC endpoint
  are transport concerns the adapter owns; `publish()`'s contract makes no
  timing guarantee today (the no-op returns immediately).
- **Result/delivery outcome.** Whether a real telegram was accepted,
  rejected, or timed out is not observable through this port as currently
  defined (`void publish(...)`); a real adapter needing to report delivery
  outcome back to the order domain would require a considered, separate
  extension to the port's contract — not a silent behavior change.
- **Retry ownership.** No retry loop, queue, or scheduler exists or is
  planned as part of this port; a real adapter would own retries (using
  `eventId` as the idempotency key an at-least-once retry would replay) and
  must not block the transaction that calls `publish()`.
- **Transaction boundary.** `publish()` is currently called synchronously
  inside the same `@Transactional` method that completes the order (before
  commit). A real adapter performing real network I/O should reconsider
  this — either move the call to post-commit (accepting the small window
  where a commit succeeds but publishing is never attempted) or adopt a
  transactional-outbox pattern (accepting more implementation complexity to
  close that window). Neither is implemented; this is a documented decision
  point for that future work, not a defect in the no-op seam.
- **Observability.** The no-op adapter logs one structured line per
  publication (`docs/log-analysis-guide.md`); a real adapter would add its
  own transport-level correlation (e.g. propagating the request's
  `correlationId` into the telegram) and metrics (latency, delivery
  success/failure counts) — none of which exist today.
