# HandheldPi loopback integration — first real client against the `/api/v1` HHT surface

**Build/configuration identifier:** warehouse-management git HEAD `494a760`
plus the `V1_2__seed_hht_demo_picker.sql` working tree; HandheldPi git HEAD
`512ef48` plus the Phase 3 v1-contract working tree (both uncommitted at
execution time — commit hashes to be recorded in the repos' histories at
commit). 2026-07-15.

## Scope

Stage 2 (loopback) of the HHT LAN integration plan (`handheld-plan.md`):
the real HandheldPi client stack — `HttpWmsClient`, sqlite `OfflineQueue`,
`PickingStateMachine` — driven end-to-end against a live WMS dev instance on
this machine. This is the first time a real external client (not the
integration-test suite) has exercised the `/api/v1` HHT contract.

WiFi loss was simulated with a kill-able local TCP proxy in front of port
8080, so timeouts/connection-refused take the real socket path; admin
operations (order creation, task block/resume) ran directly against the WMS
admin API. Driver: `e2e_loopback.py` (session scratchpad; deterministic,
44 explicit checks).

Stage 3 (physical GamePi20 over WiFi + runbook §3–4 firewall exercise) is
**not** covered here and remains open — it needs the physical device and the
owner at the LAN.

## Toolchain and runtime

| Item | Observed value |
| --- | --- |
| OS | Linux Mint 22.3, amd64 |
| JDK | OpenJDK 21.0.11 (`21.0.11+10-1-24.04.2-Ubuntu`, per ADR 0010) |
| Maven | 3.9.16 (wrapper, ADR 0009) |
| Docker / Compose | 29.6.1 / v5.3.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea…2193` (pinned digest, fresh volume) |
| Python (HHT client) | 3.12.3, HandheldPi deps only (`requests`, stdlib sqlite3) |
| WMS profile | `dev` (fixtures `V1_1` + `V1_2` applied by Flyway at boot) |
| Date | 2026-07-15 |
| Operator | Claude (Alexander Gomez's assistant), per the owner's implementation authorization |

## 1. WMS gate with the new dev fixture (`V1_2__seed_hht_demo_picker.sql`)

`env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify` — **BUILD SUCCESS**,
33/33 integration tests, 0 Checkstyle violations, 0 SpotBugs findings.
`FlywayMigrationIT` 5/5 with the updated additive assertions (3 successful
migrations under dev locations, 3 Argon2id users). The fixture adds
`picker02` (numeric password for the HHT PIN pad; hash generated with the
application `PasswordEncoder` via the evidenced Phase 7 Step 0 throwaway-test
pattern, generator deleted after capture) and loopback device `HHT-DEV-01`.

## 2. HandheldPi automated suite

`python -m pytest` in the HandheldPi repo — **100 passed**, including the new
HTTP-level suite (`tests/test_http_client.py` against the in-process
`tests/fake_wms.py`, real sockets) and the rewritten scripted functional
tests (`happy_path`, `wrong_scans`, `offline_pick`, `sync_failed`,
`discrepancy`, `token_expiry`).

## 3. Loopback end-to-end — 44/44 checks passed

Fresh database (compose volume recreated), badge `OP:picker02` + PIN 2468 on
device `HHT-DEV-01`.

| # | Scenario | Result |
| --- | --- | --- |
| 1 | Happy path: badge+PIN login → claim `DEMO-1001` task (A-01-01, ART-001, 20) → live scans → exact-quantity confirm → queue drained | PASS; server task `COMPLETED`, line pickedQuantity 20 |
| 2 | Server-side scan validation (raw client): wrong location, out-of-order article scan, wrong article | PASS — `409 WRONG_LOCATION`, `409 INVALID_TASK_STATE`, `409 WRONG_ARTICLE` |
| 3 | Quantity discipline: raw confirm qty 4 of 5 → `422 QUANTITY_MISMATCH`; device count-mismatch → `DISCREPANCY`, nothing sent; recount confirms | PASS |
| 4 | Idempotent confirm across an outage: confirm queued while "WiFi" down, delivered on reconnect; manual retry with same `confirmationId` returns identical outcome; different payload → `409 CONFIRMATION_ID_REUSED` | PASS |
| 5 | Work exhausted → `204` → NO_TASK screen | PASS |
| 6 | Level 2 offline chain (`E2E-1001`): claim online, complete pick offline (queue 3), claim-guard ("Sync pending — wait") and logout-guard verified, FIFO replay on reconnect → order `COMPLETED` | PASS |
| 7 | Replay rejection + recovery (`E2E-1002`): pick completed offline, admin **blocks** the task meanwhile, replay refused (`TASK_NOT_ASSIGNED_TO_USER` — block releases the assignment per ADR 0004), chain dead-lettered (3 rows), device `SYNC_FAILED` → acknowledged; admin **resume** → task claimed again and completed | PASS |
| 8 | `DEVICE_ASSIGNMENT_CONFLICT`: picker01 login on `HHT-DEV-01` while picker02 holds an active task → `409` | PASS |
| 9 | Token revoked mid-session: next live call → `401` → device drops to login keeping the queue; re-login; server returns the still-active task mid-state (resume) → completed | PASS |
| 10 | Correlation: device-generated `X-Correlation-Id` `668c69e8-15e5-413e-9b16-d6b1c19dc40c` (a deliberate `TASK_NOT_FOUND` probe) appears in both the device JSONL log and the WMS structured log (`GlobalExceptionHandler`, `problemCode: TASK_NOT_FOUND`) | PASS |

Note on scenario 7: the workflow-baseline expectation "blocked task → replay
rejected" is met; the specific problem code observed is
`TASK_NOT_ASSIGNED_TO_USER` because ADR 0004 blocking releases the user/device
assignment before state is evaluated. Both codes are handled identically by
the device (dead-letter + `SYNC_FAILED`).

## 4. SQL reconciliation after the run

Executed via `docker compose exec postgres psql -U wms -d wms`:

| Check | Result |
| --- | --- |
| `stock.quantity` vs `sum(stock_movement.quantity_delta)` per article/location | 0 mismatches |
| `COMPLETED` tasks with ≠ 1 `PICK` movement | 0 |
| Orders | `DEMO-1001`, `DEMO-1003`, `E2E-1001`, `E2E-1002`, `E2E-1003` = `COMPLETED`; `DEMO-1002` = `IN_PROGRESS` (seeded picker01 task, untouched — correct) |
| `PICK` movements | 7 (1 seeded + 6 from this run) |

## Deviations and residual risk

- The "token expiry" client path was exercised via **revocation** (logout of
  the picker's token by a parallel session) rather than waiting out
  `WMS_AUTH_TOKEN_TTL`; both return `401` with an auth `code` and take the
  identical client path (`WmsAuthError` → re-login → replay). `TOKEN_EXPIRED`
  specifically is covered by the HandheldPi HTTP suite against the fake
  server.
- WiFi loss was simulated at TCP level (proxy kill), not by radio power-down;
  Stage 3 on the physical device covers the real radio.
- Runbook §3–4 firewall steps remain **not exercised** — Stage 3 item.
