# Windows installation, operation, and rollback runbook

**Status:** Phase 9 deliverable (renamed from `docs/runbook.md` to
`docs/runbook-windows.md` by ADR 0010, for symmetry with
`docs/runbook-linux.md`; content otherwise unchanged except the
Docker-liveness note in Section 1). Covers a clean 64-bit Windows environment
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

Confirm Docker Desktop's engine is actually running before any
Docker-dependent step (Testcontainers, `compose.yaml`) — installed is not
the same as running, and a stopped/starting engine surfaces only as a
connection error such as `error during connect: this error may indicate
that the docker daemon is not running`:

```powershell
docker info --format "{{.ServerVersion}}"   # prints a version if the engine is up
```

If it does not, start Docker Desktop from the Start Menu (or
`Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"`) and wait
for the tray whale icon to report "Docker Desktop is running" before
retrying. Whether Docker Desktop also starts at login is its own
Settings > General toggle in Docker Desktop — a machine-wide choice outside
this runbook's scope, the same as the firewall-rule enablement decision in
Section 3.

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

### 5.1 Optional MFC/WCS integration (ADR 0011)

By default (`WMS_MFC_ADAPTER=noop`, or the variable unset) nothing in this
runbook changes: no telegrams are sent, no scheduler runs, and the sections
above are the complete procedure. Enable the real MFC integration only when
a WCS (`agv-fleet-controller`, or its stand-in) actually exists to receive
telegrams:

```powershell
$env:WMS_MFC_ADAPTER = "telegram"
$env:WMS_MFC_TELEGRAM_BASE_URL = "http://<wcs-host>:<port>"
$env:WMS_MFC_TRANSPORT_SOURCE_LOCATION = "<existing-location-code>"
$env:WMS_MFC_TRANSPORT_DESTINATION_LOCATION = "<existing-location-code>"
# optional tuning: WMS_MFC_TELEGRAM_RETRY_INTERVAL (PT30S), WMS_MFC_TELEGRAM_MAX_ATTEMPTS (5)
```

Operational facts to plan around (`docs/configuration-matrix.md` has the
full parameter reference; `TELEGRAMS.md` is the wire contract):

- **Fail-fast:** with `telegram` selected and no base-url, or missing
  transport locations, the application refuses to start and the failure
  names the missing property — same discipline as the FT-15 database
  checks. The two location codes must exist as `location` rows.
- **Network:** telegram delivery is an *outbound* HTTP POST from the WMS to
  the WCS base-url — no new inbound firewall rule for it. WCS confirmations
  arrive *inbound* on the existing API port (`/api/v1/mfc/missions/...`), so
  if the WCS sits outside the subnet scoped in Section 3, extend that rule's
  `-RemoteAddress` range rather than opening a new port.
- **Credentials:** the WCS authenticates with the same bearer-token login as
  every API client, under the `WCS` role. `wcs01`/`AGV-FC-01` exist **only**
  in the dev seed (`db/devdata/`); a preprod deployment must provision its
  own WCS-role user and device by a new versioned migration, with an
  externally supplied password — never reuse the dev credentials.
- **Recovery:** a mission that exhausts its dispatch attempts is marked
  `FAILED` with the last error recorded (`docs/sql-diagnostics.md` §5,
  `docs/log-analysis-guide.md`); missions queued while the WCS was down stay
  `PENDING` and are picked up automatically on a later scheduler tick.
- **Integration check:** `scripts/wcs-standin/wcs_standin.py` is a scripted
  WCS stand-in for verifying the loop end to end before a real fleet
  controller exists — see
  `docs/evidence/2026-07-19-mfc-transport-loop.md` for a recorded run.

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
