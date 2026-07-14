# MVP functional test specification

**Status:** Approved design specification. FT-01–FT-18 executed and passed,
FT-19 intentionally blocked pending Phase 10 — see
`docs/executed-test-report.md` for the aggregated, evidence-cited results.  
**Date:** 2026-07-13  
**Related:** `docs/requirements-traceability.md`, `docs/executed-test-report.md`

## Test data and evidence

Use deterministic development fixtures only. Record build/configuration ID,
profile, database migration version, test date, operator, request correlation
IDs, and evidence paths. Do not record passwords, bearer tokens, or password
hashes.

## Numbered cases

| ID | Case | Expected result |
|---|---|---|
| FT-01 | Login with valid picker/device and logout | Opaque token issued; token is not logged; logout and repeated logout succeed. |
| FT-02 | Create an order requiring two stock bins | Complete allocation succeeds in ascending location order; shortage rolls back all rows. |
| FT-03 | Send malformed request, missing token, and insufficient role | RFC 9457 responses use stable codes and correlation IDs; no state changes. |
| FT-04 | Two users claim concurrently and one user/device requests another task | Each task is claimed once; one user/device has at most one active task. |
| FT-05 | Scan wrong location and wrong article | Correct error returned; task, stock, movement, and transition state do not advance. |
| FT-06 | Scan correct location and article in order | Task reaches the confirmation-ready state; repeated correct scans are replay-safe. |
| FT-07 | Confirm zero, over-quantity, and partial quantity | Exact-quantity rule rejects all mismatches with no stock or movement change. |
| FT-08 | Confirm exact quantity and repeat the same UUID | One stock decrement and one PICK movement; repeat returns the original result. |
| FT-09 | Block an assigned task and resume it as administrator | Required reason is audited; assignment is released; resume returns AVAILABLE; no stock change. |
| FT-10 | Complete all tasks in a line and then all lines in an order | Line and order states progress only when their approved work is complete. |
| FT-11 | Attempt to update/delete a stock movement | Database/application rejects mutation; existing ledger row is unchanged. |
| FT-12 | Reuse a confirmation UUID with different canonical payload | `409 CONFIRMATION_ID_REUSED`; no second stock change. |
| FT-13 | Reconcile stock against movement deltas | Diagnostic query returns equality for clean fixtures and identifies an injected discrepancy. |
| FT-14 | Expired, revoked, inactive-user, and inactive-device access | Requests fail closed with stable authentication/authorization problems. |
| FT-15 | Start preproduction with missing or unsafe required configuration | Startup fails with safe diagnostic; no secret or connection detail is exposed. |
| FT-16 | Execute a stock-changing request and inspect logs | JSON includes correlation/order/task/user/device/article/location/movement fields and excludes secrets. |
| FT-17 | Generate the same location/article labels twice | Payloads are exact; PNG/PDF outputs are deterministic and scan correctly. |
| FT-18 | Open the admin dashboard and wait for a polling interval | Admin-only view refreshes without full-page reload; unauthorized access fails. |
| FT-19 | Inspect the MVP surface for excluded features | No HHT skip, partial pick, direct HHT DB access, TCP, scheduler, or transport retry exists. |

## Execution classification

- `FT-01` through `FT-14`: API/database functional and integration evidence.
- `FT-15` and `FT-16`: configuration and observability evidence.
- `FT-17` and `FT-18`: presentation and operational evidence.
- `FT-19`: scope and architecture review.

The final report must include passed, failed, blocked, and not-applicable cases.
A case marked blocked by an unavailable runtime is not a pass.

