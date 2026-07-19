# Configuration matrix

**Status:** Introduced as a Phase 9 deliverable; extended in Phase 10. Covers
every externally configurable parameter, plus the safety-critical values
that are deliberately fixed per profile rather than externally configurable.

All parameters below are read once at Spring context startup (no
`@RefreshScope` or Actuator `/actuator/refresh` is exposed in this PoC), so
**every row requires an application restart to take effect** — that column is
omitted from the table below and stated once here instead.

## Externally configurable (environment variable)

| Variable | Owner | Default | Environment | Sensitivity | Purpose |
|---|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Deployment operator | `dev` (`spring.profiles.default`) | dev, preprod | Low | Selects the active profile; preprod must be set explicitly — it is never the default. |
| `WMS_DB_URL` | Database administrator | dev: `jdbc:postgresql://localhost:5432/wms`; preprod: **none — required** | dev, preprod | Low (URL, no credential) | JDBC connection string. |
| `WMS_DB_USERNAME` | Database administrator | dev: `wms`; preprod: **none — required** | dev, preprod | Medium (credential identity) | Database login. |
| `WMS_DB_PASSWORD` | Database administrator | dev: `wms_dev_password`; preprod: **none — required** | dev, preprod | **High — secret** | Database password. Never commit a real value; local dev value lives only in `.env`/`compose.yaml` defaults and is not valid outside a developer workstation. |
| `WMS_DB_NAME` | Database administrator | `wms` | dev (Compose only) | Low | Database name created by `compose.yaml`. Not read by the application itself — only by Compose. |
| `WMS_DB_PORT` | Database administrator | `5432` | dev (Compose only) | Low | Host-side port `compose.yaml` binds PostgreSQL to (loopback-only). |
| `WMS_SERVER_ADDRESS` | Deployment operator (network/firewall coordination) | `0.0.0.0` | dev, preprod | Low | API/dashboard bind address. |
| `WMS_SERVER_PORT` | Deployment operator (network/firewall coordination) | `8080` | dev, preprod | Low | API/dashboard port; the only port the installation runbook opens to the LAN. |
| `WMS_TASK_STUCK_THRESHOLD` | Application owner (operations tuning) | `PT30M` (ISO-8601 duration) | dev, preprod | Low | Active-task age after which the dashboard/admin API flags a task as stuck. |
| `WMS_AUTH_TOKEN_TTL` | Application owner (security/operations tuning) | `PT8H` (ISO-8601 duration) | dev, preprod | Low | HHT bearer-token absolute lifetime (ADR 0005). |
| `WMS_DASHBOARD_POLL_INTERVAL` | Application owner (operations tuning) | `PT2S` (ISO-8601 duration) | dev, preprod | Low | Dashboard client-side polling interval (ADR 0007). |
| `WMS_MFC_ADAPTER` | Application owner | `noop` | dev, preprod | Low | Selects the `OrderCompletionPublisher` adapter (ADR 0007). `noop` (default) does nothing beyond one log line; `telegram` (ADR 0011, MFC work package) emits real MFC mission telegrams. |
| `WMS_MFC_TELEGRAM_BASE_URL` | Application owner (ecosystem integration) | *(empty)* | dev, preprod | Low (a LAN URL, not a secret) | WCS base URL the dispatcher POSTs telegrams to; required (constructor refuses to start) when `WMS_MFC_ADAPTER=telegram`, unused otherwise. |
| `WMS_MFC_TELEGRAM_RETRY_INTERVAL` | Application owner (operations tuning) | `PT30S` (ISO-8601 duration) | dev, preprod | Low | Dispatcher poll interval and per-mission retry backoff (ADR 0011); only active under the `telegram` adapter. |
| `WMS_MFC_TELEGRAM_MAX_ATTEMPTS` | Application owner (operations tuning) | `5` | dev, preprod | Low | Dispatch attempts before a mission is marked `FAILED` (ADR 0011); only active under the `telegram` adapter. |
| `WMS_MFC_TRANSPORT_SOURCE_LOCATION` | Application owner (ecosystem integration) | *(empty)* | dev, preprod | Low | Location code TRANSPORT missions use as the tote source (TELEGRAMS.md); must name an existing `location.code`. Required (first-use failure) when `WMS_MFC_ADAPTER=telegram`. |
| `WMS_MFC_TRANSPORT_DESTINATION_LOCATION` | Application owner (ecosystem integration) | *(empty)* | dev, preprod | Low | Location code TRANSPORT missions use as the handover destination (TELEGRAMS.md); must name an existing `location.code`. Required (first-use failure) when `WMS_MFC_ADAPTER=telegram`. |

## Fixed per profile (not externally configurable)

These are deliberately **not** environment-variable-tunable — they are
safety-critical and changing them per-deployment would undermine the
guarantee they exist to provide.

| Setting | Owner | dev value | preprod value | Sensitivity | Why fixed |
|---|---|---|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | Application owner | `validate` | `validate` | Low | Flyway owns the schema (CLAUDE.md); Hibernate must never create/alter it in any environment. |
| `spring.flyway.locations` | Application owner | `db/migration` + `db/devdata` | `db/migration` only | Low | Preprod must never install demonstration fixtures or seeded credentials. |
| `management.endpoints.web.exposure.include` | Application owner | `health,info` | `health,info` | Low | Keeps the actuator surface minimal; no env/beans/metrics exposure without a deliberate future decision. |
| `logging.level.com.alexandergomez.wms` | Application owner | `DEBUG` | `INFO` | Low | Preprod avoids the verbosity/PII-adjacent risk of `DEBUG` business-logic logging. |
| `logging.level.org.hibernate.SQL` | Application owner | `INFO` (dev only entry) | unset (Spring default) | Low | Dev-only SQL visibility for troubleshooting; never enabled in preprod. |

## Startup validation (FT-15, Phase 9 Step 1)

Under the `preprod` profile only, `PreprodConfigurationValidator`
(`EnvironmentPostProcessor`) checks the required variables above before any
bean or datasource is created:

- **Missing:** `WMS_DB_URL`, `WMS_DB_USERNAME`, or `WMS_DB_PASSWORD` absent or
  blank → the application fails fast with one clean diagnostic line naming
  only the missing variable (never its value, which does not exist to leak).
- **Unsafe:** `WMS_DB_PASSWORD` equal to the committed development default
  (`wms_dev_password`) → the application fails fast rather than silently
  running preprod against a known, publicly committed credential.

See `docs/evidence/` for the corresponding FT-15 test evidence and
`docs/log-analysis-guide.md` for reading the resulting diagnostic.
