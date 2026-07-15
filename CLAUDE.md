# Miniature WMS project guidance

This file is the single canonical source of repository-wide working rules for
every assistant and editor used on this project — Claude, GitHub Copilot,
IntelliJ IDEA, VS Code, or any other tool. If you edit a rule, edit it here
only.

## Source of truth

1. `README.md` is the live record of project status, delivered scope, and
   the documentation index. `docs/executed-test-report.md` and
   `docs/evidence/` are the record of what has actually been verified —
   a compiling file, a passing build, or prior generated output is not by
   itself completed or accepted work; only retained, citable evidence
   counts.
2. Architecture decisions belong in ADRs (`docs/decisions/`); testable
   requirements belong in `docs/requirements-traceability.md` and
   `docs/functional-test-specification.md`.
3. All ten delivery phases of this PoC are implemented and evidenced; there
   is no further phase gate pending. Changes from here on are maintenance,
   extension, or new scope — evaluate each against the workflow invariants
   and rules below rather than against a phase plan.

## Current authorization boundary

- The project owner installs and manages all system tools (Java, Docker, PostgreSQL, IDE plugins/extensions) independently. Do not install or download them. Maven itself is the one documented exception: the committed Maven Wrapper (`mvnw`/`mvnw.cmd`) bootstraps Maven 3.9.16 on first build run per ADR 0009 — that is the approved provisioning path, not a tool the owner installs by hand.
- Planning, research, comparison, review, and documentation proposals are always allowed.
- Ask before altering a confirmed workflow rule below, changing a pinned tool version without a new/updated ADR, or making a change whose blast radius extends beyond this repository.

## Research standards

- Prefer official product documentation, standards, OWASP guidance, and well-established engineering sources.
- Record links and access dates.
- Distinguish source findings from project interpretation, approved decisions, and open questions.
- Compare alternatives and tradeoffs rather than selecting the newest or most fashionable tool.
- Research credible WMS behavior, but keep the PoC proportional to the job-application goal.
- Do not present assumptions as warehouse-industry facts.

## Confirmed workflow baseline

Unless the project owner explicitly changes it:

- confirmations require the exact task quantity;
- the initial HHT has no skip operation;
- task selection is global FIFO by order creation, line, and task sequence;
- task claims are atomic, with at most one active task per user/device;
- multi-bin lines split by ascending location code;
- stock decrements only at successful confirmation;
- stock, movement, task, line, and order changes commit atomically;
- `stock_movement` and `task_transition` are append-only;
- an administrator may block/resume a task per ADR 0004; the HHT itself still has no skip, block, or resume operation;
- the HHT is a separate LAN REST client;
- MFC integration is a future seam, not a TCP implementation.

## Technology and IDE rules

- Tool versions are pinned per ADR 0002 (Spring Boot 4.0.7, PostgreSQL 17.10 pinned by immutable image digest, pgJDBC 42.7.13) as amended by ADR 0009 (Maven 3.9.16, provisioned via the committed wrapper rather than a manual install) and ADR 0010 (JDK: any OpenJDK 21.x LTS distribution at the latest patch its channel offers — Temurin recommended, not required; CI pins Temurin). Do not change a pinned version without a new/updated ADR.
- The project owner installs workstation tools independently (JDK, Docker, PostgreSQL, IDE); Maven is wrapper-provisioned per ADR 0009.
- The project is developed across a Windows workstation and a Linux Mint 22 desktop; keep provisioning, scripts, and documentation OS-neutral or maintain matched per-OS variants (see `docs/runbook-windows.md` and `docs/runbook-linux.md`) rather than assuming Windows-only.
- Maven and Git are the portable sources of truth; do not depend on editor-only build behavior.
- IntelliJ migration means importing the existing Maven repository, not generating a second project or rewriting the application.
- Keep `.idea/`, local run configurations, workspace files, machine paths, and secrets out of version control unless a later review explicitly approves a safe shared file.
- Keep the project usable from both IntelliJ IDEA and VS Code through documented command-line workflows.
- Do not introduce Kotlin without an ADR, Java/Kotlin interoperation rules, and verified Maven/CI support.
- Do not rewrite working Java merely to demonstrate Kotlin.

## Data and configuration rules

- Flyway owns the schema; Hibernate may validate but must not create or update it.
- Never edit an applied migration. Once a migration has been applied to any retained environment it is immutable — fix forward with a new versioned migration, and never use `flyway repair` to conceal checksum drift.
- Keep common schema migrations separate from development fixtures and known demo credentials.
- Keep preproduction credentials external and never commit secrets or bearer tokens.
- Every stock-changing transaction must lock/update stock and append exactly one matching movement.
- Preserve relational consistency among order, line, task, article, location, stock, and movement records.
- Document each parameter's default, owner, environment, sensitivity, and restart requirement.

## Testing and observability rules

- Tests, diagnostics, logs, configuration, and runbooks are first-class deliverables.
- Include positive, negative, concurrency, idempotency, migration, and recovery coverage as applicable.
- Operational events must support correlation by request, order, task, user/device, article, location, and movement without exposing credentials or tokens.
- Retain objective evidence under a build/configuration identifier in `docs/evidence/`.
- Do not claim completion from compilation alone.

## Change discipline

- Alexander Gomez is the sole author of this project. Do not add `Co-Authored-By` or any other assistant-attribution — commit trailers, code comments, doc bylines, or anywhere else in the project — an assistant is a tool here, not a co-author.
- Prefer the smallest approved change and avoid unrelated reformatting.
- Update documentation, diagnostics, tests, and configuration references with behavioral changes.
- Record failed experiments and unresolved risks rather than hiding them.
- Stop and ask for approval before altering a confirmed workflow rule above or making a change with effects beyond this repository.
