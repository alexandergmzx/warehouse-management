# Log-analysis guide

**Status:** Phase 9 deliverable. Structured JSON logs are the request/event
diagnostic layer; `docs/sql-diagnostics.md` is the corresponding
database-state layer. An incident is normally diagnosed by correlating both
(`docs/incident-record-template.md`), not from one source alone.

## Where the logs come from

Every profile emits one-line structured JSON to stdout
(`logging.structured.format.console: logstash` in `application.yml`) via
Spring Boot's **native** structured-logging support
(`org.springframework.boot.logging.structured`) — no third-party encoder or
`logback-spring.xml` is involved. Two field sources land in the JSON, both
automatically, with no manual wiring per call site:

- **MDC** (`org.slf4j.MDC`) — currently just `correlationId`, set for the
  whole request lifetime by `CorrelationIdFilter`.
- **SLF4J 2.x fluent key-values** — `log.atInfo().addKeyValue("k", v)....log("message")`,
  used at each business-event call site below. (`net.logstash.logback.argument.StructuredArguments`
  is a different, unrelated mechanism for logback's own third-party encoder;
  it does **not** work with Boot's native formatter, and was removed from
  this project's dependencies after Phase 9 confirmed it was unused —
  attempting to reintroduce it here will silently produce no separate JSON
  fields.)

Every log line always carries: `@timestamp` (UTC-offset ISO-8601),
`message`, `logger_name`, `thread_name`, `level`, `level_value`, and
(`logging.include-application-name: true`) the application name. Exceptions
add `stack_trace`.

## Field reference by event

| `message` | Logger | Level | Fields | When |
|---|---|---|---|---|
| `login succeeded` | `auth.AuthenticationService` | INFO | `username`, `deviceCode` (in the message text, not separate fields) | Successful `/api/v1/auth/login` |
| `login rejected` | `auth.AuthenticationService` | WARN | `username`, `deviceCode`, `code` (message text) | Failed login |
| `pick confirmed` | `picking.PickingService` | INFO | `orderNumber`, `taskNumber`, `articleSku`, `locationCode`, `quantity`, `userId`, `deviceId`, `durationMs` | Successful `POST /hht/tasks/{id}/confirm` (FT-16 positive case) |
| `task assignment conflict` | `picking.PickingService` | WARN | `claimedTaskId`, `userId`, `deviceId` (message text) | Concurrent claim race on `GET /hht/tasks/next` |
| `task blocked` | `picking.PickingService` | INFO | `taskId`, `taskNumber`, `adminUserId`, `reason` | `POST /admin/tasks/{id}/block` |
| `task resumed` | `picking.PickingService` | INFO | `taskId`, `taskNumber`, `adminUserId` | `POST /admin/tasks/{id}/resume` |
| `stock adjusted` | `admin.StockAdminService` | INFO | `articleSku`, `locationCode`, `quantityDelta`, `resultingQuantity`, `adminUserId` | `POST /admin/stock/adjustments` |
| `order created` | `admin.OrderAdminService` | INFO | `orderNumber`, `lineCount`, `taskCount`, `adminUserId` | `POST /admin/orders` |
| `order completion published (no-op adapter)` | `mfc.NoopOrderCompletionPublisher` | INFO | `eventId`, `orderNumber` | Order completed under the default `noop` adapter (ADR 0007) |
| `MFC TRANSPORT mission queued for dispatch` | `mfc.TelegramOrderCompletionPublisher` | INFO | `eventId`, `orderNumber` | Order completed under the `telegram` adapter (ADR 0011): the outbox row was written in the completing transaction |
| `MFC mission dispatched` | `mfc.MissionDispatcher` | INFO | `missionId`, `eventId`, `attempts` | Telegram POSTed to the WCS and acknowledged 2xx |
| `MFC mission dispatch failed; retry scheduled` | `mfc.MissionDispatcher` | INFO | `missionId`, `attempts`, `error` | One failed delivery attempt; mission stays `PENDING` with `next_attempt_at` set |
| `MFC mission dispatch exhausted; marked FAILED` | `mfc.MissionDispatcher` | WARN | `missionId`, `eventId`, `attempts`, `error` | `WMS_MFC_TELEGRAM_MAX_ATTEMPTS` reached — the operator-attention signal for a dead WCS link |
| `MFC mission confirmed` | `mfc.MissionConfirmationService` | INFO | `missionId`, `previousState`, `newState`, `wcsOccurredAt` | WCS confirmation applied via `POST /api/v1/mfc/missions/{id}/confirmations` (idempotent replays do **not** log this line) |
| `business rule violation` | `api.GlobalExceptionHandler` | WARN | `problemCode`, `detail`, `extensions` (nested object; carries `taskId`/`expectedLocationCode`/`scannedQrValue` for `WRONG_LOCATION`/`WRONG_ARTICLE`, empty for most other codes) | **Every** `ProblemException` app-wide — this is the single log point for wrong-scan and business-rule incidents (Phase 9 acceptance gate) |
| `Unhandled exception` | `api.GlobalExceptionHandler` | ERROR | `stack_trace` | A genuine bug (`500 INTERNAL_ERROR`), not a business-rule rejection |
| Preprod startup failure | n/a (Boot's own failure-analysis banner) | ERROR | `Description`/`Action` text only | FT-15 — see below |

All fields are allow-listed at the call site (ADR 0006); none of the above
ever include a password, token hash, or bearer token value.

## Worked examples

Logs are one JSON object per line, so any line-oriented JSON tool works.
`docs/evidence/2026-07-14-phase9-*.md` records the exact commands used to
produce the evidence for FT-15/FT-16.

### PowerShell

```powershell
# Every business-rule violation, most recent first
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.message -eq 'business rule violation' } |
    Select-Object '@timestamp', problemCode, detail, correlationId |
    Sort-Object '@timestamp' -Descending

# Everything for one correlation ID (ties a wrong scan to its HTTP request)
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.correlationId -eq '<uuid-from-the-problem+json-response>' }

# Every confirmed pick for one order, with duration
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.message -eq 'pick confirmed' -and $_.orderNumber -eq 'DEMO-1001' } |
    Select-Object taskNumber, articleSku, locationCode, quantity, durationMs
```

### jq (cross-platform, if available)

```bash
jq 'select(.message == "business rule violation")' app.log
jq 'select(.correlationId == "<uuid>")' app.log
jq 'select(.message == "stock adjusted") | {articleSku, quantityDelta, resultingQuantity, adminUserId}' app.log

# Full MFC mission lifecycle for one mission (queue -> dispatch -> confirm),
# then cross-check database state with docs/sql-diagnostics.md §5
jq 'select(.missionId == 42 or (.message | startswith("MFC")))' app.log
```

## Diagnosing FT-15: a preprod instance that will not start

A missing/unsafe required variable produces Spring Boot's own boxed
`APPLICATION FAILED TO START` report — not a JSON log line, since it happens
before the datasource (and, depending on timing, the structured-logging
formatter) is created. Read the `Description`/`Action` text directly; it
names the missing variable, never a value:

```text
***************************
APPLICATION FAILED TO START
***************************

Description:

Missing required preprod configuration: WMS_DB_URL. Set these environment
variables before starting the preprod profile.

Action:

Review docs/configuration-matrix.md for the required WMS_DB_* variables and
their sensitivity, then set them before starting the preprod profile again.
```

## Diagnosing a wrong-scan or stock-integrity incident (no debugger)

1. From the operator/HHT report, get the approximate time and, if available,
   the `correlationId` from the `application/problem+json` response body
   (every error response includes one).
2. Filter logs for `business rule violation` around that time/correlation ID
   (examples above). `problemCode` tells you the exact rule
   (`WRONG_LOCATION`, `WRONG_ARTICLE`, `QUANTITY_MISMATCH`,
   `INSUFFICIENT_STOCK`, `CONFIRMATION_ID_REUSED`, …); `extensions` carries
   the task/expected/scanned values for the two scan-mismatch codes.
3. Cross-check current state and the append-only audit trail in SQL —
   `docs/sql-diagnostics.md` §1 (stuck tasks) and §3 (full order trace, which
   also surfaces each `task_transition.correlation_id` for cross-referencing
   back into the logs).
4. Never repair state with direct SQL; use the audited admin block/resume
   recovery endpoints (ADR 0004) and record the incident
   (`docs/incident-record-template.md`).

## Known limitation: no embedded build/configuration identifier per line

ADR 0006 asks for a build/configuration identifier on every log line. This
PoC does not embed one (no git-commit-id plugin or `MANIFEST.MF` version
stamping is wired up); correlate logs to a build/configuration by matching
`@timestamp` against the deployment window recorded in
`docs/evidence/`/the incident record instead. Recorded as a residual gap,
not silently dropped — worth adding (e.g., `git-commit-id-maven-plugin` plus
one more MDC entry in `CorrelationIdFilter`) if this moves beyond a PoC.
