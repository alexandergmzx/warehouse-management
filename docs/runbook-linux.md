# Linux Mint 22 installation, operation, and rollback runbook

**Status:** counterpart to `docs/runbook.md` (Phase 9 deliverable), added under
ADR 0009 (cross-platform developer provisioning). Covers a clean Linux Mint
22 environment running the `preprod` profile against a dedicated PostgreSQL
instance. For day-to-day development, see the shorter `README.md` walkthrough
(Docker Compose, `dev` profile, seeded demo data) instead — this runbook is
the operational/production-style procedure. Every invariant, warning, and
rollback boundary in `docs/runbook.md` applies here unchanged; only the
shell syntax and OS-specific tooling differ.

## 1. Prerequisites (owner-managed)

Per ADR 0002 as amended by ADR 0009, install and record versions
independently before starting:

```bash
java -version      # latest Eclipse Temurin 21.x LTS (Adoptium apt repo)
docker --version    # only if PostgreSQL runs in a container
docker compose version
```

Maven itself does not need a separate install: the committed wrapper
(`./mvnw`) bootstraps Maven 3.9.16 on first run. If a system Maven 3.9.16 is
already installed, that is equally valid.

JDK install, if not already present (Adoptium's `apt` repository, `noble`
component — Linux Mint 22 is based on Ubuntu 24.04):

```bash
sudo apt update && sudo apt install -y wget apt-transport-https gpg
wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb noble main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-21-jdk
```

Docker install, if not already present (Docker's own `apt` repository, not
the distribution-packaged `docker.io`):

```bash
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu noble stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update && sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker "$USER"   # log out/in for the group change to take effect
```

A reachable PostgreSQL 17.10 instance (containerized or native) with an empty
database is also required. `compose.yaml` is the documented convenience route
for a self-hosted container; a managed/native instance is equally acceptable
as long as network reachability and TLS/network policy match your
environment's requirements (this PoC does not itself configure database TLS).

## 2. Install on a clean environment

1. Clone or extract the repository to the target machine.
2. Build once to confirm the toolchain and pinned dependencies resolve:

   ```bash
   ./mvnw -B package -DskipTests
   ```

3. Set the required `preprod` environment variables (see
   `docs/configuration-matrix.md` for the full list, owners, and
   sensitivity). At minimum:

   ```bash
   export SPRING_PROFILES_ACTIVE=preprod
   export WMS_DB_URL="jdbc:postgresql://<db-host>:5432/<db-name>"
   export WMS_DB_USERNAME="<preprod-username>"
   export WMS_DB_PASSWORD="<preprod-password>"   # never the committed dev default
   ```

   Starting with a missing or unsafe value fails fast with a clean
   diagnostic (FT-15) — see `docs/log-analysis-guide.md` if that happens.

4. Start the application. Flyway applies `db/migration` only — no
   development fixtures, no demo credentials:

   ```bash
   ./mvnw spring-boot:run
   ```

   or, from the packaged jar:

   ```bash
   java -jar target/warehouse-management-0.1.0-SNAPSHOT.jar
   ```

5. Confirm health locally before opening any firewall rule:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

## 3. Scoped API-port firewall rule (LAN access for the HHT and dashboard)

Only the application port (`WMS_SERVER_PORT`, default `8080`) is ever exposed
to the LAN. PostgreSQL stays loopback-only or on a private database network —
never opened to the LAN directly. Scope the firewall rule to the specific LAN
subnet the HHT devices and dashboard users are on, not "anywhere". Linux Mint
ships with `ufw` (Uncomplicated Firewall) as its default front end for
`iptables`/`nftables`:

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp comment "WMS API (LAN)"
sudo ufw status numbered
```

Replace `192.168.1.0/24` with the actual warehouse LAN's subnet. If `ufw` is
not yet enabled (`sudo ufw status` reports inactive), enabling it is a
separate, machine-wide decision outside this runbook's scope — coordinate
before running `sudo ufw enable` on a machine with other services depending
on default-allow behavior. Remove or disable the rule as part of rollback
(Section 6).

## 4. LAN/HHT reachability check

From a second machine on the same LAN segment as the HHT/dashboard clients
(not the server itself):

```bash
curl http://<server-lan-ip>:8080/actuator/health
```

A successful `{"status":"UP", ...}` response confirms both the firewall rule
and the application are reachable from where the HHT devices actually sit.
If this fails but the loopback check in Section 2 succeeded, the problem is
the firewall rule or LAN routing, not the application — do not restart the
application to "fix" a network-layer failure.

## 5. Operating the service

- **Stop:** `Ctrl+C` in the running console, or stop the hosting `systemd`
  unit/process manager if one wraps the JVM.
- **Restart after a configuration change:** every parameter in
  `docs/configuration-matrix.md` requires a restart — this PoC exposes no
  `/actuator/refresh` or config server.
- **Logs:** structured JSON on stdout (`docs/log-analysis-guide.md`);
  redirect/capture per your log-retention tooling (e.g. `journald` if run
  under `systemd`) — this application does not manage log retention itself
  (ADR 0006).

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

```bash
sudo ufw status numbered            # find the rule number
sudo ufw delete <rule-number>
```

## 7. Evidence

Record the outcome of a runbook rehearsal (date/time, operator, build/
configuration identifier, each numbered step's result) under
`docs/evidence/`, per the project's evidence-retention discipline — a
successful rehearsal without a retained record does not close this phase's
acceptance-gate item.
