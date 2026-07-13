# Miniature Warehouse Management System

A Java 21/Spring Boot proof of concept focused on warehouse picking, SQL diagnostics, configuration discipline, test evidence, and supportability.

## Current status

Phase 1 is scaffolded:

- PostgreSQL schema and deterministic seed data managed by Flyway;
- append-only stock movement ledger;
- released, stuck, and completed demonstration orders;
- migration/integrity integration test against real PostgreSQL;
- HHT REST contract and phased implementation plan;
- SQL diagnostic query pack;
- development and preproduction configuration profiles;
- GitHub Actions verification workflow.

The REST controllers and picking services described in the contract are phase 2 and are not implemented yet.

## Prerequisites

- 64-bit Windows 10 or 11 with virtualization enabled;
- Java Development Kit 21;
- Maven 3.9 or newer;
- Docker Desktop with Docker Compose v2.

Verify the tools from PowerShell:

```powershell
java -version
mvn -version
docker --version
docker compose version
```

## Start phase 1 locally

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

This permanently deletes local WMS database data and reruns all migrations on the next start:

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

Local secrets belong in `.env` or process environment variables, never committed files. Only the application port will be opened to the LAN in the installation runbook; Compose binds PostgreSQL to `127.0.0.1` deliberately.

## Docker Compose versus native PostgreSQL on Windows

Docker Compose is recommended for this PoC because it pins the PostgreSQL major/image version, creates the database consistently, keeps extensions and data isolated, and makes reset/integration testing reproducible. The tradeoff is Docker Desktop's installation size, memory use, virtualization dependency, and possible corporate licensing/policy restrictions.

A native PostgreSQL 17 installation can use the same database name, user, password, and port. It starts faster and avoids a VM layer, but Windows service setup, `pg_hba.conf`, extension availability, upgrades, data cleanup, and troubleshooting become machine-specific. For a job-application demonstration, the reproducibility of Docker is more valuable, so native installation is an exception rather than the primary path.

## Key documents

- `PLAN.md` — phased delivery plan and acceptance gates.
- `API.md` — explicit HHT/admin REST contract.
- `docs/sql-diagnostics.md` — stuck-task, ledger-reconciliation, and order-trace SQL.
- `docs/architecture.md` — module boundaries, transactions, and the MFC seam.
- `docs/decisions/0001-build-and-database.md` — Maven/PostgreSQL decision record.

## Repository layout

```text
.github/workflows/       CI pipeline
src/main/java/           Spring Boot application and future domain modules
src/main/resources/      profile configuration and Flyway migrations
src/test/java/           unit and PostgreSQL integration tests
docs/                    diagnostics, decisions, specifications, and runbooks
compose.yaml              local PostgreSQL service
```
