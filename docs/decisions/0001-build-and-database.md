# ADR 0001: Maven build and containerized PostgreSQL

- Status: accepted
- Date: 2026-07-11

## Context

The PoC must be repeatable on Windows and demonstrate Java, SQL, configuration, testing, and operational discipline. The database must behave like PostgreSQL in both development and tests.

## Decision

Use Maven with Java 21 and provision PostgreSQL 17 through Docker Compose for development. Use Testcontainers for migration/integration tests.

## Consequences

- Maven's lifecycle and XML are familiar in enterprise Java environments and keep CI commands unsurprising.
- Docker avoids machine-specific PostgreSQL installers, services, users, paths, and extension setup.
- PostgreSQL remains bound to loopback; only the Spring API is intended for LAN access.
- Docker Desktop adds installation, virtualization, memory, and licensing-policy considerations. Where corporate policy forbids it, a native PostgreSQL 17 service can use the same database/user values, but setup and cleanup become machine-specific and must be documented separately.
- Integration tests require a working Docker-compatible container runtime.
