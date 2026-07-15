# Final acceptance sweep — SQL diagnostics, manual spot-check, preprod fail-fast, runbook rehearsal

**Build/configuration identifier:** git HEAD `f3cdee0` (Phases 8–10 commit,
current `main`), 2026-07-14.

## Scope

This sweep closes the two remaining open items recorded in `PLAN.md`'s
"Final acceptance" section and Phase 6 status line:

1. Execute the SQL diagnostic pack (`docs/sql-diagnostics.md`) against a
   running development database and record results (Phase 6 residual).
2. Perform a runbook rehearsal (`docs/runbook-windows.md`) and retain evidence
   (the sole unchecked Final-acceptance item).

It also re-confirms the full automated gate and exercises the manual/HTTP
surface (auth, picking, dashboard, labels, structured logs, preprod
fail-fast) end to end on a live instance, not just via integration tests.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| JDK | Temurin 21.0.11 (`21.0.11+10-LTS`) |
| Maven | 3.9.16 |
| Docker | Desktop, engine 29.6.1, Compose v5.2.0 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| OS | Windows 11 Pro, amd64 |
| Date | 2026-07-14 |
| Operator | Claude (Alexander Gomez's assistant), per the owner's implementation authorization |

**Note on host port:** the workstation's native PostgreSQL service already
holds port 5432 (pre-existing, unrelated to this project). The compose
container was started with `WMS_DB_PORT=5433` for this sweep; this is a
host-port binding only and does not affect the application, which reads
`WMS_DB_URL` from configuration.

## 1. Full automated gate (`mvn -B verify`)

| Check | Result |
|---|---|
| Integration tests (13 `*IT` classes) | 33 run, 0 failures, 0 errors, 0 skipped |
| Checkstyle | 0 violations |
| SpotBugs | 0 bugs |
| Overall | `BUILD SUCCESS` |

The one `ERROR`-level line in the run's structured log output
(`duplicate key value violates unique constraint "uq_picking_task_active_user"`)
is expected: `PickingNegativePathApiIT`'s concurrent-claim case deliberately
triggers this constraint and asserts exactly one claim succeeds (FT-04).

## 2. SQL diagnostic pack against a running dev database (closes Phase 6 residual)

Executed via `docker compose exec postgres psql -U wms -d wms` against the
`dev`-profile database (schema + demo seed data applied by Flyway on
application startup).

| Query | Expected (per `docs/sql-diagnostics.md`) | Observed |
|---|---|---|
| 1. Stuck picking tasks | `DEMO-1002-001-01`, `LOCATION_CONFIRMED`, past the 30-minute threshold | Exactly one row returned: `DEMO-1002-001-01`, `LOCATION_CONFIRMED`, `B-01-02`/`ART-004`, `picker01`/`HHT-PI-01`, `time_in_current_state` = `05:35:19.627372` — matches |
| 2. Stock vs. movement reconciliation | Zero rows (no discrepancy) | 0 rows — matches |
| 3. End-to-end trace, `DEMO-1003` | One completed order, one completed line, five `TRANSITION_*` events, one completed-task snapshot, one `PICK` movement of `-2` | 9 events returned in exactly this shape; final `MOVEMENT_PICK` row: `quantityDelta -2`, `resultingQuantity 13`, `correlationId 8207c66c-6ee3-47d2-8329-0e94ff654b2a` — matches |
| 4. Shift-handover integrity overview | — | `active_tasks=1`, `stuck_tasks=1`, `blocked_tasks=0`, `completed_orders=1`, `stock_discrepancies=0` |

All four diagnostics executed successfully and returned results consistent
with the seed data and every other retained evidence artifact. This
discharges the Phase 6 residual item ("execute the SQL diagnostic pack
against a running dev database and record results").

## 3. Manual HTTP/application spot-check

Run against the same live `dev`-profile instance (`java`/`mvn spring-boot:run`,
`SPRING_PROFILES_ACTIVE=dev`).

| Check | Result |
|---|---|
| `GET /actuator/health` | `200 {"status":"UP"}` |
| `POST /api/v1/auth/login` (picker01/HHT-PI-01) | `200`, bearer token issued |
| `GET /api/v1/hht/tasks/next` | `200`, returns picker01's existing active task (one-active-task-per-user invariant honored) |
| `POST /api/v1/auth/logout` | `204` |
| Dashboard form login (`/login`, CSRF token from the login page, admin/admin123) | `302` → `/dashboard` |
| `GET /dashboard` (authenticated) | `200`, HTML page renders |
| `GET /dashboard/api/tasks` (authenticated) | `200`, JSON task list; task 4 (`DEMO-1002-001-01`) correctly flagged `"stuck":true`, matching diagnostic query 1 |
| `GET /dashboard/api/tasks` (no session) | `302` → login (fails closed) |
| `GET /default-ui.css` (unauthenticated) | `200` — confirms the documented cross-chain fix (`DashboardSecurityConfiguration`) still holds |
| `GET /api/v1/admin/labels/locations/A-01-01/png` | `200 image/png`, bytes **identical (MD5 match)** to the retained sample `docs/evidence/2026-07-14-phase8-steps0-2/sample-label-location-A-01-01.png`, and identical on a repeat request (deterministic) |
| `GET /api/v1/admin/labels/articles/ART-001/pdf` | `200 application/pdf`, valid PDF signature (`%PDF-1.6`) |
| Structured log correlation | Completing task 4 (`scan-article` ART-004 → `confirm` qty 3) produced one `"pick confirmed"` log line with `correlationId 2c71a214-66b1-483d-b9d2-842f4691d5b7`; the resulting `stock_movement` row (id 7) carries the **same** `correlation_id`, and no password/token/secret appears anywhere in the captured log output |

Advancing task 4 to `COMPLETED` (to produce a genuine stock-changing
transaction for the log-correlation check) is a side effect of this sweep on
the throwaway compose database started for it; it does not touch any
persisted evidence or the committed migrations. A second device row
(`MANUAL-CHECK-DEV-01`) was registered for the admin bearer-API login, since
each login is device-scoped and the single seeded device was already held by
picker01's active task — the same pattern the integration tests use
(e.g. `OrderAllocationApiIT`).

## 4. Preprod fail-fast check (FT-15 parity, live)

| Scenario | Result |
|---|---|
| `SPRING_PROFILES_ACTIVE=preprod`, no `WMS_DB_*` set | `APPLICATION FAILED TO START`: *"Missing required preprod configuration: WMS_DB_URL, WMS_DB_USERNAME, WMS_DB_PASSWORD..."*, pointing to `docs/configuration-matrix.md`. No connection string or secret in the diagnostic. |
| `SPRING_PROFILES_ACTIVE=preprod`, `WMS_DB_PASSWORD` = committed dev default | `APPLICATION FAILED TO START`: *"WMS_DB_PASSWORD is set to the committed development default; preprod must use a distinct credential."* — an additional, previously undemonstrated live confirmation that the preprod validator rejects the known dev password specifically, not just a missing value. |

## 5. Best-effort clean-environment runbook rehearsal

**Deviation from a true fresh machine (recorded, not hidden):** this
workstation already has the pinned toolchain (JDK 21.0.11, Maven 3.9.16,
Docker) installed per ADR 0002; a genuinely blank machine was not available.
The rehearsal instead used a **fresh `git clone`** of the repository (new
working copy, no `target/`, no `.env`) to exercise the actual install/build/
run procedure rather than the developer's already-built tree.

| Runbook step (`docs/runbook-windows.md` §2, §5) | Result |
|---|---|
| 1. Clone repository to a clean directory | `git clone` into a fresh scratch directory (long-path support required on Windows: `git -c core.longpaths=true clone`) — confirmed no `target/`, no `.env` present after clone |
| 2. `mvn -B package -DskipTests` | `BUILD SUCCESS`, jar produced and repackaged by Spring Boot's plugin |
| 3. Set required preprod env vars | `WMS_DB_URL`/`WMS_DB_USERNAME`/`WMS_DB_PASSWORD` set to a **freshly created, empty** database/role (`wms_preprod_rehearsal`) in the same Postgres instance, with a password distinct from the committed dev default |
| 4. Start from the packaged jar (`java -jar target/warehouse-management-0.1.0-SNAPSHOT.jar`) | Started successfully |
| 5. `GET /actuator/health` (loopback) | `200 {"status":"UP"}` |
| Post-check: only `db/migration` applied, no dev fixtures | `flyway_schema_history` shows only version `1` ("create schema"); `app_user` table has 0 rows — confirms preprod correctly excludes `db/devdata` |
| Post-check: no secret leakage | Rehearsal password does not appear anywhere in captured startup log output |
| Clean shutdown | JVM process stopped; health check subsequently refused connection as expected |

Section 3 (scoped LAN firewall rule) and Section 4 (reachability from a
second LAN machine) were **not** performed — both require administrative
network changes and/or a second physical machine, out of scope for a
software-only rehearsal; this is a recorded exclusion, not a silent gap.

The rehearsal-only role and database (`wms_preprod_rehearsal`) and the
scratch clone directory were removed after the rehearsal; they were never
part of the committed repository or the persistent dev database.

## 6. Visual evidence (screenshots)

Captured in a follow-up step on the same date, against a freshly started
(and afterward torn-down) instance of the same dev-profile stack described
above — a new compose Postgres container and application process, not the
one used for sections 1–5. All six images are genuine captures of live
application output; none are mockups. Files: `docs/evidence/2026-07-14-final-acceptance-sweep/`.

| Image | What it shows | Capture method |
|---|---|---|
| `login.png` | The dashboard's Spring Security sign-in form (`/login`) | Single-shot headless Chrome: `chrome --headless=new --screenshot --window-size=1280,900 http://localhost:8080/login` — no authentication needed, the page is `permitAll()` |
| `dashboard.png` | The authenticated admin task board (`/dashboard`) — all five seeded demo tasks, with `DEMO-1002-001-01` highlighted and flagged `STUCK`, and a "Last refreshed" timestamp confirming the client-side poll ran | Real form login as `admin`/`admin123` via `curl` (CSRF token read from the login page, session cookie captured), then the `JSESSIONID` injected into headless Chrome over the Chrome DevTools Protocol (`Network.setCookie`) using a small Node script (Node's built-in `WebSocket`/`fetch`, no npm install); navigated to `/dashboard` and waited one `WMS_DASHBOARD_POLL_INTERVAL` cycle before capturing |
| `label-location-qr.png` | The raw QR PNG label for location `A-01-01` | Direct fetch of `GET /api/v1/admin/labels/locations/A-01-01/png` with an admin bearer token — not a browser screenshot, the API's actual response bytes |
| `label-article.png` | The full A4 PDF label for article `ART-001` (QR + human-readable text), cropped to the content area | Fetched `GET /api/v1/admin/labels/articles/ART-001/pdf` with an admin bearer token, then rasterized with PDFBox's `PDFRenderer` (already a project Maven dependency — a small standalone helper class compiled against `mvn dependency:build-classpath`, not added to `src/`) |
| `preprod-failfast.png` | The real preprod startup failure console output (missing `WMS_DB_*`) | Re-ran `SPRING_PROFILES_ACTIVE=preprod mvn spring-boot:run` with no `WMS_DB_*` set, captured the actual `APPLICATION FAILED TO START` text, then rendered it verbatim into a minimal terminal-styled HTML page and screenshotted that page with headless Chrome (for a readable fixed-width panel) — the on-screen text is the real captured output, not fabricated |
| `sql-diagnostics.png` | Real output of SQL diagnostic queries 1 (stuck tasks) and 4 (integrity overview) against the live dev database | Ran both queries via `docker compose exec postgres psql`, captured the actual result text, rendered verbatim into the same terminal-styled HTML template, screenshotted |

Raw text backing the two terminal panels (for independent verification):

```
$ psql -U wms -d wms -c "-- query 1: stuck picking tasks"
   task_number    | order_number |    task_status     | assigned_user | location_code |   sku   | requested_quantity | time_in_current_state
------------------+--------------+---------------------+---------------+---------------+---------+---------------------+-----------------------
 DEMO-1002-001-01 | DEMO-1002    | LOCATION_CONFIRMED | picker01      | B-01-02       | ART-004 |                   3 | 02:07:48.672999
(1 row)

$ psql -U wms -d wms -c "-- query 4: integrity overview"
     check_name      | value
---------------------+-------
 active_tasks        |     1
 stuck_tasks         |     1
 blocked_tasks       |     0
 completed_orders    |     1
 stock_discrepancies |     0
(5 rows)
```

```
$ SPRING_PROFILES_ACTIVE=preprod mvn spring-boot:run
[INFO] --- spring-boot:4.0.7:run (default-cli) @ warehouse-management ---
14:47:32.373 [main] ERROR org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter --

***************************
APPLICATION FAILED TO START
***************************

Description:

Missing required preprod configuration: WMS_DB_URL, WMS_DB_USERNAME, WMS_DB_PASSWORD. Set these environment variables before starting the preprod profile.

Action:

Review docs/configuration-matrix.md for the required WMS_DB_* variables and their sensitivity, then set them before starting the preprod profile again.

[INFO] BUILD FAILURE
[INFO] Total time:  9.229 s
```

No new tools were downloaded to produce these images: Chrome and Node were
already installed on this workstation (owner-managed, per CLAUDE.md); the
PDFBox rasterizer reused an existing project dependency; the CDP script used
only Node's built-in `WebSocket`/`fetch`.

## Deviations and notes summary

- Compose Postgres bound to host port 5433 instead of the default 5432
  (pre-existing native PostgreSQL service already held 5432 on this
  workstation) — application-level configuration only, not a code or
  schema change.
- The runbook rehearsal used a fresh clone rather than a fresh machine
  (toolchain pre-installed); Sections 3–4 (firewall, cross-machine LAN
  check) were excluded as out of scope for this environment.
- No other deviations from documented procedures.

## Final acceptance status

With this sweep, both previously open items are closed:

- The Phase 6 SQL diagnostic pack has been executed against a running
  development database, with results recorded above matching every
  documented expectation.
- A runbook rehearsal has been performed and evidenced, with the fresh-
  machine caveat above recorded rather than hidden.

Combined with the existing FT-01–FT-19 = 19 Passed record
(`docs/executed-test-report.md`) and this sweep's full `mvn verify` re-run
(33/33, 0 static-analysis violations), all `PLAN.md` "Final acceptance"
checklist items are now discharged.
