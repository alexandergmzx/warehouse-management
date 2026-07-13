# Miniature WMS project guidance

This file is the single canonical source of repository-wide working rules for
every assistant and editor used on this project — Claude, GitHub Copilot,
IntelliJ IDEA, VS Code, or any other tool. `.github/copilot-instructions.md`
is a short pointer to this file plus a safety-net subset for tools that only
auto-load from that path; it must not carry its own copy of these rules. If
you edit a rule, edit it here only.

## Source of truth

1. Read `PLAN.md` before proposing or making project changes — its "Plan status" section is the live record of the current phase, gate, and authorization state. Do not assume any phase/gate snapshot elsewhere (including in this file) is still current without checking it.
2. Treat any artifact belonging to a phase that `PLAN.md` has not marked implemented and evidenced as a provisional draft, regardless of whether it currently compiles or runs.
3. A compiling file, a passing build, or prior generated output is not by itself accepted design or a completed phase; only the evidence recorded in `PLAN.md` and `docs/evidence/` counts.
4. Architecture decisions belong in ADRs (`docs/decisions/`); testable requirements belong in `docs/requirements-traceability.md` and `docs/functional-test-specification.md`.

## Current authorization boundary

Stable rules that hold regardless of the current phase:

- The project owner installs and manages all system tools (Java, Maven, Docker, PostgreSQL, IDE plugins/extensions) independently. Do not install or download them.
- Do not implement, refactor, delete, or extend an artifact belonging to a phase `PLAN.md` has not marked implemented and evidenced.
- Once a phase is implemented and evidenced, its normal build/test/migration commands (e.g. `mvn verify` with Testcontainers) are the accepted workflow for that phase — this is not by itself authorization to extend a later, still-gated phase.
- Planning, research, comparison, review, and documentation proposals are always allowed.
- Ask before crossing a gate recorded in `PLAN.md` or altering a confirmed workflow rule below.

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

- Tool versions are pinned per ADR 0002 (Temurin 21.0.11, Maven 3.9.16, Spring Boot 4.0.7, PostgreSQL 17.10 pinned by immutable image digest, pgJDBC 42.7.13). Do not change a pinned version without a new/updated ADR.
- The project owner installs workstation tools independently.
- Maven and Git are the portable sources of truth; do not depend on editor-only build behavior.
- IntelliJ migration means importing the existing Maven repository, not generating a second project or rewriting the application.
- Keep `.idea/`, local run configurations, workspace files, machine paths, and secrets out of version control unless a later review explicitly approves a safe shared file.
- Keep the project usable from both IntelliJ IDEA and VS Code through documented command-line workflows.
- Do not introduce Kotlin without an ADR, Java/Kotlin interoperation rules, and verified Maven/CI support.
- Do not rewrite working Java merely to demonstrate Kotlin.

## Data and configuration rules

- Flyway owns the schema; Hibernate may validate but must not create or update it.
- Never edit an applied migration. The V1 baseline was replaced once, pre-application, per D-14/ADR 0002; once a migration has been applied to any retained environment it is immutable — fix forward with a new versioned migration, and never use `flyway repair` to conceal checksum drift.
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

- Prefer the smallest approved change and avoid unrelated reformatting.
- Update documentation, diagnostics, tests, and configuration references with behavioral changes.
- Record failed experiments and unresolved risks rather than hiding them.
- Before implementation, classify provisional artifacts as keep, revise, replace, or remove.
- Stop and ask for approval when a request would cross the current plan gate or alter a confirmed workflow rule.
