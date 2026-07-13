# Copilot instructions

**Full rules live in [`CLAUDE.md`](../CLAUDE.md) at the repository root — it is
the single canonical source for every assistant working in this repository
(Claude, Copilot, IntelliJ, VS Code, or any other editor). Read `CLAUDE.md`,
then read [`PLAN.md`](../PLAN.md) for the current phase, gate, and
authorization state, before proposing or making changes.**

Do not add or duplicate rules in this file — edit `CLAUDE.md` instead so every
assistant stays in sync. The subset below is repeated only as a safety net in
case this file is the only context a Copilot request has access to.

## Non-negotiable guardrails

- The project owner installs and manages all system tools (Java, Maven, Docker, PostgreSQL, IDE plugins/extensions). Do not install or download them.
- Do not implement, refactor, delete, or extend an artifact belonging to a phase that `PLAN.md` has not marked implemented and evidenced.
- Never edit an applied Flyway migration; add a new versioned migration instead.
- Every stock-changing transaction must update `stock` and append exactly one matching `stock_movement` row in the same transaction; `stock_movement` and `task_transition` are append-only.
- A compiling file or a passing build is not by itself acceptance evidence for a phase.
- Ask before crossing a gate recorded in `PLAN.md` or altering a confirmed workflow rule in `CLAUDE.md`.
