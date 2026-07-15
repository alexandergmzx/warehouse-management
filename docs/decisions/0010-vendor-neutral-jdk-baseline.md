# ADR 0010: Vendor-neutral JDK 21 baseline

- Status: accepted
- Date: 2026-07-15
- Amends: `docs/decisions/0009-cross-platform-developer-provisioning.md`
  (JDK provisioning clause only); does not supersede it — the Maven Wrapper
  and Docker provisioning decisions in ADR 0009, and every ADR 0002 pin it
  left standing (Spring Boot, PostgreSQL, Flyway, Testcontainers, Hibernate,
  pgJDBC), are unchanged.

## Context

ADR 0009 relaxed ADR 0002's exact `21.0.11+10` pin to "latest available
Eclipse Temurin 21.x LTS patch". It relaxed the *patch* while keeping the
*vendor* fixed to Eclipse Temurin, and documented Adoptium as the only
install route on each OS.

Provisioning the Linux Mint 22 desktop surfaced the practical cost of the
vendor clause. The machine already carries Ubuntu's distribution-packaged
OpenJDK 21 (`openjdk-21-jdk`, `21.0.11+10-1~24.04.2`) from the `noble`
archive, installed and patched through the OS's own update channel. Under
ADR 0009 as written, provisioning would mean adding Adoptium's third-party
apt repository and a second, parallel JDK 21 to a machine that already has a
conformant one, leaving `update-alternatives` to arbitrate which `java` lands
on `PATH` — a standing provisioning hazard for no functional gain.

Verified on this machine (2026-07-15, commit `b95e298`):

- `java -version` → `openjdk 21.0.11 2026-04-21`, build
  `21.0.11+10-1-24.04.2-Ubuntu`
- `./mvnw -B package -DskipTests` → `BUILD SUCCESS`; `./mvnw -v` reports
  `Java version: 21.0.11, vendor: Ubuntu`, `Apache Maven 3.9.16`

The patch level (`21.0.11+10`) is identical to the Temurin build ADR 0002
originally pinned; only the packaging vendor differs. Both are builds of the
same upstream OpenJDK 21 sources.

Nothing in this project depends on a vendor-specific JDK capability:
`pom.xml` requires only `<java.version>21</java.version>`, and the schema,
Flyway migrations, Testcontainers usage, and test suite are vendor-agnostic.
No Temurin-only behavior is relied upon.

## Decision

**Relax the JDK pin from a vendor to a specification.** The baseline is now
*any OpenJDK 21.x LTS distribution, at the latest patch available through the
chosen install channel*, on any supported development OS. Eclipse Temurin
remains the recommended and documented default; it is no longer mandatory.

Explicitly acceptable:

- Linux Mint 22 / Ubuntu 24.04: distribution-packaged `openjdk-21-jdk` from
  the `noble` archive, **or** Adoptium's `temurin-21-jdk`.
- Windows: Adoptium MSI/zip for the latest 21.x build, or an equivalent
  OpenJDK 21 build.

**CI continues to pin `distribution: temurin`.**
`.github/workflows/ci.yml` is unchanged by this ADR. CI is the arbiter of a
build's correctness, so it stays on one deterministic distribution rather
than floating across vendors. Local vendor variation is therefore bounded:
anything vendor-specific still meets a single fixed distribution before merge.

**Windows runbook renamed for naming symmetry — `docs/runbook.md` →
`docs/runbook-windows.md`**, alongside `docs/runbook-linux.md`. Raised
separately from the JDK question during this same provisioning session; a
plain file move (content byte-identical) rather than an architectural
decision in its own right, so it is recorded here rather than in a dedicated
ADR. This **supersedes, for the filename only,** ADR 0009's Consequences
clause that the file would be "retained as-is... rather than rewritten" —
that clause was reasoning about not needing to rewrite Windows-specific
*content* for cross-platform support, not a commitment to never touch the
file again. The content itself remains unedited by this ADR.

## Consequences

- A provisioned machine may satisfy the baseline with the JDK its OS already
  ships; no third-party apt repository is required on Debian/Ubuntu-family
  systems.
- Dev and CI may now differ in JDK *vendor* (never in major version). This is
  accepted: both build the same upstream sources, and CI remains
  authoritative. A defect reproducible only on a local vendor's build is a
  legitimate finding to raise, not a silent divergence.
- `CLAUDE.md`'s tool-version rule, `README.md`'s stated baseline, and
  `docs/runbook-linux.md` §1 are updated to state the specification and to
  present Adoptium as one route rather than the required one.
- `docs/runbook-windows.md` (renamed from `docs/runbook.md`; content
  unchanged) gains the same Docker-daemon-liveness caution
  `docs/runbook-linux.md` §1 already carries, adapted for Docker Desktop.
  Every cross-reference to the old filename (`README.md`, `CLAUDE.md`,
  `docs/runbook-linux.md`, ADR 0009, and the Phase 9/10/final-acceptance-sweep
  evidence files) is updated to the new one.
- ADRs 0002 and 0009 are otherwise retained unedited as historical records;
  this ADR is the current statement of the JDK pin.
- Retained evidence recording "Eclipse Temurin 21.0.11" accurately describes
  the toolchain those runs actually used and stays as-is.
- Should a vendor-specific regression ever appear, re-pinning to a single
  vendor (or to an exact build) requires a new ADR.
- No schema, migration, dependency, CI, or application code changes.
