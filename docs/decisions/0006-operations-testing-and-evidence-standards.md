# ADR 0006: Operations, observability, testing, and evidence

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decisions: D-11, D-12, D-13

## Decision

Emit one-line structured JSON logs to standard output. Include UTC timestamp,
event, level, build/configuration ID, correlation ID, and applicable order, task,
user/device, article, location, movement, outcome, error, and duration fields.
Use allow-listed fields and never emit authorization headers, tokens, passwords,
hashes, database secrets, arbitrary bodies, or unsafe exception details.

Bind `wms.*` configuration to typed validated properties. Use separate `dev` and
`preprod` profiles; development fixtures and credentials are never loaded by
preproduction. Validate ports, durations, credentials, and required security
values at startup. Keep PostgreSQL loopback-only and document any scoped API
firewall exposure in the runbook.

Use Surefire for unit tests and Failsafe with pinned PostgreSQL Testcontainers
for migration, repository, transaction, API, concurrency, idempotency, and
recovery tests. Maintain numbered functional cases, read-only diagnostics,
traceability, and retained machine-readable/human reports under an immutable
build/configuration identifier. Compilation alone is not acceptance.

## Consequences

- Log retention is an operational responsibility outside the application.
- CI uses least privilege, immutable action references, dependency caching, and
  approved report retention.
- Configuration and test evidence are first-class deliverables.

