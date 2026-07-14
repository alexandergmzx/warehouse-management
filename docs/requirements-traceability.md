# MVP requirements-to-test traceability

**Status:** Accepted design specification. Execution evidence for FT-01–FT-18
is retained in `docs/evidence/`; see `docs/executed-test-report.md` for the
aggregated status.  
**Date:** 2026-07-13  
**Authority:** `README.md`, `docs/research/phase-3-decision-packet.md`, and accepted ADRs

| ID | Requirement/invariant | Primary evidence | Planned test IDs |
|---|---|---|---|
| R-01 | Exact task quantity is required; partial picks are rejected | API response, task/stock state | FT-07, IT-08 |
| R-02 | HHT has no skip; blocked work uses admin recovery | API surface, audit transition | FT-09, IT-12 |
| R-03 | Claim order is global FIFO with deterministic tie-breakers | Claim query and concurrent evidence | FT-04, IT-05 |
| R-04 | User/device has at most one active task | Unique constraint and conflict response | FT-04, IT-06 |
| R-05 | Multi-bin allocation follows ascending location code | Task rows and allocation query | FT-02, IT-03 |
| R-06 | Stock changes only on successful confirmation | Before/after stock and movement report | FT-06–FT-08, IT-08 |
| R-07 | Stock, task, line, order, and movement commit atomically | Rollback evidence | FT-08, IT-09 |
| R-08 | Every stock change appends exactly one movement | Reconciliation SQL and movement count | FT-08, IT-10 |
| R-09 | Movement ledger is append-only | Mutation rejection evidence | FT-13, IT-11 |
| R-10 | Confirmation retry cannot decrement twice | Same-ID response and movement count | FT-08, IT-08 |
| R-11 | Wrong scans and invalid quantities make no stock change | Problem response and SQL check | FT-05, FT-07, IT-08 |
| R-12 | Admin block/resume is audited and preserves reservation | Transition history | FT-09, IT-12 |
| R-13 | Authentication tokens are opaque, hashed, expiring, and revocable | Security test and database inspection | FT-01, IT-13 |
| R-14 | API uses versioned REST, RFC 9457 errors, and correlation IDs | Contract test and log record | FT-03, IT-14 |
| R-15 | Preproduction rejects missing/unsafe required configuration | Startup output and profile test | FT-15, IT-15 |
| R-16 | Logs support order/task/user/device/article/location/movement diagnosis | Redaction and structured-log evidence | FT-16, IT-16 |
| R-17 | QR payloads are exact and generated labels deterministic | Hashes and scan results | FT-17, IT-17 |
| R-18 | Dashboard is authenticated and refreshes by polling | Browser evidence | FT-18 |
| R-19 | Completion publisher is a no-op seam with one fake observation | Fake adapter test | IT-18 |
| R-20 | Explicit exclusions do not leak into the MVP | API/package/scope review | FT-19 |

## Evidence rules

Each executed test records build ID, configuration ID, date/time, operator,
environment, result, evidence paths, and defect reference where applicable.
A failed test remains in the report with its diagnosis and disposition.
Compilation alone cannot satisfy a traceability row.

