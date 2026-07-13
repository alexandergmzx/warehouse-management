# Phase 6 runtime evidence — Maven verification on the pinned baseline

**Build/configuration identifier:** `b51d4ed+phase5-6-rebaseline / 2026-07-13T21:41:17Z`
(git HEAD `b51d4ed5c89a0938272cae7053e813deb9470fbd` plus the Phase 5/6 rebaseline
working tree; the identifier becomes immutable when the rebaseline is committed.)

## Scope

Objective evidence for the ADR 0002 runtime-acceptance items that remained open
after the Phase 3 design approvals:

- the approved Spring Boot 4.0.7 / pgJDBC 42.7.13 stack compiles and verifies;
- a clean PostgreSQL 17.10 database migrates without manual SQL;
- the migration/integrity test suite passes against the digest-pinned image;
- static quality gates (Checkstyle, SpotBugs) pass.

This record does not claim Phase 7+ behavior, Compose service validation, or
diagnostic-query execution evidence; those remain open.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B verify` |
| Maven | 3.9.16 |
| JDK | Eclipse Temurin 21.0.11 (`jdk-21.0.11.10-hotspot`), UTF-8 platform encoding |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1, API 1.55 |
| Testcontainers | 2.0.5 (Boot 4.0.7 managed) |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Server version reported by Flyway | PostgreSQL 17.10 |
| Finished at | 2026-07-13T15:41:17-06:00 (21:41:17Z), total time 01:10 min |

The image digest equals the Docker Hub index digest observed on 2026-07-12 in
`docs/research/phase-3-validation-log.md` §4 and the local `docker images
--digests` RepoDigest, satisfying the ADR 0002 immutable-pin requirement. The
same reference is pinned in `compose.yaml` and
`src/test/java/com/alexandergomez/wms/database/FlywayMigrationIT.java`.

## Results

| Gate | Result |
|---|---|
| Compilation and packaging (Boot 4.0.7 repackage) | Success |
| Flyway validation | 2 migrations validated |
| Flyway migration on empty schema | `V1 create schema`, `V1.1 seed demo data` applied, 0.592 s |
| Failsafe `FlywayMigrationIT` | Tests run 5, failures 0, errors 0, skipped 0 (36.57 s) |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` |

The five integration tests cover: full migration plus seed-scenario assertions
(including the `task_transition` ledger and multi-bin split), seed-stock
reconciliation against the append-only movement ledger, rejection of
`stock_movement` mutations, rejection of `task_transition` mutations, and
rejection of inconsistent task/movement relationships.

## Raw log excerpt (tail of the run)

```text
[INFO] Running com.alexandergomez.wms.database.FlywayMigrationIT
15:40:36.288 [main] INFO org.testcontainers.DockerClientFactory -- Connected to docker:
  Server Version: 29.6.1
  API Version: 1.55
  Operating System: Docker Desktop
15:40:45.166 [main] INFO tc.postgres:17.10-alpine@sha256:742f40... -- Creating container for image: postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193
15:40:50.845 [main] INFO tc.postgres:17.10-alpine@sha256:742f40... -- Container postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193 started in PT5.6791667S
15:40:52.175 [main] INFO org.flywaydb.core.FlywayExecutor -- Database: jdbc:postgresql://localhost:58861/wms_test?loggerLevel=OFF (PostgreSQL 17.10)
15:40:52.349 [main] INFO org.flywaydb.core.internal.command.DbValidate -- Successfully validated 2 migrations (execution time 00:00.076s)
15:40:52.655 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Migrating schema "public" to version "1 - create schema"
15:40:53.125 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Migrating schema "public" to version "1.1 - seed demo data"
15:40:53.462 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Successfully applied 2 migrations to schema "public", now at version v1.1 (execution time 00:00.592s)
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 36.57 s -- in com.alexandergomez.wms.database.FlywayMigrationIT
[INFO] --- checkstyle:3.6.0:check (checkstyle) @ warehouse-management ---
[INFO] You have 0 Checkstyle violations.
[INFO] --- spotbugs:4.10.2.0:check (spotbugs) @ warehouse-management ---
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
[INFO] Total time:  01:10 min
[INFO] Finished at: 2026-07-13T15:41:17-06:00
```

## Deviation noted during pinning

The first digest-pinned run failed fast: Testcontainers rejects a
digest-qualified image reference unless it is declared compatible with the
canonical `postgres` image. The fix uses
`DockerImageName.parse(...).asCompatibleSubstituteFor("postgres")`, as
prescribed by the Testcontainers error message. Recorded per the
failed-experiment rule; no schema, data, or configuration change was involved.
