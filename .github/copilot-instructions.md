# Miniature WMS workspace instructions

Read `PLAN.md` and `CLAUDE.md` before proposing or making changes. `PLAN.md` is the authoritative roadmap; `CLAUDE.md` contains the full repository-wide working rules and must remain aligned with this file.

## Current project mode

The project is in research and user-managed workstation preparation.

- Existing source code, migrations, configuration, API documentation, CI, and README content are provisional drafts, not approved implementation.
- Do not install or download Java, Maven, Docker, PostgreSQL, IntelliJ plugins, VS Code extensions, or other system tools. The project owner manages installation.
- Do not run builds, servers, containers, migrations, or integration tests unless explicitly requested.
- Do not implement, refactor, delete, or extend provisional application artifacts before the design checkpoint is explicitly approved.
- Research, compare, review, and plan first. Prefer official and authoritative sources, record access dates, and distinguish findings from decisions.
- Stop at each approval gate in `PLAN.md`.

## Confirmed workflow baseline

- Exact-quantity confirmations only; partial picks are rejected initially.
- No HHT skip operation initially; blocked work requires administrative recovery.
- Global FIFO by order creation, order line, and task sequence.
- Atomic claims with at most one active task per user/device.
- Multi-bin lines split by ascending location code.
- Stock decrements only on successful confirmation.
- Stock, task, line, order, and movement changes commit atomically.
- `stock_movement` is append-only and records every stock change.
- The Raspberry Pi HHT remains a separate LAN REST client.
- The MFC deliverable is an extension seam only, not TCP implementation.

## Portability and quality rules

- Maven and Git are the sources of truth; keep the repository usable from IntelliJ IDEA and VS Code.
- IntelliJ migration imports the existing Maven project and must not create a duplicate project or IDE-dependent build.
- Keep `.idea/`, local run configurations, machine paths, and secrets out of version control unless explicitly reviewed and approved.
- Do not introduce Kotlin without an ADR, interoperation rules, and Maven/CI verification.
- Flyway owns the schema; Hibernate may validate only.
- Never edit an applied migration. Decide explicitly how to treat provisional migrations before approving the baseline.
- Keep common schema migrations separate from development fixtures and known demo credentials.
- Tests, diagnostics, structured logs, configuration matrices, evidence, and runbooks are first-class deliverables.
- Do not mark work complete because files exist or compilation succeeds; require the acceptance evidence defined in `PLAN.md`.
