# ADR 0009: Cross-platform developer provisioning

- Status: accepted
- Date: 2026-07-14
- Amends: `docs/decisions/0002-supported-technology-and-local-baseline.md`
  (D-01 tool baseline); does not supersede it — the Spring Boot, PostgreSQL,
  Flyway, Testcontainers, Hibernate, and pgJDBC pins in ADR 0002 are
  unchanged.

## Context

Development has so far happened only on one 64-bit Windows workstation. The
owner also wants to develop on a Linux Mint 22 desktop, and asked for the
project to become devOS-agnostic. Two parts of ADR 0002's tool baseline are
Windows-provisioning-specific in practice even though nothing in the
application, tests, or `compose.yaml` is:

1. Maven 3.9.16 was installed manually as a tarball under `C:\Tools`. Debian/
   Ubuntu-family `apt` repositories (including Linux Mint 22, based on
   Ubuntu 24.04 "noble") do not carry that exact release, so the same manual
   step would need to be repeated, and kept in sync by hand, on every
   additional machine.
2. ADR 0002 pins an exact Temurin patch, `21.0.11+10`. Adoptium's `apt`
   repository (the standard Linux install route) tracks the latest patch
   within a major line, not an exact patch build, and the project's own CI
   (`.github/workflows/ci.yml`) already requests `java-version: "21"` from
   `actions/setup-java` — a floating patch, not `21.0.11` — so the exact-patch
   pin was already inconsistent with actual practice before this ADR.

## Decision

**Maven provisioning — adopt the Maven Wrapper.** Commit `mvnw`, `mvnw.cmd`,
and `.mvn/wrapper/maven-wrapper.properties`, generated via
`mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=only-script` against the
already-approved 3.9.16 release. This changes *how* the pinned version is
provisioned, not the pinned version itself: `./mvnw` (Linux/macOS) and
`.\mvnw.cmd` (Windows) each download and cache Apache Maven 3.9.16 from
`repo.maven.apache.org` on first run, byte-identical on both machines and in
CI, without a manual tarball install. A system-installed Maven 3.9.16 (as
already exists on the Windows workstation) remains equally valid; the
wrapper is the new documented default entry point, not a replacement
requirement.

**JDK provisioning — relax the pin to the latest Temurin 21 LTS patch.**
Replace ADR 0002's exact `21.0.11+10` pin with "latest available Eclipse
Temurin 21.x LTS patch" on both platforms:

- Windows: Adoptium MSI/zip installer for the latest 21.x build.
- Linux Mint 22: Adoptium's `apt` repository (`packages.adoptium.net`,
  `noble` component), `temurin-21-jdk`.

`pom.xml`'s `<java.version>21</java.version>` already only requires the
major version; nothing in the schema, migrations, or tests depends on the
specific patch build. This brings the documented baseline in line with what
CI has already been doing.

**Docker provisioning — Docker Engine on Linux, Docker Desktop on Windows.**
No change to the Compose-first decision in ADR 0002 (D-02); only the install
mechanism differs per OS (native `docker-ce` + `docker-compose-plugin` via
Docker's own `apt` repository on Mint, versus Docker Desktop on Windows).
Testcontainers and `compose.yaml` are unchanged and already OS-neutral.

## Consequences

- `README.md` and a new `docs/runbook-linux.md` document per-OS provisioning
  steps; the existing Windows-specific `docs/runbook-windows.md` (named
  `docs/runbook.md` at the time of this decision; renamed for symmetry by
  ADR 0010, content unchanged) is retained as-is
  (it is an evidenced Phase 9 deliverable) rather than rewritten.
- `CLAUDE.md`'s tool-version reference is updated to match this ADR's
  language so the two stay consistent, per its own "edit it here only" rule
  for canonical text.
- Future contributors run `./mvnw`/`.\mvnw.cmd` instead of assuming a
  system `mvn`; CI may adopt the wrapper too, but `ci.yml` already installs
  Maven via `setup-java`'s `cache: maven`, so switching it is optional
  follow-up, not required by this decision.
- A future patch-level regression traceable to a specific Temurin 21 build
  would require re-introducing an exact pin; none is known at this time.
- No schema, migration, dependency, or application code changes.
