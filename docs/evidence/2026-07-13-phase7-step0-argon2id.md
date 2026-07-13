# Phase 7 Step 0 evidence — Argon2id demo credentials on the pinned baseline

**Build/configuration identifier:** `91629af+phase7-step0-argon2id / 2026-07-13T22:56:37Z`
(git HEAD `91629afd0aa8a1399fb25ff2bf6c59899306e6dd` plus the Phase 7 Step 0
working tree; the identifier becomes immutable when the change is committed.)

## Scope

Objective evidence for the Phase 7 *execution plan* Step 0 prerequisite
(`PLAN.md`), which revises the Phase 6 demo fixture so authentication can be
built on Argon2id per ADR 0005 "Implementation refinement (2026-07-13)":

- the application exposes an Argon2id `PasswordEncoder` bean;
- demo credential hashes are stored as literal Argon2id PHC strings, not
  `pgcrypto` bcrypt fixtures;
- the schema holds the wider hash and no longer depends on `pgcrypto`;
- the migration/integrity suite still passes against the digest-pinned image;
- static quality gates (Checkstyle, SpotBugs) pass.

This record does **not** claim any Phase 7 acceptance-gate behavior (login,
claim, scan, confirm, admin recovery); those slices remain to be built.

## Changes in this step

| Artifact | Change |
|---|---|
| `pom.xml` | Add `org.bouncycastle:bcprov-jdk18on:1.85` (Argon2 provider; unmanaged by Boot 4, pinned per ADR 0005). |
| `configuration/PasswordEncoderConfiguration.java` | New `@Configuration` exposing `PasswordEncoder` = `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`. |
| `db/migration/V1__create_schema.sql` | `app_user.password_hash` widened `VARCHAR(100)`→`VARCHAR(255)`; `CREATE EXTENSION pgcrypto` removed (now unused). |
| `db/devdata/V1_1__seed_demo_data.sql` | `crypt('…', gen_salt('bf', 10))` bcrypt fixtures replaced by precomputed Argon2id PHC literals for `admin` / `picker01`. |
| `database/FlywayMigrationIT.java` | `crypt('picker123', …)` verification replaced by a format assertion that both seeded users store `$argon2id$`-prefixed hashes. |
| `docs/decisions/0005-…md` | Refinement section records the `pgcrypto` drop and the pinned BouncyCastle provider. |

### Execution-time sub-decisions (recorded per `PLAN.md` Step 0)

- **Fold into V1, not a new V2.** No retained or shared database exists (D-14);
  V1 has only ever run against disposable Testcontainers, so the width change
  and seed edit were folded into the immutable-once-applied V1 baseline before
  any durable dev database is created. This is the plan's recommended option.
- **`pgcrypto` dropped, not kept.** After removing the `crypt()`/`gen_salt()`
  fixtures, the only remaining UUID default (`gen_random_uuid()`) is core in
  PostgreSQL 13+, so the extension was genuinely unused. The clean migration
  below confirms nothing else depends on it.

## Precomputed hash parameters

Both demo hashes were generated once with the application encoder
(`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`). The self-describing
PHC header confirms the ADR 0005 parameters: `$argon2id$v=19$m=16384,t=2,p=1$…`
(Argon2id, 16 MiB memory, 2 iterations, parallelism 1). The throwaway generator
test was deleted after capture; no plaintext, hash regeneration step, or secret
is retained in source beyond the demo PHC literals in `db/devdata`.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B verify` |
| Maven | 3.9.16 |
| JDK | Eclipse Temurin 21.0.11 (`jdk-21.0.11.10-hotspot`), UTF-8 platform encoding |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-13, total time 37.5 s |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging (Boot 4.0.7 repackage) | Success |
| Flyway migration on empty schema (no `pgcrypto`) | `V1 create schema`, `V1.1 seed demo data` applied, 0.194 s |
| Failsafe `FlywayMigrationIT` | Tests run 5, failures 0, errors 0, skipped 0 (18.58 s) |
| `$argon2id$` seed-format assertion | Passes (both seeded users) |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` |

## Raw log excerpt (key lines)

```text
[INFO] --- failsafe:3.5.6:integration-test (default) @ warehouse-management ---
16:56:08.735 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Migrating schema "public" to version "1 - create schema"
16:56:08.897 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Migrating schema "public" to version "1.1 - seed demo data"
16:56:08.980 [main] INFO org.flywaydb.core.internal.command.DbMigrate -- Successfully applied 2 migrations to schema "public", now at version v1.1 (execution time 00:00.194s)
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 18.58 s -- in com.alexandergomez.wms.database.FlywayMigrationIT
[INFO] You have 0 Checkstyle violations.
[INFO] BugInstance size is 0
[INFO] Error size is 0
[INFO] BUILD SUCCESS
[INFO] Total time:  37.503 s
```

## Notes and residual risk

- The `PasswordEncoder` bean is defined but not yet exercised by a booted
  Spring context; real login verification (encode/verify round-trip) arrives
  with the Step 2 auth slice and the Java-level FT-01 test.
- `spring-boot-starter-security` remains on the classpath. With no
  `SecurityFilterChain` yet, default auto-configuration would secure endpoints;
  no HTTP endpoints exist at this step, so there is no behavioral impact. The
  approved filter chain is Step 2's deliverable.
