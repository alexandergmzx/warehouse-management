# ADR 0002: Supported technology and local baseline

- Status: accepted; runtime baseline validated 2026-07-13
  (`docs/evidence/2026-07-13-phase6-maven-verify.md`)
- Date: 2026-07-13
- Decisions: D-01, D-02, D-03, D-14
- Supersedes: `docs/decisions/0001-build-and-database.md`

## Context

The portfolio MVP needs a reproducible Java/SQL baseline while remaining usable
from Maven, IntelliJ IDEA, and VS Code. Existing application files and the
initial migration are provisional. The owner reported no retained database that
requires the provisional migration history.

## Decision

Use Java only with Eclipse Temurin Java 21.0.11+10, Maven 3.9.16, Spring Boot
4.0.7, PostgreSQL 17.10, Flyway 11.14.1, Testcontainers 2.0.5, Hibernate ORM
7.2.19.Final, and pgJDBC 42.7.13. The pgJDBC version explicitly overrides the
Boot-managed 42.7.11 value because the vendor-documented security fix begins
with 42.7.12.

Use Docker Compose as the primary local PostgreSQL route, loopback-bound, with
native PostgreSQL documented as a fallback. Use PostgreSQL Testcontainers for
integration evidence. Pin the final PostgreSQL image by immutable digest after
runtime validation.

Replace the unapplied provisional V1 with a clean approved baseline only after
the complete design gate. Never edit an applied migration or use `repair` to
conceal checksum drift.

## Consequences

- Java remains the sole JVM language; Kotlin requires a later ADR.
- Docker runtime evidence and Maven/IDE equivalence remain acceptance work.
- The existing `0001` record is retained as historical context and superseded
  by this decision.
- No version, migration, or build file is changed by this ADR alone.

