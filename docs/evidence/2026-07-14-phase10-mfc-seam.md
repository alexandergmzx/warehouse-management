# Phase 10 evidence — MFC extension seam and FT-19 scope closure

**Build/configuration identifier:** `a845a74+phase10 / 2026-07-14T10:56:08-06:00`
(git HEAD `a845a74` — the committed Phase 9 baseline — plus the Phase 10
working tree; the identifier becomes immutable when this change is
committed.)

## Scope

Objective evidence for Phase 10 (`PLAN.md`): the `OrderCompletionPublisher`
application port, its immutable message, a configuration-selected no-op
adapter, a test fake proving one publication per completed order, documented
(not implemented) future TCP/serialization/timeout/retry/observability
boundaries, and the FT-19 scope-exclusion review this phase was gating
(`docs/executed-test-report.md`).

## What was added

| Area | Artifacts |
|---|---|
| `orders` | `OrderCompletionEvent` (record: `eventId`, `orderId`, `orderNumber`, `completedAt`, per ADR 0007), `OrderCompletionPublisher` (application port) |
| `mfc` (new package) | `NoopOrderCompletionPublisher` — the only adapter, selected by `wms.mfc.adapter=noop` (`@ConditionalOnProperty`, `matchIfMissing = true`); logs one structured line, does nothing else |
| `picking.PickingService` | `confirm()` calls `orderCompletionPublisher.publish(...)` exactly where `CustomerOrder` transitions to `COMPLETED` |
| `application.yml` | `wms.mfc.adapter: ${WMS_MFC_ADAPTER:noop}` |
| `docs/configuration-matrix.md` | `WMS_MFC_ADAPTER` row |
| `docs/architecture.md` | "MFC extension seam" section expanded: implementation status plus the documented (not implemented) serialization/timeout/result/retry/transaction-boundary/observability boundaries a future TCP adapter would own |
| `docs/decisions/0007-…md` | Implementation note recording the exact classes/config toggle and confirming, by code inspection, that order-domain code imports nothing beyond the port and the event |
| tests | `orders.FakeOrderCompletionPublisher` (test fake), `OrderCompletionSeamApiIT` |

### Design decisions (recorded)

- **The port and event live in `orders`; the adapter lives in a new `mfc`
  package.** This matches `docs/architecture.md`'s own pre-existing module
  table (`mfc — Future outbound completion adapter — orders completion port
  only`), written before this phase started implementation, and is the
  standard ports-and-adapters split: the domain (`orders`) owns the
  interface it depends on; the swappable implementation detail (`mfc`) owns
  the adapter and its own dependencies (today: none beyond SLF4J).
- **`publish()` is called synchronously, inside the same transaction, at the
  exact point of completion** — the simplest correct wiring for a no-op
  adapter. `docs/architecture.md` documents (does not solve) the two real
  options a future network-calling adapter would face: move the call
  post-commit, or adopt a transactional-outbox pattern.
- **The test fake replaces the adapter via `@TestConfiguration`/`@Primary`**,
  not Mockito — consistent with this project's established preference for
  fakes over mocks for this kind of substitution, and directly satisfies the
  acceptance gate's literal wording ("a test fake observes one completion
  publication").
- **FT-19 was executed now, not left Blocked**, since Phase 10 — the
  scope-review's own precondition per `PLAN.md` — completed the same day.
  The review is a `grep`-based code inspection (see Results), not a new
  automated test: there is nothing to unit-test about the *absence* of a
  skip endpoint, a scheduler, a socket, or a retry library — inspecting the
  actual source is the correct and sufficient method, and is retained here
  as evidence rather than asserted without citation.

## Toolchain and runtime

| Item | Observed value |
|---|---|
| Command | `mvn -B verify` |
| Maven / JDK | 3.9.16 / Eclipse Temurin 21.0.11 |
| Operating system | Windows 11, amd64 |
| Docker engine | Docker Desktop, server 29.6.1 |
| PostgreSQL image | `postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193` |
| Finished | 2026-07-14T10:56:08-06:00, total time 1:44 min |

## Results

| Gate | Result |
|---|---|
| Compilation and packaging | Success |
| `OrderCompletionSeamApiIT` (Phase 10 acceptance gate) | Tests run 1, failures 0, errors 0 |
| Full regression (all other IT classes, re-run) | Tests run 32, failures 0, errors 0 |
| Checkstyle | 0 violations |
| SpotBugs (`effort=Max`, `threshold=Low`) | 0 bugs, 0 errors |
| Overall | `BUILD SUCCESS` (33 tests total) |

`OrderCompletionSeamApiIT` proves, end to end, against a real Testcontainers
database: a full claim → scan-location → scan-article → confirm flow that
completes a single-line, single-task order results in **exactly one**
`OrderCompletionEvent` captured by the fake, with a non-null `eventId`, the
correct `orderNumber`, and a non-null `completedAt`; the fixture setup itself
(article/location/stock/order creation, login) publishes zero events,
confirming the publish call is scoped to actual order completion, not fired
spuriously elsewhere.

**FT-19 code-inspection results** (see `docs/executed-test-report.md` for the
full citation):

| Excluded feature | Check | Result |
|---|---|---|
| HHT skip operation | `grep -rni skip` across `picking`/`admin` | Only an unrelated SQL `FOR UPDATE ... SKIP LOCKED` clause and `TaskStatus`'s own "no skip" doc comment; no skip endpoint or service method |
| Partial pick | Inspect `PickingService.confirm()` | Still requires `quantity == task.getRequestedQuantity()` exactly; `QUANTITY_MISMATCH` otherwise (unchanged since Phase 7) |
| Direct HHT database access | Inspect `API.md`/`PickingController` | The HHT surface is `/api/v1/hht/**` REST only; no DB credentials or JDBC path is exposed to it |
| Scheduler | `grep -rn "@Scheduled\|ScheduledExecutorService\|TaskScheduler\|@EnableScheduling"` across `src/main/java` | Zero matches |
| Raw TCP socket | `grep -rn "java.net.Socket\|ServerSocket\|new Socket("` across `src/main/java` | Zero matches |
| Transport retry | `grep -rn "spring-retry\|resilience4j\|@Retryable\|RetryTemplate"` across `src/main/java` and `pom.xml` | Zero matches |

## Deviations and notes

No deviations from the established patterns (Testcontainers per class,
`RestTemplate` + JDK `HttpClient` factory, Jackson 3 `tools.jackson`,
`@TestConfiguration`/`@Primary` bean substitution).

## Residual risk

- The five boundaries `docs/architecture.md` documents for a future TCP
  adapter (serialization, timeout, delivery result, retry ownership,
  transaction boundary, observability) are, by design, not implemented or
  tested here — Phase 10's explicit scope is the seam only. A real MFC
  integration remains future work with its own design/ADR.
- `publish()`'s `void` return gives the caller no delivery-outcome signal;
  `docs/architecture.md` flags that a future adapter needing to report
  outcome back to the order domain requires a considered port-contract
  change, not a silent addition.

## Final acceptance status

With this phase, all of FT-01–FT-19 are Passed (`docs/executed-test-report.md`)
and every phase through 10 is implemented and evidenced. The `PLAN.md`
"Final acceptance" checklist items this discharges: the approved automated
verification suite passes on pinned tool versions; the numbered functional
test suite is executed and reported; SQL, logs, reports, screenshots, and
label evidence are retained under `docs/evidence/`. The one item not
completed in this session is a fresh-machine/clean-environment runbook
rehearsal (`docs/runbook-windows.md` Section 7) — recorded as a residual item in the
Phase 9 evidence and still open here.
