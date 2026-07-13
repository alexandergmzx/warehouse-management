# ADR 0003: Persistence, allocation, and lock model

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decisions: D-04, D-05, D-06

## Decision

Use Spring Data JPA for ordinary persistence and focused reads. Use named
`JdbcTemplate` operations or narrowly scoped native SQL for allocation, FIFO
claims, explicit row locking, reconciliation, and other PostgreSQL-specific
paths. Transaction boundaries belong in application services.

Define availability as:

```text
stock.quantity - sum(requested quantity of unfinished tasks)
```

Order allocation locks all candidate stock rows in ascending
`(article_id, location_id)` order, recalculates availability under lock, and
creates the complete order, lines, and task slices atomically or creates
nothing. Multi-bin slices follow ascending location code.

Use PostgreSQL `READ COMMITTED` with explicit locks. Claims use the complete FIFO
ordering and `FOR UPDATE OF task SKIP LOCKED`; FIFO means oldest currently
claimable work. Confirmation locks the task, validates ownership/state/idempotency,
then locks stock and updates task, line, order, stock, and movement atomically.

## Consequences

- Released tasks are durable reservations; no second mutable allocation balance
  is introduced initially.
- Lock order and indexes require concurrency and query-plan evidence.
- `SKIP LOCKED` can bypass a locked older task, so stuck-task age is observable.
- Broad automatic retries are prohibited; only bounded, known transient SQL
  retries may be considered after evidence.

