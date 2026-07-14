# Windows installation, operation, and rollback runbook

**Status:** Phase 9 deliverable. Covers a clean 64-bit Windows environment
running the `preprod` profile against a dedicated PostgreSQL instance. For
day-to-day development, see the shorter `README.md` walkthrough (Docker
Compose, `dev` profile, seeded demo data) instead — this runbook is the
operational/production-style procedure.

## 1. Prerequisites (owner-managed)

Per ADR 0002, install and record versions independently before starting:

```powershell
java -version    # Eclipse Temurin 21.0.11+
mvn -version     # Maven 3.9.16+
docker --version # only if PostgreSQL runs in a container
```

A reachable PostgreSQL 17.10 instance (containerized or native) with an empty
database is also required. `compose.yaml` is the documented convenience route
for a self-hosted container; a managed/native instance is equally acceptable
as long as network reachability and TLS/network policy match your
environment's requirements (this PoC does not itself configure database TLS).

## 2. Install on a clean environment

1. Clone or extract the repository to the target machine.
2. Build once to confirm the toolchain and pinned dependencies resolve:

   ```powershell
   mvn -B package -DskipTests
   ```

3. Set the required `preprod` environment variables (see
   `docs/configuration-matrix.md` for the full list, owners, and
   sensitivity). At minimum:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "preprod"
   $env:WMS_DB_URL = "jdbc:postgresql://<db-host>:5432/<db-name>"
   $env:WMS_DB_USERNAME = "<preprod-username>"
   $env:WMS_DB_PASSWORD = "<preprod-password>"   # never the committed dev default
   ```

   Starting with a missing or unsafe value fails fast with a clean
   diagnostic (FT-15) — see `docs/log-analysis-guide.md` if that happens.

4. Start the application. Flyway applies `db/migration` only — no
   development fixtures, no demo credentials:

   ```powershell
   mvn spring-boot:run
   ```

   or, from the packaged jar:

   ```powershell
   java -jar target/warehouse-management-0.1.0-SNAPSHOT.jar
   ```

5. Confirm health locally before opening any firewall rule:

   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

## 3. Scoped API-port firewall rule (LAN access for the HHT and dashboard)

Only the application port (`WMS_SERVER_PORT`, default `8080`) is ever exposed
to the LAN. PostgreSQL stays loopback-only or on a private database network —
never opened to the LAN directly. Scope the firewall rule to the specific LAN
subnet the HHT devices and dashboard users are on, not `Any`:

```powershell
New-NetFirewallRule -DisplayName "WMS API (LAN)" `
    -Direction Inbound -Protocol TCP -LocalPort 8080 `
    -RemoteAddress 192.168.1.0/24 `
    -Action Allow
```

Replace `192.168.1.0/24` with the actual warehouse LAN's subnet. Remove or
disable the rule as part of rollback (Section 6).

## 4. LAN/HHT reachability check

From a second machine on the same LAN segment as the HHT/dashboard clients
(not the server itself):

```powershell
Invoke-RestMethod http://<server-lan-ip>:8080/actuator/health
```

A successful `{"status":"UP", ...}` response confirms both the firewall rule
and the application are reachable from where the HHT devices actually sit.
If this fails but the loopback check in Section 2 succeeded, the problem is
the firewall rule or LAN routing, not the application — do not restart the
application to "fix" a network-layer failure.

## 5. Operating the service

- **Stop:** `Ctrl+C` in the running console, or stop the hosting Windows
  service/process manager if one wraps the JVM.
- **Restart after a configuration change:** every parameter in
  `docs/configuration-matrix.md` requires a restart — this PoC exposes no
  `/actuator/refresh` or config server.
- **Logs:** structured JSON on stdout (`docs/log-analysis-guide.md`);
  redirect/capture per your log-retention tooling — this application does
  not manage log retention itself (ADR 0006).

## 6. Rollback

Two distinct rollback scenarios, handled differently:

### 6.1 Application rollback (routine)

Stop the running process, deploy the previous known-good build (jar or
commit), and start it against the **same** database. Because Hibernate never
creates or alters schema (`ddl-auto: validate`) and Flyway migrations are
additive and immutable, a previous application version continues to run
correctly against a schema that has since gained additive migrations, as
long as no migration in between made a breaking, non-additive change (none
have, per the project's migration discipline).

### 6.2 Schema rollback — **not supported by design**

Flyway migrations in this project are immutable once applied to any retained
environment (CLAUDE.md, ADR 0002); there is no down-migration path and
`flyway repair` must never be used to conceal checksum drift. If a migration
introduces a defect, the fix is a **new, forward migration** that corrects
it — never edit or revert an applied one. Before applying any migration to a
preprod database for the first time, take a database-level backup/snapshot
through your PostgreSQL hosting platform's own mechanism (this application
does not provide one); that snapshot — not a schema down-migration — is the
actual schema-level recovery path if a migration proves faulty.

### 6.3 Firewall rollback

Remove the scoped rule added in Section 3:

```powershell
Remove-NetFirewallRule -DisplayName "WMS API (LAN)"
```

## 7. Evidence

Record the outcome of a runbook rehearsal (date/time, operator, build/
configuration identifier, each numbered step's result) under
`docs/evidence/`, per the project's evidence-retention discipline — a
successful rehearsal without a retained record does not close this phase's
acceptance-gate item.
