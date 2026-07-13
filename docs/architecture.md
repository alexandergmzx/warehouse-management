# Architecture and package layout

The application is a modular monolith. A single deployment keeps the PoC operable while module boundaries demonstrate where responsibilities and future integrations belong.

| Module/package | Responsibility | May depend on |
|---|---|---|
| `catalog` | Articles, locations, QR identity | shared primitives only |
| `inventory` | Stock balances and append-only movements | catalog, identity identifiers |
| `orders` | Orders, lines, release/completion rules | catalog, inventory application ports |
| `picking` | Allocation, atomic task claim, scan state machine, confirmation | orders, inventory, identity |
| `identity` | Users, devices, opaque tokens | shared primitives only |
| `operations` | Dashboard reads, health, diagnostics-facing projections | read-only ports from all modules |
| `mfc` | Future outbound completion adapter | orders completion port only |

REST controllers will call application services and never repositories directly. Transaction boundaries belong to application services. Domain code does not depend on HTTP DTOs, JPA repositories, PostgreSQL classes, or the future TCP adapter.

## Database transaction boundaries

- Order creation locks candidate stock, checks total unallocated availability, creates all lines/tasks, and releases the order atomically.
- Next-task claim uses a transaction and `FOR UPDATE SKIP LOCKED`, ordered by order creation time, order ID, line number, task sequence, and task ID.
- Confirmation locks the task and stock row, verifies ownership/state/idempotency, updates stock/task/line/order, and inserts the movement before commit.
- Every stock-changing transaction updates `stock` before inserting the movement. The database validates that movement `resulting_quantity` equals the current locked stock row.

## Configuration boundaries

The schema migration location is common to all profiles. The development profile adds a second Flyway location containing demo credentials and fixtures. Preproduction never scans that location and requires externally supplied database credentials.

## MFC extension seam

Phase 5 will define an `OrderCompletionPublisher` application port in the order module and a disabled no-operation adapter. A future `mfc` TCP adapter will implement that port and own telegram serialization, connection timeouts, delivery outcomes, and transport metrics. Order-domain code will know only the immutable completion message and will not import socket or telegram classes. No TCP implementation is part of the current scaffold.
