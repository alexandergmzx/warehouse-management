# ADR 0004: Domain states, recovery, and audit model

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decisions: D-07, D-10

## Decision

Use task states `AVAILABLE`, `ASSIGNED`, `LOCATION_CONFIRMED`,
`ARTICLE_CONFIRMED`, `BLOCKED`, and `COMPLETED`. The normal path is
`AVAILABLE -> ASSIGNED -> LOCATION_CONFIRMED -> ARTICLE_CONFIRMED -> COMPLETED`.
An administrator may move active work to `BLOCKED` with a required reason and
may resume it to `AVAILABLE`; the HHT has no skip, block, or resume operation.
Resume clears scan confirmations, preserves FIFO/allocation fields, and releases
assignment without changing stock.

Use order states `OPEN`, `IN_PROGRESS`, and `COMPLETED`, and line states
`OPEN`, `IN_PROGRESS`, and `COMPLETED`. A line completes only when all its tasks
complete; an order completes only when all lines complete.

Create append-only `task_transition` records for claim, scans, block, resume,
and completion. Each record contains its ID, task, prior/new state, timestamp,
actor, device where applicable, reason where required, correlation ID, and
confirmation ID where applicable. Stock movements and transitions cannot be
updated or deleted.

## Consequences

- Direct SQL state repair, fabricated confirmations, and deletion of history are
  prohibited.
- Cancellation and reassignment are outside the MVP.
- Exact timestamps and constraints must be included in the approved schema
  specification before migration work begins.

