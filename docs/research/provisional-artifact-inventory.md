# Provisional artifact inventory

**Status:** Research recommendation; not an approved implementation baseline
**Inventory date:** 2026-07-12
**Authority:** `PLAN.md` remains authoritative

## Purpose

This inventory completes the Phase 0 review of the workspace after the initial
Phase 2 research synthesis. It records what is useful in the existing drafts
and recommends whether each artifact should be **keep**, **revise**,
**replace**, or **remove** during the post-approval rebaseline.

The classifications do not approve technology versions, schema, API,
configuration, CI, or application behavior. No application artifact may be
changed from this inventory before the Phase 3 gate is explicitly approved.

## Classification meanings

- **Keep** — retain the artifact and its present role; normal review and small
  documentation corrections may still be required.
- **Revise** — retain the artifact, but reassess or change material content
  after the relevant design decision is approved.
- **Replace** — create an approved successor rather than evolving the draft.
- **Remove** — exclude the artifact from the approved repository or delivery.

## Repository artifacts

| Artifact | Recommendation | Evidence and required follow-up |
|---|---|---|
| `.env.example` | Revise | The separation of local values is useful. Reconcile names/defaults with the approved configuration matrix and retain development-only credentials only. |
| `.github/copilot-instructions.md` | Keep | It mirrors the repository-wide authorization boundary and confirmed workflow. Keep it aligned with `CLAUDE.md`. |
| `.github/workflows/ci.yml` | Revise | Least-privilege permissions, Maven verification, and report upload are useful. Revalidate the JDK and Maven baseline, pin actions according to the approved update policy, and approve retention/quality gates. |
| `.gitignore` | Keep | It excludes IDE state, local configuration, logs, reports, and Maven output as required by the portability rules. |
| `API.md` | Revise | It captures the small HHT/admin surface, exact quantity, FIFO claim, and confirmation retry intent. Reconcile it with the approved recovery operation, scan retry semantics, RFC 9457 decision, authentication lifecycle, and final state model. |
| `CLAUDE.md` | Keep | It is an active repository rule document and is aligned with the current gate. |
| `PLAN.md` | Keep | It is the authoritative roadmap and approval source. Status changes require evidence. |
| `README.md` | Revise | It currently presents provisional setup and execution instructions as an implemented phase. Keep a project overview, but publish executable instructions only after the corresponding acceptance evidence exists. |
| `compose.yaml` | Revise | Loopback database exposure and persistent development storage are useful. Retain only if the database-route decision approves Compose, and revalidate the exact image and credential model. |
| `config/checkstyle/checkstyle.xml` | Revise | A portable Maven quality configuration is useful. Reassess the rule set and plugin integration against the approved quality standard. |
| `docs/architecture.md` | Revise | The modular-monolith boundaries and MFC port direction are useful proposals. Reconcile all modules, dependency rules, lock order, and phase references with approved ADRs. |
| `docs/decisions/0001-build-and-database.md` | Revise | The Maven/Compose rationale is useful, but its accepted status and exact versions predate the design gate. Replace its proposal with an approved ADR decision after live version and workstation checks. |
| `docs/research/phase-2-research.md` | Keep | It contains the Phase 2 source register and draft design outputs. Preserve it as research evidence; resolve its open questions before approval. |
| `docs/research/provisional-artifact-inventory.md` | Keep | This is the Phase 0 classification evidence. Update it only when a decision changes a recommendation. |
| `docs/research/phase-3-decision-packet.md` | Keep | This is the owner review worksheet for the unopened Phase 3 gate; it is not itself approval. |
| `docs/sql-diagnostics.md` | Revise | Stuck-task, ledger-reconciliation, and order-trace queries are valuable support drafts. Revalidate every query against the approved schema and remove obsolete phase claims before operational use. |
| `pom.xml` | Revise | Maven, Java 21, quality plugins, and test separation support portability. Revalidate every dependency/plugin version and the persistence/security/test dependencies after the technology decisions. |
| `src/main/java/com/alexandergomez/wms/WarehouseManagementApplication.java` | Keep | The minimal bootstrap contains no warehouse behavior. Retain only if the approved baseline remains Spring Boot and preserve the package after naming review. |
| `src/main/resources/application.yml` | Revise | Flyway ownership and Hibernate validation match confirmed rules. Reconcile defaults, bind address, health exposure, startup validation, and approved properties. |
| `src/main/resources/application-dev.yml` | Revise | Development fixture isolation is useful. Revalidate import behavior, SQL logging, and development credentials against the final configuration model. |
| `src/main/resources/application-preprod.yml` | Revise | External database values and fixture exclusion are useful. Add only approved fail-closed validation, logging, and security settings later. |
| `src/main/resources/db/migration/V1__create_schema.sql` | Revise | It demonstrates relational constraints, active-assignment indexes, and an append-only movement guard, but predates research. The owner must first inventory any database that has applied it and choose replace-provisional-V1 or preserve-and-forward-fix. If replacement is approved, change this classification to **Replace** before editing it. |
| `src/main/resources/db/devdata/V1_1__seed_demo_data.sql` | Revise | The available, stuck, completed, multi-bin, and reconciled-ledger scenarios are useful. Rebuild or adapt fixtures only after the schema, password hashing, and deterministic evidence rules are approved. |
| `src/test/java/com/alexandergomez/wms/database/FlywayMigrationIT.java` | Revise | The draft tests migration, fixtures, reconciliation, append-only behavior, and relational rejection. Rework it against approved migrations, pinned Testcontainers/PostgreSQL versions, and the final migration test matrix. |

No tracked artifact is currently recommended for unconditional replacement or
removal. That is deliberate: the migration policy and other disputed choices
are still open, so deleting or replacing their evidence now would cross the
current authorization boundary.

## Local-only workspace artifacts

| Artifact | Recommendation | Evidence and handling |
|---|---|---|
| `.git/` | Keep | Repository history and Git metadata are required locally but are not a deliverable artifact. |
| `.idea/` | Remove | Remove from any repository/delivery inventory and keep ignored. Local IntelliJ state may remain on the owner's workstation. |
| `target/` | Remove | Generated classes and JARs are not source or acceptance evidence. Keep ignored; regenerate only after execution is authorized. |

## Useful behavior represented by the drafts

The following items are design evidence only; they are not verified or
accepted behavior:

1. Common migrations and development fixtures use separate Flyway locations.
2. Hibernate is configured to validate rather than create/update the schema.
3. PostgreSQL is exposed only on loopback in the Compose draft.
4. The schema attempts nonnegative stock, deterministic task sequencing,
   one active task per user/device, task/line/stock consistency, and an
   append-only movement ledger.
5. Fixtures represent available work, multi-bin allocation, a stuck task, a
   completed pick, and stock-to-movement reconciliation.
6. The API draft represents location/article scanning, exact-quantity
   confirmation, atomic claim intent, and retry-safe final confirmation.
7. Diagnostics attempt stuck-task detection, stock-ledger reconciliation, and
   end-to-end order tracing.
8. CI and Maven drafts attempt editor-independent verification and retention of
   unit/integration test reports.
9. Configuration drafts separate development defaults from externally supplied
   preproduction database credentials.
10. The architecture draft keeps the HHT as a REST client and MFC as a future
    application-port seam without TCP implementation.

## Decisions that may change classifications

- The exact supported technology baseline can change `pom.xml`, CI, Compose,
  tests, and configuration recommendations.
- The local database route can retain or remove `compose.yaml`.
- The provisional V1 policy can change the migration recommendation from
  **Revise** to **Replace**.
- The approved API/error/idempotency model can require substantial replacement
  of `API.md` rather than revision.
- The approved audit model can add a task-transition ledger and require the
  schema, diagnostics, and test drafts to be replaced.

## Inventory conclusion

The workspace contains useful provisional evidence, but no application,
schema, API, configuration, CI, or operational draft is approved merely because
it exists or was previously compiled. The next permitted step is owner review
of the Phase 3 decision packet. Implementation remains blocked.
