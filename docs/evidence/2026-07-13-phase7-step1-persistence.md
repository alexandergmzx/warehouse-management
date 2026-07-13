# Phase 7 Step 1 evidence — persistence layer on the pinned baseline

**Build/configuration identifier:** `2d93dd7+phase7-step1-persistence / 2026-07-13T23:18:49Z`
(git HEAD `2d93dd7` — the committed Step 0 baseline — plus the Step 1 working
tree; the identifier becomes immutable when Step 1 is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 1 (`PLAN.md`): the
persistence layer that maps the Flyway-owned schema, with **no domain behavior**.

- JPA entities for all 11 tables, grouped by the ADR/architecture modules
  (`identity`, `catalog`, `inventory`, `orders`, `picking`);
- a Spring Data repository per entity;
- named `JdbcTemplate` scaffolding for the PostgreSQL-specific read paths
  (availability, reconciliation) per ADR 0003;
- Hibernate runs in `validate` mode only — it never creates or updates schema.

This record does **not** claim any Phase 7 acceptance-gate behavior (login,
claim, scan, confirm, admin recovery); those slices remain to be built.

## What was added

| Area | Artifacts |
|---|---|
| `identity` | `AppUser`, `Device`, `AuthToken` (+ `UserRole`) and their repositories |
| `catalog` | `Article`, `Location` and their repositories |
| `inventory` | `Stock` (+ `StockId` composite key), `StockMovement` (+ `MovementType`), their repositories, and `StockJdbcRepository` (+ `StockLedgerDiscrepancy`) |
| `orders` | `CustomerOrder`, `OrderLine` (+ `OrderStatus`, `OrderLineStatus`) and their repositories |
| `picking` | `PickingTask`, `TaskTransition` (+ `TaskStatus`) and their repositories |
| build | `spring-boot-starter-test` (test scope); `config/spotbugs/spotbugs-exclude.xml` |

### Mapping decisions (recorded)

- **Foreign keys are held as scalar identifiers, not JPA associations.** The
  schema has overlapping composite foreign keys (`picking_task` and
  `stock_movement` both reuse `article_id`), and ADR 0003 puts the locking,
  allocation, FIFO-claim, and reconciliation paths in narrowly scoped JDBC. A
  scalar-id mapping maps the schema faithfully, passes `validate` without
  overlapping-column workarounds, and avoids lazy-association pitfalls in the
  concurrency-sensitive flows. Simple, non-overlapping relationships can adopt
  associations later if a read genuinely needs them.
- **`TIMESTAMPTZ` → `OffsetDateTime`; `CHAR(64)` (`token_hash`) → `String` with
  `@JdbcTypeCode(SqlTypes.CHAR)`; check-constrained text columns → string
  enums (`@Enumerated(STRING)`).** All accepted by Hibernate `validate` (the
  context started).
- **`stock.version` is a plain value, not `@Version`.** Concurrency is
  pessimistic row locking under `READ COMMITTED` (ADR 0003); an implicit
  optimistic-lock column would conflict with the locking SQL.
- **Trigger/default-owned audit timestamps (`created_at`, `updated_at`,
  `last_transition_at`) are mapped read-only** (`insertable=false, updatable=false`)
  so the database keeps ownership.
- **JDBC scaffolding is read-only.** `StockJdbcRepository` implements the
  availability and reconciliation reads; the mutating locking SQL
  (`FOR UPDATE OF task SKIP LOCKED`; ascending `(article_id, location_id)`
  allocation lock) is deferred to Steps 3 and 5 with their transactions, so no
  row-locking SQL runs in this step.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B clean verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13T17:18:37-06:00, total time 39.8 s |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `FlywayMigrationIT` (Step 0 schema/seed) | Tests run 5, failures 0, errors 0 |
| `PersistenceLayerIT` (validate + repo + JDBC reads) | Tests run 4, failures 0, errors 0 |
| Hibernate schema validation (`ddl-auto=validate`) | Context started — all 11 entity mappings match the Flyway schema |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` |

`PersistenceLayerIT` proves, against the migrated container:

- every entity mapping validates against the Flyway schema (context boot);
- repositories read the deterministic seed fixtures (users/roles, devices,
  articles + QR values, locations, stock, orders/lines, the 5 picking tasks by
  status, the completed task's 5-row transition history, and the 6 movement
  ledger rows including the single `PICK` of `-2`);
- availability = on-hand − unfinished-task reservations returns 0 / 9 / 13 for
  the three checked bins (ADR 0003 formula);
- stock reconciles to the movement ledger with an empty discrepancy result.

## Deviations and notes

- **SpotBugs `EI_EXPOSE_REP2` on `StockJdbcRepository`.** The first verify run
  reported one Medium bug: storing the constructor-injected
  `NamedParameterJdbcTemplate` in a field. This is the well-known Spring
  dependency-injection false positive. Resolved with a narrowly scoped
  `config/spotbugs/spotbugs-exclude.xml` that excludes `EI_EXPOSE_REP2` only for
  Spring stereotype classes (`*Repository|*Service|*Controller|*Filter|*Configuration`).
  Domain entities and value objects (which return only immutable types) are not
  matched and remain fully analysed. Recorded per the change-discipline rule.
- **Mockito self-attach warning.** `spring-boot-starter-test` brings Mockito;
  it prints a self-attaching JDK-agent warning. No Mockito is used yet; the
  warning is benign and can be silenced with the Mockito agent if/when mocks
  are introduced.
