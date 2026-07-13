# Miniature WMS research and delivery plan

## Plan status

**Current stage:** implementation — Phase 5 rebaseline and Phase 6 database foundation  
**Implementation status:** authorized by the project owner after the Phase 3 design approvals (ADRs 0002–0008); Phases 7–10 remain gated  
**Tool installation owner:** project owner; assistants must not install or configure system tools unless explicitly asked later

The pre-research scaffold was reviewed in the Phase 5 rebaseline. The Maven build, schema baseline, development fixtures, and migration tests now reflect the approved design (ADRs 0002–0008). Artifacts belonging to later phases — REST controllers, services, dashboard, labels, CI hardening, and runbooks — remain **provisional drafts** until their phase is implemented and evidenced.

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

- [ ] Concurrent requests cannot claim the same task.
- [ ] A user/device cannot hold multiple active tasks.
- [ ] Wrong scans and rejected quantities make no stock change.
- [ ] One successful confirmation creates exactly one movement.
- [ ] Retrying a confirmation cannot decrement stock twice.
- [ ] Orders complete only when all approved task requirements are satisfied.

## Phase 8 — Live dashboard and QR labels

### Deliverables

- minimal live task-state dashboard using polling;
- diagnostic identifiers and stuck-state visibility;
- deterministic location/article QR payloads;
- printable PNG and A4 PDF labels with human-readable text.

### Acceptance gate

- [ ] Dashboard state refreshes without a full reload.
- [ ] Generated labels scan to values accepted by the HHT API.
- [ ] Repeated generation is deterministic.

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

### Acceptance gate

- [ ] A clean Windows environment can be configured from the runbook.
- [ ] Every requirement maps to one or more numbered tests.
- [ ] Wrong-scan and stock-integrity incidents can be diagnosed from logs and SQL without a debugger.
- [ ] Evidence is retained under a build and configuration identifier.

## Phase 10 — MFC extension seam only

- Define an application port such as `OrderCompletionPublisher`.
- Define an immutable order-completion message and idempotency identifier.
- Provide a configuration-selected no-operation adapter.
- Document future TCP serialization, timeout, result, retry ownership, and observability boundaries.
- Do not create sockets, schedulers, retry loops, or real telegram delivery.

### Acceptance gate

A test fake observes one completion publication without TCP or telegram classes leaking into order-domain code.

## Final acceptance

- [ ] The approved automated verification suite passes on pinned tool versions.
- [ ] The numbered functional test suite is executed and reported.
- [ ] SQL, logs, reports, screenshots, and label evidence are retained.
- [ ] A fresh-machine or clean-environment runbook rehearsal succeeds.
- [ ] The portfolio explicitly demonstrates SQL, Java, configuration, functional testing, log analysis, and operational documentation.

## Progress tracking rules

- Update this file only when evidence supports a status change.
- Do not mark a phase complete because files exist or compilation succeeds.
- Record decisions in ADRs rather than silently changing assumptions.
- Record deviations, failed experiments, and unresolved risks.
- Stop at every gate and obtain explicit approval before entering the next implementation stage.
