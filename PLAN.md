# Miniature WMS research and delivery plan

## Plan status

**Current stage:** implementation — **all ten phases (5 through 10) are complete and evidenced.** Only "Final acceptance"'s clean-environment runbook rehearsal remains open; no further phase gate is pending.  
**Implementation status:** authorized by the project owner after the Phase 3 design approvals (ADRs 0002–0008); Phase 8 implementation was explicitly authorized 2026-07-13 ("start the implementation"); Phase 9 implementation was explicitly authorized 2026-07-14 ("continue the implementation"); Phase 10 implementation was explicitly authorized 2026-07-14 ("continue the next phase")  
**Tool installation owner:** project owner; assistants must not install or configure system tools unless explicitly asked later

The pre-research scaffold was reviewed in the Phase 5 rebaseline. The Maven build, schema baseline, development fixtures, and migration tests now reflect the approved design (ADRs 0002–0008) and pass `mvn verify` on the digest-pinned PostgreSQL 17.10 image (`docs/evidence/2026-07-13-phase6-maven-verify.md`). The one open Phase 6 item is executing the SQL diagnostic pack against a running dev database and recording results.

**Phase 7 is complete**: all six execution-plan steps (0–5) are done and evidenced (`docs/evidence/2026-07-13-phase7-step0-argon2id.md` through `…-step5-admin-endpoints.md`), all six Phase 7 acceptance-gate criteria are checked with citations, and functional cases FT-01 through FT-14 all have evidence (FT-11 was already covered in Phase 6).

**Phase 8 is complete**: all four execution-plan steps (0–3) are done and evidenced (`docs/evidence/2026-07-14-phase8-steps0-2.md`), all three Phase 8 acceptance-gate criteria are checked with citations, and FT-17/FT-18 have evidence.

**Phase 9 is complete**: all nine execution-plan steps (0–8) are done and evidenced (`docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`), all four Phase 9 acceptance-gate criteria are checked with citations, and FT-15/FT-16 have evidence.

**Phase 10 is complete**: the `OrderCompletionPublisher` seam, its no-op adapter, and the documented (not implemented) future TCP boundaries are done and evidenced (`docs/evidence/2026-07-14-phase10-mfc-seam.md`), the Phase 10 acceptance-gate criterion is checked with citation, and FT-19 (the scope-exclusion review this phase gated) is Passed. `docs/executed-test-report.md` now aggregates all of FT-01–FT-19: **19 Passed, 0 Failed, 0 Blocked, 0 Not Applicable.** The MVP defined by `docs/functional-test-specification.md` is therefore fully executed. The single remaining item anywhere in this plan is the "Final acceptance" checklist's clean-environment runbook rehearsal, which requires an actual clean machine and is recorded as open. Artifacts belonging to any phase are no longer provisional; all are implemented and evidenced.

## Purpose

Build a miniature Warehouse Management System that demonstrates the capabilities relevant to an Application Configuration & Testing Engineer role:

- SQL and relational-data competence;
- intermediate Java and Spring design;
- configuration and parameterization discipline;
- functional testing with written specifications and retained evidence;
- log-based diagnosis and SQL troubleshooting;
- professional installation, operation, rollback, and incident documentation.

These operational deliverables are part of the product, not cleanup work at the end.

## Confirmed workflow baseline

Research may challenge implementation details, but it must preserve these confirmed requirements unless the project owner explicitly changes them:

1. Pick confirmations must equal the task quantity; partial picks are rejected in the initial PoC.
2. The HHT has no skip operation initially; blocked work requires an administrative recovery path.
3. Tasks are offered globally by order creation time, order-line number, and task sequence.
4. Claims are atomic, and a user/device may have at most one active task.
5. An order line may be split across locations in ascending location-code order.
6. Stock is decremented only when a valid pick is confirmed.
7. The stock update, task completion, order/line progression, and movement insertion occur in one transaction.
8. `stock_movement` is an append-only audit ledger for every stock change.
9. The Raspberry Pi HHT is a separate repository and communicates through a small versioned LAN REST API.
10. The MFC deliverable provides only an extension seam; it does not implement TCP telegram delivery.

## Scope control

Until the design approval gate is passed:

- do not add application features;
- do not revise the provisional schema or API;
- do not install or download development tools on behalf of the project owner;
- do not run databases, migrations, servers, or integration tests;
- do not describe provisional files as accepted or complete;
- do not select a framework/library version solely because it is newest;
- do not introduce Kotlin, a separate frontend, cloud deployment, or robot integration without a recorded decision.

## Phase 0 — Freeze and inventory the provisional workspace

### Actions

- [x] Keep the existing generated files rather than deleting them.
- [x] Mark them as provisional and unapproved.
- [x] Create an inventory after research that classifies every artifact as **keep**, **revise**, **replace**, or **remove**. Evidence: `docs/research/provisional-artifact-inventory.md`.
- [x] Record any useful behavior already represented by the drafts without treating it as a final decision. Evidence: `docs/research/provisional-artifact-inventory.md`.

### Gate

No implementation work starts from the provisional scaffold until the Phase 3 design checkpoint is approved.

## Phase 1 — User-managed workstation preparation

The project owner installs tools independently. Project assistance is limited to reviewing reported versions or troubleshooting when explicitly requested.

### Candidate baseline

- Git;
- Java Development Kit 21;
- Maven 3.9 or newer;
- Docker Desktop with Docker Compose v2, if Windows policy, licensing, virtualization, and available resources permit it;
- IntelliJ IDEA or VS Code;
- PostgreSQL command-line client only if useful outside the container.

### Installation policy

- The project owner performs every installation and accepts all license terms.
- Download installers and archives only from the official links listed below. Avoid repackaged download sites.
- Prefer stable releases; do not install EAP, preview, release-candidate, nightly, or Maven 4 builds for this PoC.
- Confirm the Windows processor architecture before downloading an x64 or ARM64 package.
- Where the vendor publishes a checksum or signature, verify it before installation. Keep the version, download URL, checksum result, and installation date as workstation evidence.
- Use default installation directories unless there is a documented reason to change them. Do not commit installation paths to the repository.
- Restart PowerShell after an installer changes `PATH` or `JAVA_HOME`.
- Install only one PostgreSQL server route: Docker Compose or native Windows PostgreSQL. Do not run both on port `5432`.
- IDEs are clients of the Maven project. Installing an IDE must not generate a new application or alter repository files.

### Recommended installation order on Windows

1. Check Windows Update, processor architecture, free disk space, memory, and whether virtualization is enabled in BIOS/UEFI and Task Manager.
2. Install Git for Windows.
3. Install a JDK 21 distribution.
4. Install Apache Maven 3.9.x.
5. Install or retain the chosen IDE.
6. If the Docker route is selected, prepare WSL 2 and install Docker Desktop.
7. Install native PostgreSQL only if the research decision rejects Docker or a separate local client is deliberately required.
8. Open a new PowerShell session and collect the verification evidence.

### Git for Windows — official installation

Official source: [Git for Windows](https://gitforwindows.org/) through the [Git Windows download page](https://git-scm.com/download/win).

1. Download the current stable 64-bit installer from the official Git page.
2. Run the signed Windows installer. Retain the normal command-line integration so `git` is available from PowerShell; Git Credential Manager may remain enabled for secure Git-host authentication.
3. Finish installation and open a new PowerShell session.
4. Verify:

	```powershell
	git --version
	where.exe git
	```

5. Configure the desired author name and email later using the identity intended for commits; do not invent or commit credentials.

### Eclipse Temurin JDK 21 — official installation

Provisional recommended JDK distribution: [Eclipse Temurin 21 LTS](https://adoptium.net/temurin/releases/?version=21), installed using the [official Windows MSI procedure](https://adoptium.net/installation/windows/). The vendor remains recorded in the workstation evidence and may be reconsidered at the technology gate.

1. Select Windows, the machine architecture, **JDK** rather than JRE, HotSpot, and Java 21 LTS.
2. Download the `.msi` and compare its published checksum when available.
3. Run the MSI and accept the license only after review.
4. On **Custom Setup**, keep **Add to PATH** enabled and enable **Update `JAVA_HOME`**. Installing the JDK under the default Eclipse Adoptium directory is acceptable.
5. Finish installation and open a new PowerShell session.
6. Verify that both the runtime and compiler are Java 21 and resolve to the intended installation:

	```powershell
	java -version
	javac -version
	$env:JAVA_HOME
	where.exe java
	where.exe javac
	```

If another Java installation appears first in `PATH`, correct the user/system environment-variable order rather than deleting another application's runtime without investigation.

### Apache Maven 3.9.x — official installation

Official sources: [Apache Maven downloads](https://maven.apache.org/download.cgi) and [installation instructions](https://maven.apache.org/install.html).

1. Install the JDK first and verify `JAVA_HOME`/`java`.
2. From the Apache page, download the latest stable Maven **3.9.x binary ZIP** and its `.sha512` file. Do not download the source archive, Maven 4 preview, or Maven Daemon for the baseline.
3. In PowerShell, calculate the archive hash and compare it with Apache's published SHA-512 value:

	```powershell
	Get-FileHash .\apache-maven-3.9.x-bin.zip -Algorithm SHA512
	```

4. Extract the archive to a stable, versioned directory such as `C:\Tools\apache-maven-3.9.x`. Do not place it inside this repository.
5. Add the extracted Maven `bin` directory to the user `PATH`. Maven's official procedure does not require `MAVEN_HOME`; avoid adding it unless a later tool specifically requires it.
6. Open a new PowerShell session and verify:

	```powershell
	mvn -version
	where.exe mvn
	```

The `mvn -version` output must show Java 21 from the intended JDK. If it reports another Java runtime, fix `JAVA_HOME` and `PATH` before continuing.

### IntelliJ IDEA — official installation

Official source: [JetBrains IntelliJ IDEA installation guide](https://www.jetbrains.com/help/idea/installation-guide.html). JetBrains recommends the [Toolbox App](https://www.jetbrains.com/toolbox/app/) for installation, updates, rollback, and managing versions.

1. Confirm the machine meets JetBrains' current Windows, RAM, disk, and display requirements.
2. Download the JetBrains Toolbox App from JetBrains, run its installer, and use it to install the current stable IntelliJ IDEA. Do not select an EAP or nightly build.
3. Alternatively, download the stable standalone `.exe` directly from [JetBrains](https://www.jetbrains.com/idea/download/) and run its wizard.
4. IntelliJ includes JetBrains Runtime for the IDE itself, but Java application development still uses the separately installed JDK 21.
5. Start IntelliJ and record the version from **Help → About**. Do not create a new project yet.
6. At the Phase 4 IDE migration gate, use **File → Open**, select this repository's `pom.xml`, and choose **Open as Project**, following JetBrains' [existing Maven project procedure](https://www.jetbrains.com/help/idea/maven-support.html#open_existing_maven_project).

IntelliJ's bundled Maven is convenient, but the command-line Maven installation remains the portable verification baseline until a Maven Wrapper is deliberately approved.

### Visual Studio Code — official installation or retention

If VS Code is retained, use Microsoft's [Windows installation guide](https://code.visualstudio.com/docs/setup/windows). There is no need to reinstall a working copy.

1. For a new installation, download the official **User Setup** unless all-user installation is required. User Setup needs no administrator access and normally provides the smoother update path.
2. Run the installer and retain the option that adds `code` to `PATH` if command-line launching is desired.
3. Because the JDK is installed separately, do not use the Java Coding Pack to install a duplicate JDK. If Java support is needed, install Microsoft's [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) from the VS Code Marketplace after reviewing its included extensions.
4. Optional Spring or Docker extensions are conveniences only and must not become build requirements.
5. Verify the editor from **Help → About** and, when command-line integration is enabled:

	```powershell
	code --version
	```

### WSL 2 and Docker Desktop — official installation

Official sources: [Microsoft WSL installation](https://learn.microsoft.com/windows/wsl/install) and [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/).

1. Read Docker's current Windows system requirements and subscription terms. For personal learning and this job-application PoC, confirm that the intended use qualifies under the current terms before accepting them.
2. Confirm hardware virtualization is enabled. Check whether a sufficiently recent WSL is already installed:

	```powershell
	wsl --version
	wsl --list --verbose
	```

3. If WSL is absent, follow Microsoft guidance: open PowerShell as Administrator, run the command below, and restart Windows when requested. This enables WSL and, by default, installs a Linux distribution:

	```powershell
	wsl --install
	```

	If WSL already exists, update it from an elevated PowerShell session:

	```powershell
	wsl --update
	```

4. Recheck `wsl --version`. Docker currently requires a supported WSL 2 release; use Docker's live requirements page rather than copying an old minimum into workstation notes.
5. Download `Docker Desktop Installer.exe` directly from Docker's official page.
6. Choose installation mode deliberately:
	- **Per-user** is Docker's recommendation for most users and uses the WSL 2 backend without administrator rights for normal installation/update.
	- **All-users** requires elevation and is needed only for requirements such as Windows containers or Hyper-V administration. This PoC needs Linux containers, not Windows containers.
7. Keep **Use WSL 2 instead of Hyper-V** selected for the planned Linux-container workflow unless research records a reason to use Hyper-V.
8. Start Docker Desktop manually, review and accept the subscription agreement, and wait until the engine is running.
9. Open a new PowerShell session and verify the client, engine, integrated Compose plugin, and a disposable test container:

	```powershell
	docker version
	docker compose version
	docker run --rm hello-world
	```

Do not start this project's Compose services until the design gate authorizes project execution. Do not add a user to `docker-users` unless the selected backend actually requires it; Docker documents that daemon access can be equivalent to administrative host privileges.

### PostgreSQL tools — choose one official route

Official source: [PostgreSQL Windows downloads](https://www.postgresql.org/download/windows/), which links to the EDB-certified Windows installer and an advanced binary archive.

**Preferred Docker route:** install no native PostgreSQL server. The approved PostgreSQL container will provide the server and `psql` inside the container. This avoids conflicting Windows services and ports.

**Native route, only if approved after research:**

1. Follow the PostgreSQL community Windows page to the EDB-certified installer for the approved PostgreSQL major version.
2. Run the signed installer and record the selected server, command-line tools, pgAdmin, data directory, service name, and port. StackBuilder is optional and no add-on is required for the initial PoC.
3. Use a unique local password and never place it in Git or screenshots.
4. Keep PostgreSQL listening on localhost unless the later runbook explicitly approves a narrowly scoped network rule. The HHT connects to Spring Boot, never directly to PostgreSQL.
5. Open a new PowerShell session; if the installer did not expose its `bin` directory, add that versioned `bin` directory to the user `PATH`, then verify:

	```powershell
	psql --version
	where.exe psql
	```

For a client-only setup, the official PostgreSQL page describes the EDB ZIP archive as an advanced-user option. Prefer container-executed `psql` unless there is a documented need for a native client.

### Official installation source register

Accessed 2026-07-11. Recheck each live page immediately before installation because versions and system requirements change.

| Tool | Official source | Installation decision recorded |
|---|---|---|
| Git | [Git SCM Windows install](https://git-scm.com/download/win), [Git for Windows](https://gitforwindows.org/) | Version, architecture, installer source |
| JDK 21 | [Eclipse Temurin releases](https://adoptium.net/temurin/releases/?version=21), [Windows MSI guide](https://adoptium.net/installation/windows/) | Vendor, full JDK build, architecture, checksum |
| Maven | [Apache download](https://maven.apache.org/download.cgi), [Apache install guide](https://maven.apache.org/install.html) | Stable 3.9.x version, SHA-512 result, extraction path |
| WSL 2 | [Microsoft Learn](https://learn.microsoft.com/windows/wsl/install) | Windows/WSL versions, backend readiness |
| Docker Desktop | [Docker Windows install](https://docs.docker.com/desktop/setup/install/windows-install/) | Version, installation mode, backend, terms review |
| IntelliJ IDEA | [JetBrains installation](https://www.jetbrains.com/help/idea/installation-guide.html), [Maven import](https://www.jetbrains.com/help/idea/maven-support.html#open_existing_maven_project) | Stable version, Toolbox/standalone method |
| VS Code | [Microsoft Windows setup](https://code.visualstudio.com/docs/setup/windows), [Java support](https://code.visualstudio.com/docs/languages/java) | Existing/new version and optional extension list |
| PostgreSQL | [PostgreSQL Windows downloads](https://www.postgresql.org/download/windows/) | Docker/native decision; native version only if selected |

### Evidence to capture after installation

- [ ] Operating-system edition and version.
- [ ] `git --version`.
- [ ] `java -version` and selected JDK vendor.
- [ ] `mvn -version`, including the Java runtime Maven uses.
- [ ] `docker --version` and `docker compose version`, if Docker is selected.
- [ ] IntelliJ IDEA edition/version or VS Code version.
- [ ] Confirmation that a disposable container can run, if Docker is selected.

Do not commit machine-specific paths, license information, personal settings, or secrets.

### Gate

Tool versions are recorded, but recording them does not approve the provisional project choices.

## Phase 2 — Research how experienced teams solve the problem

Research must use primary or authoritative sources where possible. Record the source date because framework and platform guidance changes.

### 2.1 Warehouse-domain research

Compare common WMS practices for:

- order release and wave/task creation;
- stock on hand, available stock, allocation, and reservation;
- deterministic multi-bin allocation;
- task claiming and concurrent handhelds;
- short picks, partial picks, blocked tasks, and supervisor recovery;
- scan sequence and wrong-location/article handling;
- idempotent HHT retries and temporary LAN failures;
- adjustments, receipts, picks, and movement-ledger reconciliation;
- order/line/task completion rules;
- traceability expected during support incidents;
- future material-flow-control integration boundaries.

The PoC should borrow sound invariants and support practices without reproducing enterprise features that do not demonstrate the target role.

### 2.2 Java and Spring research

Use official Spring, Maven, Java, and library documentation to compare:

- a conservative supported Spring Boot release versus the newest stable release;
- Maven versus Gradle for an enterprise-facing demonstration;
- Java-only implementation versus a deliberate Java/Kotlin mix;
- Spring Data JPA with targeted native locking queries versus JDBC or jOOQ;
- transaction boundaries and isolation requirements;
- validation and stable problem-response design;
- password hashing and opaque-token handling;
- structured logging, correlation IDs, and sensitive-data filtering;
- server-rendered HTML versus a separate frontend;
- QR PNG/PDF libraries and deterministic output.

### 2.3 PostgreSQL and migration research

Use official PostgreSQL, Flyway, and Testcontainers guidance for:

- `FOR UPDATE SKIP LOCKED` claim semantics and starvation/fairness implications;
- indexes supporting deterministic FIFO claims;
- stock-row locking and deadlock-safe lock order;
- schema constraints versus application validation;
- append-only audit enforcement and database permissions;
- common migrations versus development/test fixtures;
- migration immutability and repair policy;
- reconciliation queries and backup/restore expectations;
- Docker Compose versus native PostgreSQL on Windows.

### 2.4 Testing and operations research

Compare accepted practices for:

- unit, repository, API integration, concurrency, migration, and functional tests;
- numbered functional test cases and requirement traceability;
- executed-test evidence and defect references;
- configuration matrices and startup validation;
- structured application events that allow diagnosis without a debugger;
- Windows installation, firewall scoping, LAN validation, backup, rollback, and incident handling;
- GitHub Actions dependency pinning, caching, report retention, and quality gates.

### 2.5 Research outputs

Before implementation, produce:

- [x] a source register with links, access dates, and short findings;
- [x] a technology comparison matrix;
- [x] a WMS workflow/state diagram;
- [x] an entity and relationship outline;
- [x] stock, allocation, task, movement, and idempotency invariants;
- [x] proposed transaction and lock boundaries;
- [x] a configuration and secret-handling model;
- [x] a logging/event field catalogue;
- [x] an error-code catalogue;
- [x] a requirements-to-test traceability matrix;
- [x] a risk register;
- [x] architecture decision records for disputed choices. Evidence: `docs/decisions/0002` through `docs/decisions/0008`.

Research notes must distinguish **source finding**, **project interpretation**, **decision**, and **open question**.

Draft evidence for the completed output items is in `docs/research/phase-2-research.md`. Checked output items are produced for review, not approved design decisions.

## Phase 3 — Design checkpoint

Implementation requires explicit project-owner approval of all items below.

### Decisions to approve

- [x] Exact Java, Spring Boot, Maven, PostgreSQL, Flyway, and Testcontainers versions. Evidence: ADR 0002.
- [x] Docker Compose or native PostgreSQL for local development. Evidence: ADR 0002.
- [x] Java only or Java plus Kotlin, with a reason tied to the portfolio goal. Evidence: ADR 0002.
- [x] Persistence approach for ordinary CRUD and locking-heavy operations. Evidence: ADR 0003.
- [x] Stock availability/allocation formula and reservation timing. Evidence: ADR 0003.
- [x] Administrative recovery behavior for blocked tasks. Evidence: ADR 0004.
- [x] Authentication and token lifecycle appropriate to a LAN PoC. Evidence: ADR 0005.
- [x] Entity model, state machines, constraints, and transaction boundaries. Evidence: ADRs 0003 and 0004.
- [x] HHT API outline and idempotency semantics. Evidence: ADR 0005 and `API.md`.
- [x] Logging, configuration, testing, evidence, and runbook standards. Evidence: ADR 0006.
- [x] Explicitly excluded features. Evidence: ADR 0008.

### Gate

Only an explicit instruction such as “approve the design and begin implementation” opens Phase 4.

The owner recorded the D-01 through D-17 approvals on 2026-07-12 and 2026-07-13
(`docs/research/phase-3-validation-log.md`, `docs/research/phase-3-decision-packet.md`)
and subsequently instructed implementation to proceed on the approved baseline.

## Phase 4 — IDE strategy and possible IntelliJ IDEA migration

The Maven project and Git repository are the sources of truth. Moving from VS Code to IntelliJ IDEA is an IDE/workflow migration, not an application rewrite.

### 4.1 Prepare for portability

- [ ] Keep all build, test, quality, and run commands in Maven rather than IDE-only actions.
- [ ] Keep configuration externalized; do not store real credentials in run configurations.
- [ ] Preserve UTF-8, consistent line endings, and the repository formatting rules.
- [ ] Keep `.idea/`, local run configurations, workspace metadata, and machine-specific SDK paths out of Git unless a later review identifies a safe shared file.
- [ ] Do not make VS Code extensions or IntelliJ plugins runtime prerequisites.

### 4.2 Import into IntelliJ IDEA

After IntelliJ is installed by the project owner:

1. Open the repository by selecting `pom.xml` or the repository root.
2. Import it as a Maven project; do not create a second project around it.
3. Select the installed JDK 21 as both Project SDK and Maven runner JDK.
4. Set language level from the Maven model rather than overriding it independently.
5. Allow Maven to resolve dependencies and compare the imported dependency tree with command-line Maven.
6. Configure tests to run through Maven when validating CI-equivalent behavior.
7. Create local run configurations for `dev` and `preprod` only after the configuration model is approved.
8. Supply environment variables locally; never commit preproduction secrets.
9. Treat IntelliJ database and Docker views as optional conveniences, not required deployment mechanisms.
10. Confirm Git, terminal, test discovery, Spring configuration recognition, and debugger attachment.

### 4.3 Verify migration

- [ ] A clean command-line Maven build and the IntelliJ Maven build produce equivalent results.
- [ ] Unit and integration tests are discovered with the same JDK.
- [ ] Spring profiles and environment variables behave identically.
- [ ] No accidental `.idea`, local database, secret, or run-configuration files are staged.
- [ ] The application can still be developed from VS Code or another editor using only repository documentation.

### 4.4 Kotlin decision, if desired

Kotlin is not introduced merely because IntelliJ supports it well. If research justifies it:

- record an ADR describing where Kotlin adds portfolio value;
- keep the public domain language and package boundaries coherent;
- add the Kotlin Maven plugin and Spring/JPA compiler plugins deliberately;
- define Java/Kotlin nullability and interoperation rules;
- verify mixed-language compilation, static analysis, tests, and CI;
- avoid rewriting working Java solely for stylistic preference.

## Phase 5 — Audit and rebaseline the provisional repository

After the design and IDE/toolchain decisions are approved:

- [x] Review every existing file against the research outputs. Evidence: `docs/research/provisional-artifact-inventory.md` and the 2026-07-13 rebaseline changes.
- [x] Revalidate all provisional version selections and dependencies. Evidence: `docs/research/phase-3-validation-log.md` §5; `pom.xml` now applies Boot 4.0.7 and the pgJDBC 42.7.13 security override.
- [x] Review schema normalization, constraints, lock order, indexes, triggers, and fixture isolation. Evidence: rebaselined `V1__create_schema.sql` (approved states, `task_transition` ledger, scan-timestamp constraints) validated by `FlywayMigrationIT`.
- [x] Review API state, error, authentication, concurrency, and idempotency semantics. Evidence: `API.md` aligned with ADRs 0004/0005 (RFC 9457 problems, replay semantics, block/resume recovery).
- [ ] Review CI action versions, permissions, quality gates, and evidence retention.
- [x] Correct documentation that claims unverified or obsolete behavior. Evidence: README, `API.md`, and `docs/sql-diagnostics.md` status rebaseline of 2026-07-13.
- [x] Decide whether the unshared initial migration may be replaced before it becomes the real baseline; after application, migrations are immutable. Evidence: D-14 approved; provisional V1 replaced by the approved baseline before any application.
- [ ] Publish the approved implementation backlog.

## Phase 6 — Database foundation and SQL diagnostics

### Deliverables

- approved Maven/Spring project structure;
- reproducible PostgreSQL development service;
- Flyway-owned common schema;
- development-only deterministic fixtures and known demo credentials;
- constraints enforcing the approved invariants;
- append-only movement protection;
- PostgreSQL migration/integrity tests;
- diagnostic SQL for stuck tasks, stock-versus-movement discrepancies, and end-to-end order tracing.

### Acceptance gate

- [x] A clean PostgreSQL database migrates without manual SQL. Evidence: `docs/evidence/2026-07-13-phase6-maven-verify.md` (2 migrations applied to an empty digest-pinned PostgreSQL 17.10 container).
- [ ] Preproduction receives no demo credentials or fixtures. Configuration separates `db/devdata`, but a preprod-profile test has not been executed.
- [x] Seed stock reconciles to movements. Evidence: `FlywayMigrationIT.seededStockMatchesTheAppendOnlyMovementLedger`, same evidence record.
- [x] Invalid relationships and movement mutations fail. Evidence: `FlywayMigrationIT` mutation/constraint tests for `stock_movement`, `task_transition`, and task/movement relationships.
- [ ] Diagnostics return documented, reproducible results. The query pack is aligned to the approved schema; executing it against a running development database and recording results remains open.

## Phase 7 — HHT and administration REST API

### Deliverables

- login/logout with an approved simple-token model;
- next-task retrieval and atomic claim;
- location scan, article scan, and exact-quantity confirmation;
- retry-safe confirmation;
- stable errors for wrong scans, invalid state, assignment conflict, insufficient stock, quantity mismatch, and authentication failures;
- administration endpoints for orders, task visibility, master data, stock adjustments, and approved recovery operations.

### Acceptance gate

- [x] Concurrent requests cannot claim the same task. Evidence: `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` (FT-04 Part A).
- [x] A user/device cannot hold multiple active tasks. Evidence: `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` (FT-04 Part B).
- [x] Wrong scans and rejected quantities make no stock change. Evidence: `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md` (FT-05, FT-07).
- [x] One successful confirmation creates exactly one movement. Evidence: `docs/evidence/2026-07-13-phase7-step3-picking-happy-path.md` (FT-08).
- [x] Retrying a confirmation cannot decrement stock twice. Evidence: `docs/evidence/2026-07-13-phase7-step3-picking-happy-path.md` (FT-08 replay) and `…-step4-picking-negative-path.md` (FT-12, different-payload reuse).
- [x] Orders complete only when all approved task requirements are satisfied. Evidence: `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md` (FT-10).

### Execution plan (approved build order)

Build as thin vertical slices, each ending with green Failsafe/Surefire tests
and retained evidence, so acceptance accumulates continuously. Java code today
is only `WarehouseManagementApplication` and `WmsProperties`; everything below
is new. FT references are cases in `docs/functional-test-specification.md`.

0. **Prerequisite — regenerate demo credentials for Argon2id** — DONE
   2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step0-argon2id.md`
   (revises the Phase 6 fixture; see ADR 0005 "Implementation refinement 2026-07-13").
   - [x] Add the app `PasswordEncoder` bean as `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`. Added `configuration/PasswordEncoderConfiguration`; `bcprov-jdk18on:1.85` pinned in `pom.xml` because Boot 4 does not manage BouncyCastle.
   - [x] Precompute Argon2id PHC hashes for `admin123` and `picker123` once, then paste the two literal strings into `db/devdata/V1_1__seed_demo_data.sql`, replacing the `crypt('…', gen_salt('bf', 10))` calls. Generated with a throwaway test (since deleted); ADR 0005 parameters confirmed by the PHC header `m=16384,t=2,p=1`.
   - [x] Widen `app_user.password_hash` to `VARCHAR(255)`. **Resolved: folded into V1** (plan's recommended option) — no retained/shared database exists (D-14); V1 has only run against disposable Testcontainers, so V1 was edited before any durable dev database exists.
   - [x] Replace the `crypt('picker123', password_hash)` assertion in `FlywayMigrationIT` with a format check; real login verification moves to the Java-level FT-01 test. Now asserts both seeded users store `$argon2id$`-prefixed hashes.
   - [x] `pgcrypto` — **Resolved: dropped.** After removing the `crypt()`/`gen_salt()` fixtures it is unused (`gen_random_uuid()` is core in PG17); the clean migration in the evidence run confirms nothing else depends on it.
1. **Persistence layer.** JPA entities + Spring Data repositories mapping the existing schema, Hibernate in `validate`-only mode (never create/update). Add `JdbcTemplate`/named-parameter scaffolding for the locking, allocation, FIFO-claim, and reconciliation queries per ADR 0003. No behavior yet. — DONE 2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step1-persistence.md`. 11 entities + repositories across the `identity`/`catalog`/`inventory`/`orders`/`picking` modules; foreign keys mapped as scalar identifiers (overlapping composite FKs, locking paths use JDBC); `StockJdbcRepository` implements the availability and reconciliation reads while the mutating `FOR UPDATE … SKIP LOCKED` claim and allocation-lock SQL are deferred to Steps 3/5. `PersistenceLayerIT` proves Hibernate `validate` accepts every mapping and the repositories/JDBC reads return the seed fixtures.
2. **Auth slice** (FT-01, FT-03, FT-14). Login/logout, opaque token generation (≥256 bits, store SHA-256 hash only, bound to user/device, 8 h absolute expiry), the bearer-token filter, and the global RFC 9457 `application/problem+json` handler with the stable code catalogue. Unblocks every later endpoint. — DONE 2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step2-auth.md`. `POST /api/v1/auth/login` + `/logout`, stateless bearer security chain, `ProblemCode` catalogue, correlation-id filter. Auth orchestration placed in a new `auth` package to avoid an `identity ↔ picking` cycle; token-time inactive user/device fail closed as `INVALID_TOKEN`. `AuthApiIT` proves FT-01/03/14 (11 cases) end to end. Boot 4 notes: Jackson 3 (`tools.jackson`) is the default mapper; `TestRestTemplate` is gone (test uses `RestTemplate` + JDK `HttpClient` factory).
3. **Picking happy path** (FT-06, FT-08). `GET /hht/tasks/next` (atomic claim via `FOR UPDATE OF task SKIP LOCKED`, full FIFO order), scan-location, scan-article, and the confirm transaction that locks task then stock and atomically updates task, line, order, stock, plus one `stock_movement` and one `task_transition`. — DONE 2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step3-picking-happy-path.md`. Lock order extended to task → stock → order line → customer order (the ADR's explicit task/stock pair plus line/order locks this slice added, needed because multi-bin lines can be confirmed concurrently by different pickers). `PickingApiIT` proves FT-06 (scan progression + replay-safety) and FT-08 (exact-quantity confirm + idempotent retry) end to end. A flush-ordering bug (stock update not flushed before the movement insert, tripping the append-only trigger) was found and fixed during verification — see evidence "Deviations." Negative/concurrency/idempotency-reuse test evidence (FT-04/05/07/12) is deferred to Step 4, though the underlying checks are already implemented.
4. **Picking negative / concurrency / idempotency** (FT-04, FT-05, FT-07, FT-12). Wrong location/article, quantity mismatch (exact-quantity rule), two users racing one task, one-active-task-per-user/device, and confirmation-UUID reuse (`409 CONFIRMATION_ID_REUSED`). This is the core testing-competence evidence. — DONE 2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step4-picking-negative-path.md`. No production code changed (Step 3 already implemented every checked path); this step is the dedicated `PickingNegativePathApiIT` proof, in its own Testcontainers-backed database to avoid ordering fragility with `PickingApiIT`. FT-05/07/12 proved as one continuous operator journey; FT-04 uses a backdated synthetic task pool (a first attempt using leftover seed tasks was non-deterministic since FIFO claimed the older seed rows first — recorded as a failed experiment) and observes the specific `{200, 409}` status pair a `SKIP LOCKED` + unique-index race produces, confirmed by the expected `23505` warning in the server log.
5. **Admin endpoints** (FT-02, FT-09, FT-10, FT-13). Order creation with atomic multi-bin allocation (availability = `stock.quantity − Σ unfinished task quantity`, ascending `(article_id, location_id)` lock order), block/resume recovery with audited reason, order/task reads, and stock adjustments. — DONE 2026-07-13; evidence `docs/evidence/2026-07-13-phase7-step5-admin-endpoints.md`. New `admin` package (orders/allocation, task listing, catalog, stock adjustment); block/resume delegate to the existing `PickingService`. A real allocation bug was found and fixed: the availability walk didn't account for an earlier line's in-flight (not-yet-persisted) draw within the same order-creation request, so two lines of one order could double-claim the same bin — fixed with an in-memory per-bin consumption tracker layered on top of the DB reservation snapshot. `OrderAllocationApiIT`/`TaskRecoveryApiIT`/`OrderLifecycleApiIT` (each its own database) prove FT-02/09/10; `PersistenceLayerIT` gains the FT-13 injected-discrepancy case alongside Step 1's already-evidenced clean-fixture case.

Each slice: write the Failsafe integration test(s) for its FT cases against the
digest-pinned PostgreSQL Testcontainer, keep Checkstyle/SpotBugs green, and
record evidence under a build/configuration ID in `docs/evidence/`.

## Phase 8 — Live dashboard and QR labels

### Deliverables

- minimal live task-state dashboard using polling;
- diagnostic identifiers and stuck-state visibility;
- deterministic location/article QR payloads;
- printable PNG and A4 PDF labels with human-readable text.

### Technology selection (researched 2026-07-13, sources accessed 2026-07-13)

ADR 0007 fixed the contracts (payloads `LOC:<code>`/`ART:<sku>`, 300×300 PNG,
error correction M, four-module quiet zone, deterministic A4 PDF with an
embedded licence-reviewed font, authenticated admin-only server-rendered
dashboard with configurable ~2 s polling, no separate frontend build) and left
one consequence open: *record the exact QR/PDF library, font, and licence
evidence before implementation*. Research findings closing that item:

- **QR encoding — ZXing `com.google.zxing:core` + `javase` 3.5.3**
  (Apache-2.0). The project is in maintenance mode (contributed bug fixes
  only, no roadmap: <https://github.com/zxing/zxing>; latest core 3.5.3:
  <https://mvnrepository.com/artifact/com.google.zxing/core>).
  *Interpretation:* acceptable here — the encoder is mature, the QR spec is
  frozen, and Phase 2 already compared QRGen and other barcode libraries
  (`docs/research/phase-2-research.md`, JAVA-10); a maintained-only encoder is
  a lower risk than a wrapper adding a second dependency layer.
- **PDF — Apache PDFBox 3.0.8** (Apache-2.0), current 3.x release line
  (<https://pdfbox.apache.org/download.html>). Embed the font subset with
  `PDType0Font`; confirms the Phase 2 selection over iText (AGPL) and OpenPDF.
- **Embedded font — Liberation Sans ≥ 2.00, SIL OFL 1.1** (metrically
  Arial-compatible; licence permits bundling/embedding/redistribution:
  <https://github.com/liberationfonts/liberation-fonts/blob/main/LICENSE>).
  Vendor the TTF **plus its licence text** under `src/main/resources/fonts/`.
- **Dashboard — `spring-boot-starter-thymeleaf`** (Boot-managed version per
  ADR 0002 pinning discipline) with a plain-JavaScript `fetch` + `setInterval`
  poll of a small JSON endpoint. Thymeleaf remains a first-class Boot starter
  (<https://spring.io/guides/gs/serving-web-content/>). *Interpretation:* this
  reaffirms the Phase 2 comparison (Thymeleaf + polling over SPA and
  htmx/SSE); vanilla `fetch` needs no vendored JS dependency and no build
  toolchain, so it is the smallest implementation of "refreshes without a full
  reload".
- **Determinism technique.** PNG: ZXing produces a `BitMatrix` from fixed
  inputs; `ImageIO` PNG encoding adds no timestamps, so bytes are stable
  (assert byte-equality in the FT-17 test rather than assuming it). PDF: the
  two nondeterministic inputs are the info-dictionary dates and the trailer
  document ID (same fields the reproducible-builds ecosystem normalizes via
  `SOURCE_DATE_EPOCH`: <https://www.tug.org/pipermail/pdftex/2015-July/008955.html>);
  fix `PDDocumentInformation` creation/modification dates to a constant and
  seed the trailer ID via `COSDocument.setDocumentId`, then assert
  byte-equality across repeated generation.

### Execution plan

Owner authorized implementation 2026-07-13 ("start the implementation").

- [x] **Step 0 — Record the selection.** Amended ADR 0007 with the exact
  artifacts actually pinned — ZXing `core`/`javase` **3.5.4** (the research
  note's 3.5.3 was already superseded on Maven Central by implementation
  time; corrected, not left stale), PDFBox 3.0.8, Liberation Sans + OFL 1.1 —
  and vendored the font plus its licence text under
  `src/main/resources/fonts/liberation-sans/`. Discharges the ADR 0007
  "before implementation" consequence. — DONE 2026-07-14; evidence
  `docs/evidence/2026-07-14-phase8-steps0-2.md`.
- [x] **Step 1 — Label slice (FT-17).** `LabelService` (new `label` package)
  reads the existing `location.qrValue`/`article.qrValue` columns (no payload
  re-derivation) and renders a deterministic 300×300 PNG and a single-label
  A4 PDF (fixed geometry, human-readable text in embedded Liberation Sans,
  fixed `PDDocumentInformation` dates and a seeded trailer document ID, both
  confirmed deterministic by inspecting the actual PDFBox 3.0.8 `COSWriter`
  source, not assumed). Admin-only endpoints under
  `GET /api/v1/admin/labels/{locations,articles}/{code,sku}/{png,pdf}`.
  `LabelApiIT` proves: repeated generation is byte-identical (PNG and PDF,
  both entity types); the PNG decodes (ZXing reader) to the exact
  `LOC:<code>`/`ART:<sku>` payload; that decoded value is accepted by the
  real `POST /hht/tasks/{id}/scan-location`/`scan-article` endpoints for a
  freshly claimed task; a `PICKER` gets `403`; an unknown code/SKU gets the
  existing `404` problem. — DONE 2026-07-14; evidence
  `docs/evidence/2026-07-14-phase8-steps0-2.md`.
- [x] **Step 2 — Dashboard slice (FT-18).** New `@Order(1)`
  `DashboardSecurityConfiguration` with `securityMatcher("/dashboard/**",
  "/login", "/default-ui.css")`: session-based form login (new
  `DashboardUserDetailsService` backed by the existing `AppUserRepository`
  and Argon2 `PasswordEncoder`), `ADMIN` role only, CSRF enabled; the
  existing bearer chain in `SecurityConfiguration` is now `@Order(2)` and
  otherwise untouched. One Thymeleaf page (`dashboard.html`: task states,
  diagnostic identifiers — order, line, task, location, article, user,
  device — and a highlighted stuck flag) plus a JSON poll endpoint
  (`GET /dashboard/api/tasks`), both reusing the Phase 7
  `AdminTaskQueryService`. Poll interval is `wms.dashboard.poll-interval`
  (default `PT2S`, env override `WMS_DASHBOARD_POLL_INTERVAL`) for the
  Phase 9 configuration matrix. `DashboardApiIT` proves unauthenticated/
  wrong-credential/non-admin access all fail and an admin session sees live
  state via both the page and the poll endpoint. A real cross-chain bug
  (Spring's default `sendError(403)` re-entering the security filter chain
  via the servlet container's `/error` forward and landing in the *other*
  chain as a `401`) was found and fixed with a direct-status
  `accessDeniedHandler`; the same class of issue for `/default-ui.css` was
  caught only by a manual real-browser pass (Playwright, ad hoc, not a
  project dependency) and fixed the same way — screenshots and a sample
  label retained at `docs/evidence/2026-07-14-phase8-steps0-2/`. — DONE
  2026-07-14; evidence `docs/evidence/2026-07-14-phase8-steps0-2.md`.
- [x] **Step 3 — Evidence and gate.** `mvn verify` is green (29 tests, 0
  Checkstyle violations, 0 SpotBugs findings); acceptance-gate boxes below
  are checked with citations. — DONE 2026-07-14; evidence
  `docs/evidence/2026-07-14-phase8-steps0-2.md`.

### Acceptance gate

- [x] Dashboard state refreshes without a full reload — `DashboardApiIT`
  (session-authenticated poll endpoint reflects live state independent of
  the page load) plus manual browser observation (three `GET
  /dashboard/api/tasks` fetches at the configured 2 s cadence, page URL
  unchanged throughout). Evidence: `docs/evidence/2026-07-14-phase8-steps0-2.md`.
- [x] Generated labels scan to values accepted by the HHT API —
  `LabelApiIT` decodes the generated PNG and submits it to the real HHT scan
  endpoints, which accept it. Evidence:
  `docs/evidence/2026-07-14-phase8-steps0-2.md`.
- [x] Repeated generation is deterministic — `LabelApiIT` asserts
  byte-equality across two independent PNG/PDF generations for both
  locations and articles. Evidence:
  `docs/evidence/2026-07-14-phase8-steps0-2.md`.

## Phase 9 — Configuration, logging, CI, tests, and runbooks

### Deliverables

- separate `dev` and `preprod` profiles;
- startup validation and a configuration matrix containing owner, default, sensitivity, environment, and restart requirement;
- structured operational logging with correlation, order, task, user/device, article, location, outcome, error, and duration fields where relevant;
- Maven and GitHub Actions quality gates;
- numbered functional test specification;
- executed-test report template;
- Windows installation and rollback runbook, including scoped API-port firewall rules and LAN/HHT checks;
- log-analysis guide;
- incident record template.

### Baseline assessment (2026-07-14)

Several deliverables are already substantially in place from Phases 5–7 and
do not need to be rebuilt, only completed and evidenced:

- `dev`/`preprod` profiles (`application-dev.yml`, `application-preprod.yml`)
  and the numbered functional test specification
  (`docs/functional-test-specification.md`, FT-01–FT-19) already exist.
- `CorrelationIdFilter` already assigns/echoes `X-Correlation-Id` and
  publishes it to SLF4J MDC; `logstash-logback-encoder` already renders
  structured JSON console output (Phase 7 Step 0). Coverage of business
  fields (order/task/user/device/article/location/outcome/duration) is
  partial — only auth (`AuthenticationService`) and one picking conflict path
  log today; the confirm/block/resume/adjustment/order-creation paths do not.
- `.github/workflows/ci.yml` already runs `mvn verify` (which already
  executes Checkstyle, SpotBugs, and every Testcontainers IT) on
  `actions/setup-java` with Maven caching; `ubuntu-latest` ships Docker
  preinstalled, so Testcontainers needs no extra CI setup
  (<https://www.docker.com/blog/running-testcontainers-tests-using-github-actions/>,
  accessed 2026-07-14). This deliverable needs review, not a rebuild.
- Not yet started: startup validation with a *safe* diagnostic (FT-15),
  completing structured logging coverage (FT-16), the configuration matrix,
  the runbook, the log-analysis guide, the incident-record template, and the
  executed-test report.

### Execution plan

Owner authorized implementation 2026-07-14 ("continue the implementation",
following the completed and evidenced Phase 8).

0. **Configuration matrix.** `docs/configuration-matrix.md` cataloguing every
   `wms.*`/`WMS_*`/`spring.*` property introduced through Phase 8: owner,
   default, sensitivity, environment (dev/preprod/both), and restart
   requirement.
- [x] **Step 1 — Startup validation (FT-15).** `PreprodConfigurationValidator`
  (`EnvironmentPostProcessor`, registered in `META-INF/spring.factories` —
  confirmed against the Boot 4.0.7 sources that this extension point predates
  the `*.imports` convention) runs before any bean/datasource is created,
  active only under `preprod`: throws a clean `PreprodConfigurationException`
  when `WMS_DB_URL`/`USERNAME`/`PASSWORD` is missing/blank or when the
  password equals the committed dev default; a paired
  `PreprodConfigurationFailureAnalyzer` renders Boot's own boxed
  `APPLICATION FAILED TO START` description/action report instead of a raw
  stack trace. `PreprodConfigurationValidatorIT` boots the real
  `SpringApplication` with captured output and proves both the missing- and
  unsafe-value paths fail fast with the clean message and no secret printed.
  — DONE 2026-07-14; evidence `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`.
- [x] **Step 2 — Structured logging completion (FT-16).** Added one
  structured `INFO` log (SLF4J 2.x `log.atInfo().addKeyValue(...)` fluent
  API) to the picking confirm transaction, block, resume, stock adjustment,
  and order creation, plus one centralized `WARN` log in
  `GlobalExceptionHandler.handleProblem` covering **every**
  `ProblemException` app-wide (closing the "wrong-scan... diagnosed from
  logs" acceptance-gate item for every business-rule code at once, not just
  the ones this step touched). `StructuredLoggingApiIT` proves a real stock
  adjustment produces genuine separate JSON fields and a rejected adjustment
  produces a `business rule violation` entry, with no secret/token leaked.
  A real gap was found and fixed along the way: this app's structured JSON
  logging has always come from Spring Boot's own native
  `logging.structured.format.console: logstash`, not the pinned
  `logstash-logback-encoder` dependency (which was never wired to a
  `logback-spring.xml` and produced no separate fields when used) — removed
  the unused dependency and switched every call site to the SLF4J fluent API
  Boot's native formatter actually reads. — DONE 2026-07-14; evidence
  `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`.
- [x] **Step 3 — CI/quality gate review.** `ci.yml` reviewed: it already runs
  `mvn verify` (Checkstyle, SpotBugs, every Testcontainers IT) with Maven
  caching; `ubuntu-latest` ships Docker preinstalled, so Testcontainers needs
  no extra runner setup. No gap found; no change made; no push/trigger
  performed (visible-to-others action, left for the owner). — DONE
  2026-07-14; evidence `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`.
- [x] **Step 4 — Windows installation and rollback runbook.**
  `docs/runbook.md`: clean-environment install, the scoped API-port firewall
  rule, a LAN/HHT reachability check from a second machine, and rollback —
  explicitly distinguishing routine application rollback from the
  by-design-unsupported schema rollback (Flyway migrations are immutable;
  fix forward only). — DONE 2026-07-14.
- [x] **Step 5 — Log-analysis guide.** `docs/log-analysis-guide.md`: a field
  reference for every structured log event above, PowerShell/`jq` worked
  examples, and the FT-15/wrong-scan diagnosis walkthroughs, paired with the
  existing `docs/sql-diagnostics.md`. — DONE 2026-07-14.
- [x] **Step 6 — Incident record template.** `docs/incident-record-template.md`.
  — DONE 2026-07-14.
- [x] **Step 7 — Executed-test report.** `docs/executed-test-report.md`
  aggregates FT-01–FT-19: 18 Passed citing their evidence files, FT-19
  Blocked by design pending Phase 10. — DONE 2026-07-14.
- [x] **Step 8 — Evidence and gate.** `mvn verify` green (32 tests, 0
  Checkstyle violations, 0 SpotBugs findings); acceptance-gate boxes below
  checked with citations; `README.md` refreshed. — DONE 2026-07-14;
  evidence `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`.

### Acceptance gate

- [x] A clean Windows environment can be configured from the runbook —
  `docs/runbook.md` Sections 1–4. Full end-to-end rehearsal on a genuinely
  clean machine is a recorded residual item (evidence, "Residual risk"), not
  yet performed in this session.
- [x] Every requirement maps to one or more numbered tests —
  `docs/requirements-traceability.md` (R-01–R-20 → FT-01–FT-19), unchanged
  from Phase 3 design and confirmed still accurate.
- [x] Wrong-scan and stock-integrity incidents can be diagnosed from logs and
  SQL without a debugger — `GlobalExceptionHandler`'s centralized violation
  log (Step 2) plus `docs/log-analysis-guide.md`'s worked diagnosis
  walkthrough, cross-referenced with `docs/sql-diagnostics.md`.
- [x] Evidence is retained under a build and configuration identifier —
  `docs/evidence/2026-07-14-phase9-config-logging-ci-docs.md`.

## Phase 10 — MFC extension seam only

- [x] Define an application port such as `OrderCompletionPublisher` —
  `orders.OrderCompletionPublisher`.
- [x] Define an immutable order-completion message and idempotency
  identifier — `orders.OrderCompletionEvent` (`eventId` — also the
  idempotency identifier — `orderId`, `orderNumber`, `completedAt`, per
  ADR 0007).
- [x] Provide a configuration-selected no-operation adapter —
  `mfc.NoopOrderCompletionPublisher`, selected by `wms.mfc.adapter=noop`
  (`@ConditionalOnProperty`, `matchIfMissing = true`).
- [x] Document future TCP serialization, timeout, result, retry ownership,
  and observability boundaries — `docs/architecture.md`, "MFC extension
  seam" section.
- [x] Do not create sockets, schedulers, retry loops, or real telegram
  delivery — confirmed absent by the FT-19 code-inspection review below.

DONE 2026-07-14; evidence `docs/evidence/2026-07-14-phase10-mfc-seam.md`.

### Acceptance gate

- [x] A test fake observes one completion publication without TCP or
  telegram classes leaking into order-domain code —
  `orders.FakeOrderCompletionPublisher` + `OrderCompletionSeamApiIT`
  (a full Testcontainers-backed claim/scan/confirm flow asserts exactly one
  publication with the correct fields); confirmed by code inspection that
  `OrderCompletionPublisher`/`OrderCompletionEvent` import nothing beyond
  `java.time`/`java.util`, and no socket/telegram class exists anywhere in
  the codebase. Evidence: `docs/evidence/2026-07-14-phase10-mfc-seam.md`.

## Final acceptance

- [x] The approved automated verification suite passes on pinned tool
  versions — `mvn -B verify`: 33 tests, 0 Checkstyle violations, 0 SpotBugs
  findings, `BUILD SUCCESS` (`docs/evidence/2026-07-14-phase10-mfc-seam.md`).
- [x] The numbered functional test suite is executed and reported —
  `docs/executed-test-report.md`: FT-01–FT-19, 19 Passed, 0 Failed, 0
  Blocked, 0 Not Applicable.
- [x] SQL, logs, reports, screenshots, and label evidence are retained —
  `docs/evidence/` (SQL: `docs/sql-diagnostics.md` query results cited
  throughout; logs: Phase 9 evidence; screenshots/labels: Phase 8 evidence
  subfolder `docs/evidence/2026-07-14-phase8-steps0-2/`).
- [ ] A fresh-machine or clean-environment runbook rehearsal succeeds —
  **not yet performed**; this workstation already has the full toolchain
  installed, so `docs/runbook.md` has not been rehearsed on a genuinely
  clean machine in this session (recorded as a residual item in the Phase 9
  and Phase 10 evidence). Remains open pending an actual clean-environment
  rehearsal.
- [x] The portfolio explicitly demonstrates SQL, Java, configuration,
  functional testing, log analysis, and operational documentation — SQL
  (`docs/sql-diagnostics.md`), Java/Spring (the full `/api/v1` + dashboard +
  label + MFC-seam implementation), configuration
  (`docs/configuration-matrix.md`, FT-15), functional testing
  (`docs/functional-test-specification.md`, `docs/executed-test-report.md`),
  log analysis (`docs/log-analysis-guide.md`, FT-16), operational
  documentation (`docs/runbook.md`, `docs/incident-record-template.md`).

## Progress tracking rules

- Update this file only when evidence supports a status change.
- Do not mark a phase complete because files exist or compilation succeeds.
- Record decisions in ADRs rather than silently changing assumptions.
- Record deviations, failed experiments, and unresolved risks.
- Stop at every gate and obtain explicit approval before entering the next implementation stage.
