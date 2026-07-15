# Linux Mint 22 developer provisioning evidence

**Build/configuration identifier:** `b95e298+adr0010 / 2026-07-15T02:36:56-06:00`
(git HEAD `b95e298` — the committed cross-platform provisioning baseline —
plus the ADR 0010 working tree; the identifier becomes immutable when this
change is committed.)

## Scope

First provisioning of the Linux Mint 22 desktop introduced by ADR 0009, and
the first execution of the test suite on a non-Windows machine. Verifies
`docs/runbook-linux.md` §1–§2 against a real environment. Sections 3–4
(firewall rule, LAN/HHT reachability) and the `preprod` runtime rehearsal are
**not** covered — see Not verified.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify` |
| Maven / JDK | 3.9.16 (wrapper-provisioned) / **OpenJDK 21.0.11, vendor: Ubuntu** (`21.0.11+10-1-24.04.2-Ubuntu`) |
| Operating system | Linux Mint 22.3, x86_64, kernel 6.8.0-134-generic |
| Docker engine | Docker Engine (Community) server 29.6.1, Compose v5.3.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-15T02:36:56-06:00, total time 45.569 s |

## Results

| Gate | Result |
|---|---|
| Maven Wrapper bootstraps 3.9.16 (ADR 0009) | **Pass** — `./mvnw -v` → `Apache Maven 3.9.16`, cached under `~/.m2/wrapper/dists/`; no manual Maven install |
| `./mvnw -B package -DskipTests` (runbook §2.2) | **Pass** — `BUILD SUCCESS` |
| `./mvnw -B verify` — full suite | **Pass** — `BUILD SUCCESS`, `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0` |
| Testcontainers PostgreSQL 17.10 on Docker Engine | **Pass** — container started, Flyway applied 2 migrations to `v1.1`, `Database version: 17.10` |
| Line-ending normalization (`.gitattributes`) | **Pass** — `git ls-files --eol`: `mvnw` `i/lf w/lf`, `mvnw.cmd` `i/lf w/crlf`, `pom.xml` `i/lf w/lf` |
| Application/schema changes required for Linux | **None** — ADR 0009's OS-neutrality claim holds; the 33 tests are the same suite that passes on Windows and CI |

## Findings

### F-1 — JDK vendor deviated from ADR 0009; resolved by ADR 0010

The machine carries Ubuntu's distribution-packaged OpenJDK 21
(`openjdk-21-jdk`, `21.0.11+10-1~24.04.2`), not Eclipse Temurin as ADR 0009
required. The full suite passes on it; the patch level is identical to the
Temurin build ADR 0002 originally pinned, and only the packaging vendor
differs.

Resolved by owner decision (2026-07-15) to relax the pin from a vendor to a
specification rather than install a second, parallel JDK 21 on a machine that
already had a conformant one. See ADR 0010. CI continues to pin
`distribution: temurin` and remains the arbiter of build correctness.

### F-2 — Docker installed but daemon not running

`docker --version` reported 29.6.1 while the daemon was down: `docker.service`
was `inactive (dead)` and `disabled`, having shut down cleanly on 2026-07-13
(journal shows `Daemon shutdown complete`, no crash). The only symptom was
`Cannot connect to the Docker daemon at unix:///var/run/docker.sock`.
Confirmed as a real daemon state, not a sandbox artifact, before acting.

Resolved by `sudo systemctl start docker` (owner-run). Boot behavior was
deliberately left unchanged, consistent with the runbook's treatment of
`ufw enable` as a machine-wide decision. `docker.socket` is `active`/`enabled`
but did **not** socket-activate the daemon on connection — noted as an open
question below, not diagnosed.

`docs/runbook-linux.md` §1 gained an explicit daemon-liveness check: being
installed is not the same as being up. The same caution, adapted for Docker
Desktop, was added to `docs/runbook-windows.md` §1 by owner request — this
class of gap is not Linux-specific, and the Windows runbook had never been
exercised against a stopped engine either. `docs/runbook-windows.md` is
`docs/runbook.md` renamed for symmetry (ADR 0010); not otherwise re-verified
on Windows as part of this session — the addition is a documentation
parity fix, not a rehearsed Windows finding.

### F-3 — Runbook's own `preprod` export breaks the test suite (documentation defect)

The first `./mvnw -B verify` on this machine failed: `BUILD FAILURE`,
`Tests run: 33, Failures: 0, Errors: 26`, every error reported only as
`Failed to load ApplicationContext`. The Maven summary never names the cause;
it appears only in `target/failsafe-reports/`:

```
Caused by: com.alexandergomez.wms.configuration.PreprodConfigurationException:
Missing required preprod configuration: WMS_DB_URL, WMS_DB_USERNAME, WMS_DB_PASSWORD.
```

**Root cause:** `SPRING_PROFILES_ACTIVE=preprod` was live in the session
environment — inherited from the parent shell, present in no profile file
(`~/.bashrc`, `~/.profile`, `/etc/environment` all checked and clean). It came
from following `docs/runbook-linux.md` §2 step 3, which instructs the operator
to `export SPRING_PROFILES_ACTIVE=preprod` in the same shell that §2 step 2
had just used to run `./mvnw`. The integration tests then booted under
`preprod`, where the FT-15 guard correctly refused to start against
Testcontainers' generated datasource.

**This is not an application defect — FT-15 behaved exactly as specified.**
The defect is in the runbook: it walks the operator into a state where the
suite fails opaquely, and the diagnostic quality that FT-15 provides at
application startup is lost inside Surefire/Failsafe's context-load reporting.

Proven by re-running the identical suite with the variable removed:
`env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify` → `BUILD SUCCESS`, 33/33,
0 errors. Same commit, same machine, same Docker daemon; the only delta was
the inherited variable.

`docs/runbook-linux.md` §2 step 3 now warns that the exports persist for the
shell session, that the test suite inherits them, and gives the `env -u`
override.

## Not verified on this machine

Recorded rather than claimed, per the project's evidence discipline:

- **Runbook §3 (scoped `ufw` API-port rule) and §4 (LAN/HHT reachability).**
  Not exercised; both require a second LAN machine and a machine-wide
  firewall decision.
- **`preprod` runtime rehearsal against a real PostgreSQL instance** (runbook
  §2 steps 3–5). The FT-15 *guard* was observed firing (F-3, incidentally),
  but a successful `preprod` startup was not performed here.
- **`compose.yaml` dev-profile startup and the seeded demo walkthrough.** The
  Testcontainers path exercised the same pinned image digest, but the
  Compose route itself was not run.
- **Docker daemon behavior across a reboot.** Boot behavior was intentionally
  left as-is (F-2), so the daemon will need starting again after a reboot.

## Open questions

- `docker.socket` is `active` and `enabled`, yet connecting to
  `/var/run/docker.sock` while `docker.service` was stopped returned
  `Cannot connect to the Docker daemon` instead of socket-activating it. Not
  investigated — starting the service resolved the immediate need. Worth a
  look if the daemon is ever expected to start on demand rather than being
  started explicitly.
