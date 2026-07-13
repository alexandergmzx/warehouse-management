# Miniature WMS project guidance

This file defines repository-wide working rules for assistants and developers using Claude, IntelliJ IDEA, VS Code, GitHub Copilot, or another editor. Keep it aligned with `.github/copilot-instructions.md`.

## Source of truth

1. Read `PLAN.md` before proposing or making project changes.
2. Treat `API.md`, the current schema, source code, configuration, CI, and README as provisional drafts until the research and design gates in `PLAN.md` are approved.
3. A compiling file is not automatically an accepted design or completed phase.
4. Architecture decisions belong in ADRs; testable requirements belong in the traceability matrix and functional specification.

## Current authorization boundary

The project is currently in research and user-managed workstation preparation.

- Do not install or download Java, Maven, Docker, PostgreSQL, IntelliJ plugins, VS Code extensions, or other system tools.
- Do not run servers, containers, migrations, integration tests, or build commands unless the project owner explicitly asks.
- Do not implement, refactor, delete, or extend provisional application files before explicit design approval.
- Planning, research, comparison, review, and documentation proposals are allowed.
- Ask before crossing a gate in `PLAN.md`.

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
- `stock_movement` is append-only;
- the HHT is a separate LAN REST client;
- MFC integration is a future seam, not a TCP implementation.

## Technology and IDE rules

- Tool versions remain decisions until the research checkpoint approves them.
- The project owner installs workstation tools independently.
- Maven and Git are the portable sources of truth; do not depend on editor-only build behavior.
- IntelliJ migration means importing the existing Maven repository, not generating a second project or rewriting the application.
- Keep `.idea/`, local run configurations, workspace files, machine paths, and secrets out of version control unless a later review explicitly approves a safe shared file.
- Keep the project usable from both IntelliJ IDEA and VS Code through documented command-line workflows.
- Do not introduce Kotlin without an ADR, Java/Kotlin interoperation rules, and verified Maven/CI support.
- Do not rewrite working Java merely to demonstrate Kotlin.

## Data and configuration rules

- Flyway owns the schema; Hibernate may validate but must not create or update it.
- Never edit an applied migration. Before the real baseline is approved, explicitly decide whether provisional migrations can be replaced.
- Keep common schema migrations separate from development fixtures and known demo credentials.
- Keep preproduction credentials external and never commit secrets or bearer tokens.
- Every stock-changing transaction must lock/update stock and append exactly one matching movement.
- Preserve relational consistency among order, line, task, article, location, stock, and movement records.
- Document each parameter's default, owner, environment, sensitivity, and restart requirement.

## Testing and observability rules

- Tests, diagnostics, logs, configuration, and runbooks are first-class deliverables.
- Include positive, negative, concurrency, idempotency, migration, and recovery coverage as applicable.
- Operational events must support correlation by request, order, task, user/device, article, location, and movement without exposing credentials or tokens.
- Retain objective evidence under a build/configuration identifier.
- Do not claim completion from compilation alone.

## Change discipline

- Prefer the smallest approved change and avoid unrelated reformatting.
- Update documentation, diagnostics, tests, and configuration references with behavioral changes.
- Record failed experiments and unresolved risks rather than hiding them.
- Before implementation, classify provisional artifacts as keep, revise, replace, or remove.
- Stop and ask for approval when a request would cross the current plan gate or alter a confirmed workflow rule.
