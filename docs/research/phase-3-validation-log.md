# Phase 3 validation log

**Status:** Design decisions approved; runtime evidence captured and implementation opened 2026-07-13 (section 8)  
**Validation dates:** 2026-07-12 through 2026-07-13  
**Authority:** `PLAN.md` remains the approval source

## 1. Boundary and method

This log records evidence gathered for the Phase 3 decision checkpoint. It does
not approve D-01 through D-17 and does not open Phase 4.

The validation used read-only workstation discovery, repository inspection,
official web documentation, Maven Central metadata, Docker Hub metadata, and
owner responses. No project build, server, container, migration, database,
integration test, or application implementation was run or changed. Docker
Desktop was not started.

Machine-specific installation paths, personal settings, credentials, tokens,
and licence data are intentionally omitted.

## 2. Precondition status

| Phase 3 precondition | Result | Evidence or remaining work |
|---|---|---|
| Record owner-installed workstation versions | **Partially validated** | Versions are recorded in section 3. Docker engine/container execution remains deferred. A native `psql` client is not on `PATH`, which is acceptable for the proposed container route. |
| Recheck every Phase 2 source | **Validated for reachability** | All 36 registered URLs returned HTTP 200 on 2026-07-12 with no observed redirect to a replacement source. Findings remain research, not decisions. |
| Confirm Spring Boot support status and managed versions | **Validated for candidates** | Spring marks 4.1.0, 4.0.7, and 3.5.16 as GA and 4.1.0 as current. Spring's policy gives Boot minor releases at least 13 months of support. Section 5 compares 4.0.7 and 4.1.0. |
| Confirm PostgreSQL image and component compatibility | **Documentation validated; runtime pending** | PostgreSQL 17.10 is the current supported 17.x minor; the official `postgres:17.10-alpine` tag exists; pgJDBC, Flyway, and Testcontainers documentation supports the proposed combination. Runtime proof is deferred. |
| Determine whether provisional V1 was applied to retained data | **Owner declaration recorded** | The owner reported no retained/shared database or volume requiring the provisional V1 history. This supports the D-14 replace-baseline branch after approval, but no migration was edited or database destroyed. The stopped Docker engine prevented an independent volume inspection. |
| Confirm Docker Desktop acceptability | **Owner confirmed; runtime pending** | The owner confirmed Docker is acceptable as the primary local route after considering terms, policy, resources, and virtualization. The owner requested runtime validation only after a separate explicit instruction. |

The owner approved D-04 through D-13 and D-15 through D-17 on 2026-07-13 using
the MVP recommendation and authorized preparation of the consolidated ADRs and
specifications. The implementation gate remained closed at that point because
runtime evidence was incomplete and there was no separate instruction to begin
implementation. *(Superseded later on 2026-07-13: the runtime evidence was
captured and the owner instructed implementation to proceed — see section 8.)*

## 3. Workstation evidence

| Item | Observed result | Assessment |
|---|---|---|
| Operating system | Windows 11 Pro, version 10.0.26200, build 26200, 64-bit/AMD64 | Suitable architecture for the x64 toolchain observed. |
| Git | 2.54.0.windows.1 | Installed and available from PowerShell. |
| Java runtime/compiler | Eclipse Temurin OpenJDK 21.0.11+10 LTS; `javac` 21.0.11 | Matches the Java 21 candidate. `JAVA_HOME` is set. |
| Maven | 3.9.16 | Matches the 3.9.x candidate and uses Temurin Java 21.0.11 with UTF-8 platform encoding. |
| Docker | Client 29.6.1, API 1.55 | Installed. The `desktop-linux` engine was not reachable because Docker Desktop was stopped. |
| Docker Compose | v5.2.0 | Plugin is installed; no project Compose command was run. |
| WSL | 2.7.3.0; kernel 6.6.114.1-1 | WSL 2 is installed. Registered distributions were stopped. |
| IntelliJ IDEA | 2026.1.4 | Installed; no Maven import or IDE migration validation was performed. |
| JetBrains Toolbox | 3.6.1.0 | Installed. |
| Visual Studio Code | 1.127.0 x64 | Installed. |
| Native PostgreSQL client | Not available on `PATH` | Not required if D-02 approves the container-executed `psql` route. |

Not yet evidenced: Docker engine operation, `docker run --rm hello-world`, the
project PostgreSQL image pull/start, Maven/IntelliJ equivalence, and any
project build or test. These remain deliberately deferred.

## 4. Live-source validation

### Registered Phase 2 sources

All 36 entries in `phase-2-research.md` sections WMS-01 through OPS-03 returned
HTTP 200 on 2026-07-12. This verifies reachability only; it does not imply that
every source finding or project interpretation has been approved.

### Additional version-specific sources

Accessed 2026-07-12:

| Source | URL | Validation finding |
|---|---|---|
| Spring Boot system requirements | https://docs.spring.io/spring-boot/system-requirements.html | Boot 4.0.7 and 4.1.0 require Java 17+, support Java through 26, and support Maven 3.6.3+. |
| Spring projects data | https://spring.io/page-data/projects/page-data.json | Lists Boot 3.5.16, 4.0.7, and 4.1.0 as GA; 4.1.0 is current. |
| Spring support policy | https://spring.io/support-policy | Boot minor releases receive at least 13 months of support. |
| Spring Boot BOMs | https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/ | Supplies the managed-version comparison in section 5. |
| PostgreSQL versioning | https://www.postgresql.org/support/versioning/ | PostgreSQL 17.10 is current and major 17 is supported through 2029-11-08. |
| Official PostgreSQL image | https://hub.docker.com/_/postgres | Lists `17.10-alpine` among supported tags and includes AMD64. |
| Docker tag metadata | https://hub.docker.com/v2/repositories/library/postgres/tags/17.10-alpine | Tag exists; observed index digest `sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193`, updated 2026-07-08. Recheck before pinning because tags and indexes can change. |
| pgJDBC | https://jdbc.postgresql.org/download/ | The 42.7 driver line supports PostgreSQL 8.4+ and Java 8+; PostgreSQL 17 and Java 21 are within range. |
| pgJDBC current release | https://jdbc.postgresql.org/ | 42.7.13 is current. The page states that 42.7.12 fixed CVE-2026-54291. |
| Flyway PostgreSQL support | https://documentation.red-gate.com/fd/postgresql-277579325.html | Page updated 2026-07-09, identifies verified PostgreSQL endpoints 9.2 and 18, and requires the separate `flyway-database-postgresql` module. PostgreSQL 17 is within the documented range. |
| Testcontainers PostgreSQL | https://java.testcontainers.org/modules/databases/postgres/ | Documents `PostgreSQLContainer`, the standard `postgres` image, and module version 2.0.5. |

The Spring navigation's former `/release-calendar` route returned HTTP 404.
The current official project data and support-policy pages were used instead;
no dead URL was added to the Phase 2 register.

## 5. D-01 technology validation

### Supported Spring candidates

| Managed item | Boot 4.0.7 | Boot 4.1.0 |
|---|---:|---:|
| Spring Framework | 7.0.8 | 7.0.8 |
| Spring Data BOM | 2025.1.6 | 2026.0.0 |
| Spring Security | 7.0.6 | 7.1.0 |
| Hibernate ORM | 7.2.19.Final | 7.4.1.Final |
| Flyway | 11.14.1 | 12.4.0 |
| PostgreSQL JDBC | 42.7.11 | 42.7.11 |
| Testcontainers | 2.0.5 | 2.0.5 |
| JUnit Jupiter | 6.0.3 | 6.0.3 |
| Surefire/Failsafe | 3.5.6 | 3.5.6 |

Maven Central records Boot 4.0.0 on 2025-11-20, Boot 4.0.7 on 2026-06-10,
and Boot 4.1.0 on 2026-06-10. Under the published minimum 13-month policy,
the 4.0 line is within support on the validation date.

### Validation interpretation, not a decision

**Recommend Boot 4.0.7 for owner review as the conservative candidate.** It is
a supported patched 4.x line and avoids selecting the newly current 4.1 line
solely because it is newest. Boot 4.1.0 remains a valid alternative if an
identified 4.1 capability justifies the newer Spring Data, Security, Hibernate,
and Flyway generations.

The current `pom.xml` value 4.1.0 remains provisional and was not changed.

### Security exception to both BOMs

Both BOMs manage pgJDBC 42.7.11. The authoritative pgJDBC site states that
42.7.12 fixed a silent channel-binding authentication downgrade,
CVE-2026-54291, and lists 42.7.13 as current. Therefore 42.7.11 must not be
accepted unchanged. The candidate baseline should explicitly use pgJDBC
42.7.13 unless a later Spring BOM manages an equal or newer fixed release.

An automated dependency-CVE lookup reported no known CVEs for the sampled
4.0.7 and 4.1.0 components, but it did not report this new pgJDBC advisory.
The authoritative vendor finding takes precedence and demonstrates why the
automated result is supporting evidence rather than a sole approval gate.

### Approved exact baseline; runtime acceptance pending

The owner approved D-01 with the listed pgJDBC change on 2026-07-12. This is a
design decision, not runtime acceptance and not authorization to edit `pom.xml`.

| Component | Approved D-01 value | Validation status |
|---|---:|---|
| Java | Eclipse Temurin 21.0.11+10 LTS | Installed and validated. |
| Maven | 3.9.16 | Installed and validated. |
| Spring Boot | 4.0.7 | Documentation validated; build/runtime proof pending. |
| PostgreSQL server/image | 17.10 / `postgres:17.10-alpine` | Documentation validated; runtime and immutable image pin pending. |
| Flyway | 11.14.1 | Managed by Boot 4.0.7; runtime migration proof pending. |
| PostgreSQL JDBC | 42.7.13 override | Security correction to Boot's 42.7.11 management; Maven/runtime proof pending. |
| Testcontainers | 2.0.5 | Managed by Boot 4.0.7; runtime proof pending. |

No application artifact was changed to apply these values.

## 6. D-02 and D-14 owner evidence

The owner provided these responses on 2026-07-12 and then recorded formal
approval of D-02 and D-14:

| Topic | Owner response | Consequence if approved in the formal decision record |
|---|---|---|
| Development database route | Docker acceptable as the primary route | D-02 approved. Runtime validation remains separate. |
| Provisional migration history | No retained database | D-14 approved for replacing provisional V1 with an approved clean baseline after the complete design gate. No migration change is authorized yet. |
| Runtime validation timing | Validate later on request | Docker Desktop must not be started and no container may be run without a separate explicit request. |

The formal decision records are in `phase-3-decision-packet.md`.

## 7. Decision-readiness review

| Decisions | Validation state | Required next action |
|---|---|---|
| D-01 | Approved with Boot 4.0.7 and pgJDBC 42.7.13 override | Later Maven/runtime evidence validates the approved design values. |
| D-02 and D-14 | Approved | Later perform the explicitly requested runtime/database-volume checks; replace provisional V1 only after the complete design gate. |
| D-03 | Approved: Java only | Consolidate the accepted choice in the technology ADR; Kotlin remains excluded. |
| D-04 through D-06 | Approved with MVP recommendation | ADR 0003 prepared; implementation and concurrency evidence remain pending. |
| D-07 through D-10 | Approved with MVP recommendation | ADRs 0004 and 0005 plus API specification prepared; execution evidence remains pending. |
| D-11 through D-13 | Approved with MVP recommendation | ADR 0006 plus traceability and functional-test specifications prepared. |
| D-15 and D-16 | Approved with MVP recommendation | ADR 0007 prepared; library/licence and runtime evidence remain pending. |
| D-17 | Approved with MVP recommendation | ADR 0008 prepared; scope review remains part of implementation acceptance. |

The bounded recommendations and conservative defaults proposed to resolve these
remaining details are recorded in
`docs/research/phase-3-mvp-recommendation.md`. They are not owner decisions.

## 8. Remaining gate blockers

- [x] Record **Approve**, **Approve with change**, or **Reject** for D-04 through
  D-13 and D-15 through D-17, with reviewer and date. D-01 through D-03 and
  D-14 are recorded.
- [x] Resolve the detailed state, transition, API retry, authentication, QR,
  dashboard, configuration, logging, and evidence parameters identified in the
  decision packet and section 7. Evidence: ADRs 0003 through 0008, `API.md`,
  `docs/functional-test-specification.md`, `docs/requirements-traceability.md`.
- [x] Write consolidated ADRs whose status matches the owner decisions.
- [x] On a separate explicit request, start Docker and capture engine,
  disposable-container, image, Compose, and Testcontainers/runtime evidence.
  Evidence: Docker Desktop engine 29.6.1 and Testcontainers runtime captured
  2026-07-13 in `docs/evidence/2026-07-13-phase6-maven-verify.md`. Starting the
  Compose development service remains a Phase 6/9 runbook item.
- [x] After approval, decide and record the immutable PostgreSQL image reference
  and perform the migration policy selected by D-14. Evidence: digest
  `sha256:742f40ea…` pinned in `compose.yaml` and `FlywayMigrationIT`; the
  provisional V1 was replaced by the approved baseline before any application.
- [x] Receive a separate explicit owner instruction to begin implementation.
  The owner instructed implementation to proceed on the approved baseline
  (2026-07-13).

The Phase 3 gate conditions are met and the owner opened implementation on
2026-07-13. Phase 5/6 progress and runtime evidence are tracked in `PLAN.md`
and `docs/evidence/`. Artifacts belonging to later phases remain provisional
until their phase is implemented and evidenced.
