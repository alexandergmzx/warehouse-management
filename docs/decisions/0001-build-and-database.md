# ADR 0001: Maven build and containerized PostgreSQL

- Status: superseded by ADR 0002
- Date: 2026-07-11

The research-gated baseline is recorded in
`docs/decisions/0002-supported-technology-and-local-baseline.md`.

This record predates the research gate. Its direction and exact versions remain
provisional until the live source/workstation checks and explicit Phase 3 owner
decision described in `docs/research/phase-3-decision-packet.md`.

## Context

The PoC must be repeatable on Windows and demonstrate Java, SQL, configuration, testing, and operational discipline. The database must behave like PostgreSQL in both development and tests.

## Proposed decision

Use Maven with Java 21 and provision PostgreSQL through Docker Compose for development. Use Testcontainers for migration/integration tests. Select exact Maven, PostgreSQL, Testcontainers, driver, and migration-tool versions only after the required compatibility review.

## Consequences

- Maven's lifecycle and XML are familiar in enterprise Java environments and keep CI commands unsurprising.
- Docker avoids machine-specific PostgreSQL installers, services, users, paths, and extension setup.
- PostgreSQL remains bound to loopback; only the Spring API is intended for LAN access.
- Docker Desktop adds installation, virtualization, memory, and licensing-policy considerations. Where corporate policy forbids it, a native PostgreSQL 17 service can use the same database/user values, but setup and cleanup become machine-specific and must be documented separately.
- Integration tests require a working Docker-compatible container runtime.
