# Miniature Warehouse Management System

A Java 21 / Spring Boot proof of concept focused on warehouse picking, SQL diagnostics, configuration discipline, test evidence, and supportability.

## Current status

**All ten phases (5 through 10) are implemented, evidenced, and their acceptance gates are checked. `docs/executed-test-report.md` reports all of FT-01–FT-19 as Passed.**

Delivered and evidenced (see `docs/evidence/` and `docs/executed-test-report.md`):

- Flyway-owned PostgreSQL schema with the approved order/line/task states, constraints, and append-only `stock_movement` and `task_transition` ledgers;
- the full `/api/v1` HHT and admin REST surface (`API.md`): authentication, FIFO claim/scan/confirm, admin order/task/catalog/stock endpoints;
- QR location/article labels (deterministic PNG/A4 PDF) and a session-authenticated, polling admin dashboard (Phase 8);
- preprod startup validation with a safe diagnostic, structured JSON operational logging, and the configuration matrix/runbook/log-analysis-guide/incident-record-template documentation set (Phase 9);
- the `OrderCompletionPublisher` MFC extension seam and its no-op adapter, with documented (not implemented) future TCP boundaries (Phase 10);
- migration/integrity/API/concurrency/idempotency/recovery integration tests against the digest-pinned `postgres:17.10-alpine` image, plus Checkstyle and SpotBugs, all in `mvn verify`.

The one open item across the whole plan is a fresh-machine/clean-environment rehearsal of `docs/runbook.md` — recorded as a residual item, not yet performed on a genuinely clean machine.

See `PLAN.md` for details and progress rules.

## Prerequisites — owner managed

The approved baseline (ADR 0002) is Eclipse Temurin JDK 21, Maven 3.9.16, and Docker Desktop with Compose v2 on 64-bit Windows with virtualization enabled. The project owner installs and updates workstation tools.

The project owner may collect version evidence from a new PowerShell session:

```powershell
java -version
mvn -version
docker --version
docker compose version
```

## Start the development database and application

Docker Compose is the approved development route (ADR 0002); the image is pinned by immutable digest. Record first-start evidence per ADR 0006.

1. Optionally copy `.env.example` to `.env` and change development-only database values. Both Docker Compose and the Spring `dev` profile read this file.
2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   docker compose ps
   ```

3. Start the application. Flyway creates and seeds the schema automatically:

   ```powershell
   mvn spring-boot:run
   ```

4. Check database-backed application health:

   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

5. Open a SQL session when running the diagnostic pack:

   ```powershell
   docker compose exec postgres psql -U wms -d wms
   ```

Development seed users are `admin` / `admin123` and `picker01` / `picker123`. They exist only to support the PoC and must not be reused outside development.

## Build and test

This is the validated verification path (see `docs/evidence/2026-07-13-phase6-maven-verify.md`).

Unit tests run without integration tests:

```powershell
mvn test
```

The full verification starts a disposable PostgreSQL container and validates migrations, seed reconciliation, credentials, and append-only movement enforcement:

```powershell
mvn verify
```

A working Docker runtime is required for `mvn verify`.

## Reset the development database

**This is destructive.** It permanently deletes local WMS database data and reruns all migrations on the next start:

```powershell
docker compose down -v
docker compose up -d
```

Never use volume deletion as a preproduction recovery procedure.

## Configuration

The default profile is `dev`. It adds `db/devdata` to Flyway and therefore installs demonstration fixtures. Preproduction scans only `db/migration`, has no committed credentials or demo users, and fails when required database environment variables are absent.

| Variable | Development default | Purpose |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Selects `dev` or `preprod` |
| `WMS_DB_URL` | `jdbc:postgresql://localhost:5432/wms` | JDBC connection URL |
| `WMS_DB_USERNAME` | `wms` | Database login |
| `WMS_DB_PASSWORD` | `wms_dev_password` | Database password |
| `WMS_DB_NAME` | `wms` | Compose database name |
| `WMS_DB_PORT` | `5432` | Compose host-side database port |
| `WMS_SERVER_ADDRESS` | `0.0.0.0` | API bind address |
| `WMS_SERVER_PORT` | `8080` | API and dashboard port |
| `WMS_TASK_STUCK_THRESHOLD` | `PT30M` | Active-state age treated as stuck |
| `WMS_AUTH_TOKEN_TTL` | `PT8H` | HHT token lifetime |
| `WMS_DASHBOARD_POLL_INTERVAL` | `PT2S` | Admin dashboard client-side polling interval |
| `WMS_MFC_ADAPTER` | `noop` | Selects the `OrderCompletionPublisher` adapter; `noop` is the only implemented value |

Local secrets belong in `.env` or process environment variables, never committed files. Only the application port will be opened to the LAN in the installation runbook; Compose binds PostgreSQL to `127.0.0.1` deliberately. `docs/configuration-matrix.md` is the authoritative, complete reference (owner, sensitivity, environment, restart requirement for every parameter); the table above is a quick-start subset.

## Docker Compose versus native PostgreSQL on Windows

Docker Compose is the approved primary route (ADR 0002, decision D-02) because it pins the PostgreSQL image by immutable digest, creates the database consistently, keeps extensions and data isolated, and makes reset/integration testing reproducible. The tradeoff is Docker Desktop's installation size, memory use, virtualization dependency, and possible corporate licensing/policy restrictions.

A native PostgreSQL 17 installation remains the documented fallback. It starts faster and avoids a VM layer, but Windows service setup, `pg_hba.conf`, extension availability, upgrades, data cleanup, and troubleshooting become machine-specific.

## Key documents

- `PLAN.md` — phased delivery plan and acceptance gates.
- `API.md` — the implemented and evidenced HHT/admin REST contract, plus the Phase 8 label and dashboard endpoints.
- `docs/configuration-matrix.md` — every parameter's owner, default, sensitivity, environment, and restart requirement.
- `docs/runbook.md` — clean-environment Windows install, firewall scoping, LAN/HHT check, and rollback.
- `docs/sql-diagnostics.md` — stuck-task, ledger-reconciliation, and order-trace SQL.
- `docs/log-analysis-guide.md` — structured-log field reference and worked diagnosis examples.
- `docs/incident-record-template.md` — the template for recording a real operational incident.
- `docs/architecture.md` — module boundaries, transactions, and the MFC seam.
- `docs/decisions/` — ADRs 0001–0008; ADR 0002 records the approved technology baseline.
- `docs/evidence/` — retained runtime and test evidence per build/configuration identifier.
- `docs/functional-test-specification.md`, `docs/requirements-traceability.md`, and `docs/executed-test-report.md` — numbered cases, requirement mapping, and aggregated pass/fail/blocked status for FT-01–FT-19.
- `docs/research/` — Phase 2/3 research, decision packet, and validation log.

## Repository layout

```text
.github/workflows/       CI pipeline
src/main/java/           Spring Boot application and future domain modules
src/main/resources/      profile configuration and Flyway migrations
src/test/java/           unit and PostgreSQL integration tests
docs/                    diagnostics, decisions, specifications, and runbooks
compose.yaml              local PostgreSQL service
```
