# Phase 9 evidence — configuration validation, structured logging, CI review, and runbook

**Build/configuration identifier:** `a845a74+phase9 / 2026-07-14T10:16:20-06:00`
(git HEAD `a845a74` — the committed Phase 8 baseline — plus the Phase 9
working tree; the identifier becomes immutable when this change is
committed.)

## Scope

Objective evidence for the Phase 9 *execution plan* Steps 0–8 (`PLAN.md`):
configuration matrix, preprod startup validation (FT-15), structured logging
completion (FT-16), a CI-workflow review, and the runbook/log-analysis/
incident-template/executed-test-report documentation deliverables.

## What was added

| Area | Artifacts |
|---|---|
| `docs/configuration-matrix.md` | New — every `WMS_*`/`spring.*` parameter: owner, default, environment, sensitivity, restart requirement |
| `configuration` (new classes) | `PreprodConfigurationException`, `PreprodConfigurationValidator` (`EnvironmentPostProcessor`, `META-INF/spring.factories`), `PreprodConfigurationFailureAnalyzer` (`FailureAnalyzer`) — FT-15 |
| `picking.PickingService` | `confirm`/`block`/`resume` gain one structured `INFO` log each (SLF4J 2.x `log.atInfo().addKeyValue(...)` fluent API) |
| `admin.StockAdminService`, `admin.OrderAdminService` | Same, for `adjust`/`createOrder` |
| `api.GlobalExceptionHandler` | `handleProblem` gains one centralized structured `WARN` log for **every** `ProblemException` app-wide (`problemCode`, `detail`, `extensions`) |
| `pom.xml` | Removed `net.logstash.logback:logstash-logback-encoder` — see Deviations |
| `docs/runbook-windows.md`, `docs/log-analysis-guide.md`, `docs/incident-record-template.md`, `docs/executed-test-report.md` | New Phase 9 documentation deliverables |
| tests | `PreprodConfigurationValidatorIT` (FT-15), `StructuredLoggingApiIT` (FT-16) |

### Baseline assessment confirmed correct

The Phase 9 plan's baseline assessment (already recorded in `PLAN.md`) held
up under implementation: `dev`/`preprod` profiles, the functional test
specification, `CorrelationIdFilter`, and `ci.yml` needed no rebuilding —
only the two genuine gaps (FT-15 startup validation, FT-16 logging coverage)
and the documentation deliverables required new work.

### Design decisions (recorded)

- **Startup validation throws; a `FailureAnalyzer` renders the diagnostic.**
  `PreprodConfigurationValidator` deliberately does not log-and-exit itself
  (logging may not be initialized yet at `EnvironmentPostProcessor` time);
  it throws `PreprodConfigurationException`, and
  `PreprodConfigurationFailureAnalyzer` (confirmed against the actual
  `AbstractFailureAnalyzer`/`FailureAnalysis` API in the Spring Boot 4.0.7
  sources) renders Boot's own boxed `APPLICATION FAILED TO START`
  description/action report — no raw stack trace, no property value, ever
  printed.
- **Registered via `META-INF/spring.factories`, not a `*.imports` file.**
  Confirmed against the Spring Boot 4.0.7 sources: `EnvironmentPostProcessor`
  and `FailureAnalyzer` still use the legacy `spring.factories` mechanism in
  this release (`org.springframework.boot.env.EnvironmentPostProcessor` is
  `@Deprecated(since = "4.0.0", forRemoval = "4.2.0")` in favor of
  `org.springframework.boot.EnvironmentPostProcessor`, which is what is
  actually registered) — verified by inspecting the framework jar directly
  rather than assumed from habit.
- **One centralized business-rule-violation log, not one per call site.**
  Rather than adding a log line to every `throw new ProblemException(...)`
  across the codebase, `GlobalExceptionHandler.handleProblem` — the single
  place every `ProblemException` already passes through — logs once. This
  automatically covers every existing and future `ProblemCode` (including
  ones Phase 9 did not touch, e.g. `WRONG_LOCATION`/`WRONG_ARTICLE`'s
  existing `extensions` payload of `taskId`/`expectedLocationCode`/
  `scannedQrValue`) and is the concrete mechanism that closes the Phase 9
  acceptance-gate item "wrong-scan and stock-integrity incidents can be
  diagnosed from logs... without a debugger."

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-14T10:16:20-06:00, total time 2:04 min |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `PreprodConfigurationValidatorIT` (FT-15) | Tests run 2, failures 0, errors 0 |
| `StructuredLoggingApiIT` (FT-16) | Tests run 1, failures 0, errors 0 |
| `OrderAllocationApiIT`, `OrderLifecycleApiIT`, `TaskRecoveryApiIT` | Tests run 1 each, failures 0, errors 0 |
| `AuthApiIT` | Tests run 11, failures 0, errors 0 |
| `FlywayMigrationIT` | Tests run 5, failures 0, errors 0 |
| `PersistenceLayerIT` | Tests run 5, failures 0, errors 0 |
| `PickingApiIT` | Tests run 1, failures 0, errors 0 |
| `PickingNegativePathApiIT` | Tests run 2, failures 0, errors 0 |
| `LabelApiIT`, `DashboardApiIT` (Phase 8, re-run for regression) | Tests run 1 each, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` (32 tests total) |

`PreprodConfigurationValidatorIT` proves, end to end, by actually calling
`SpringApplication.run(...)` with the `preprod` profile and capturing real
console output (`CapturedOutput`): a missing `WMS_DB_URL` fails fast with
exactly `"Missing required preprod configuration: WMS_DB_URL. Set these
environment variables before starting the preprod profile."` and a pointer
to `docs/configuration-matrix.md`, with no secret value printed; reusing the
committed dev password fails fast with a distinct clean message, and the
offending password value itself never appears in the captured output.

`StructuredLoggingApiIT` proves: a real `POST /admin/stock/adjustments`
produces a JSON log line with separate `articleSku`, `locationCode`,
`quantityDelta`, `resultingQuantity`, `adminUserId`, and `correlationId`
fields (not just baked into the message text); a subsequent
`NEGATIVE_RESULTING_STOCK` rejection produces a `business rule violation`
JSON log line with a `problemCode` field; neither the bearer token nor the
login password appears anywhere in the captured output.

## Deviations and notes

- **A real, previously-undetected gap was found and fixed: the pinned
  `logstash-logback-encoder` dependency (added Phase 7 Step 0) was dead
  weight — and my first attempt at this step's logging change silently
  produced no structured fields at all.** This application's structured
  JSON console output has always come from Spring Boot's own **native**
  structured-logging feature (`logging.structured.format.console: logstash`,
  `org.springframework.boot.logging.structured`/`logback.LogstashStructuredLogFormatter`
  in the framework itself), which does not require or use the third-party
  `net.logstash.logback` library at all — that library only takes effect
  through its own `LogstashEncoder` wired in a `logback-spring.xml`, which
  this project has never had. My first implementation used
  `net.logstash.logback.argument.StructuredArguments.kv(...)`, which
  compiled cleanly and even rendered readably in the log *message* text
  (because `StructuredArgument.toString()` returns `"key=value"`, so plain
  SLF4J parameter substitution alone made it *look* right), but produced
  **no separate JSON fields** — confirmed by inspecting `StructuredLoggingApiIT`'s
  actual captured JSON output line by line, not assumed from the message
  text looking correct. Inspecting Boot's own
  `LogstashStructuredLogFormatter` source confirmed the real mechanism:
  it flattens `ILoggingEvent.getMDCPropertyMap()` and
  `ILoggingEvent.getKeyValuePairs()` (SLF4J 2.x's `addKeyValue` fluent API)
  into top-level JSON members — nothing else. Rewrote every call site to
  `log.atInfo().addKeyValue(...).log(...)` and removed the now-confirmed-unused
  dependency from `pom.xml` entirely, rather than leaving misleading dead
  weight pinned in the build. Recorded per the failed-experiment rule; the
  Phase 7 Step 0 evidence record's claim of "structured JSON logging" was
  correct in outcome but attributed the mechanism to the wrong library.
- No other deviations from established patterns (Testcontainers per class,
  `RestTemplate` + JDK `HttpClient` factory, Jackson 3 `tools.jackson`).

## Residual risk

- **CI workflow (`.github/workflows/ci.yml`) was reviewed, not changed.**
  It already runs `mvn verify` (Checkstyle, SpotBugs, every Testcontainers
  IT) on `actions/setup-java` with Maven caching; `ubuntu-latest` ships
  Docker preinstalled, so Testcontainers needs no extra CI setup. No push
  was made to trigger a live Actions run (a visible-to-others action outside
  this session's scope); the closest available evidence is the identical
  `mvn verify` command run locally above. A real CI run remains a
  recommended follow-up for the owner.
- **No embedded build/configuration identifier per log line** — recorded
  explicitly as a known limitation in `docs/log-analysis-guide.md`, with a
  concrete follow-up suggestion, rather than silently left out.
- The runbook (`docs/runbook-windows.md`) has not been rehearsed end-to-end on a
  genuinely clean Windows environment in this session (this workstation
  already has the full toolchain installed); Section 7 of the runbook asks
  for that rehearsal's own evidence when it happens.
- FT-19 (scope/exclusion review) is intentionally still Blocked —
  `docs/executed-test-report.md` records this as a scheduling decision, to
  be executed once Phase 10 exists so the review covers Phase 10's surface
  too, not a defect in Phase 9.
