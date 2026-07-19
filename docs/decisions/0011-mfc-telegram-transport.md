# ADR 0011: MFC telegram transport

- Status: accepted
- Date: 2026-07-19
- Amends: `docs/decisions/0007-dashboard-label-and-mfc-contracts.md` (discharges
  its "future MFC transport remains a separate decision" consequence) and
  `docs/architecture.md`'s MFC extension seam section; does not change the
  `OrderCompletionPublisher` port contract itself.

## Context

`../ECOSYSTEM.md` v3 approved the MFC work package: implement the
`OrderCompletionPublisher` seam (ADR 0007) behind a real adapter that emits
mission telegrams `agv-fleet-controller` (the WCS) can dispatch, and accept
confirmations back over REST. `PLAN.md` records the acceptance gates. The
confirmed workflow rule "MFC integration is a future seam, not a TCP
implementation" excludes raw TCP telegram sockets from this repository. The
ecosystem's own rule — "not a message-broker showcase" — excludes standing up
a broker for a PoC-scale integration.

Four transports were compared:

1. **Raw TCP telegram socket.** Excluded outright by the confirmed workflow
   rule; not evaluated further.
2. **Message broker (e.g. RabbitMQ, Kafka).** Would give at-least-once
   delivery and decoupling for free, but adds an operated service, a new
   pinned dependency, and broker-specific ops/observability work
   disproportionate to a PoC integration with one consumer. Rejected per the
   "not a message-broker showcase" rule.
3. **WCS-pull REST.** The WCS polls a WMS "pending missions" endpoint instead
   of the WMS pushing. Avoids the WMS needing to know the WCS's network
   address, but inverts the natural direction of the existing seam (`publish`
   is WMS-initiated by design, ADR 0007) and still needs the same outbox
   table underneath to answer "what's pending" — it only relocates the
   dispatch loop to the consumer. No material simplification for a
   single-consumer PoC.
4. **Transactional outbox + HTTP push (chosen).** The mission row is written
   in the *same* `@Transactional` method that completes the order — the same
   place `publish()` is already called (`PickingService`, at the point
   `CustomerOrder` transitions to `COMPLETED`). Emission-exactly-once-per-
   commit and no-emission-on-rollback fall directly out of that transaction
   boundary; no separate outbox-relay infrastructure is needed beyond a
   background poller. A `@Scheduled` dispatcher claims `PENDING` rows (same
   `FOR UPDATE SKIP LOCKED` pattern this codebase already uses for FIFO task
   claiming) and pushes each as one HTTP POST via Spring's built-in
   `RestClient` — no new HTTP client dependency. Confirmations return via the
   existing `/api/v1` REST surface, consistent with `../ECOSYSTEM.md`'s
   "confirmations return via REST" description of the MFC contract.

## Decision

**Transactional outbox + HTTP push (option 4).** Concretely:

- `TelegramOrderCompletionPublisher` (the real `OrderCompletionPublisher`
  adapter, `wms.mfc.adapter=telegram`) inserts one `PENDING` `mfc_mission`
  row per completed order, pre-commit, exactly where `publish()` is already
  called. This resolves ADR 0007's "transaction boundary" open point in
  favor of the outbox pattern over post-commit dispatch: the small window
  a post-commit call would leave (commit succeeds, publish is never
  attempted) does not exist here, at the cost of the dispatcher needing its
  own retry/backoff loop — accepted, since that loop is needed for network
  delivery regardless of when the row is written.
- `MissionDispatcher` is a `@Scheduled` poller (`wms.mfc.telegram.retry-
  interval`), active only when `wms.mfc.adapter=telegram`. It claims
  `PENDING` (and due-for-retry) rows, POSTs the telegram JSON
  (`TELEGRAMS.md`) to `wms.mfc.telegram.base-url`, and on a 2xx response
  transitions the mission to `DISPATCHED`. On failure it increments
  `attempts`, schedules `next_attempt_at` with a fixed backoff, and — after
  `wms.mfc.telegram.max-attempts` — marks the mission `FAILED` with the last
  error recorded. `eventId` (already the port's idempotency key, ADR 0007)
  travels in the telegram body so a WCS-side at-least-once redelivery is
  detectable there too.
- The `OrderCompletionPublisher` port itself is unchanged: still
  `void publish(OrderCompletionEvent event)`. Delivery outcome is tracked on
  the `mfc_mission` row, not surfaced back through the port — order-domain
  code still depends on nothing beyond the port and the event record.
- Both `TelegramOrderCompletionPublisher` and `MissionDispatcher` validate
  their required configuration (`wms.mfc.telegram.base-url` for the
  dispatcher; the configured transport location codes for the sender) at
  bean construction and throw if missing — this fails the same way in any
  profile that activates the adapter, which subsumes the "preprod refuses to
  start on missing config" pattern `PreprodConfigurationValidator` uses for
  database variables, without duplicating that pre-bean-creation mechanism
  for a concern that isn't needed before the datasource exists.

## Consequences

- No new runtime dependency: Spring Boot 4's built-in `RestClient` and
  `@Scheduled` cover the whole dispatcher; `@ConditionalOnProperty` keeps the
  `noop` adapter's zero-dependency footprint for anyone who never sets
  `wms.mfc.adapter=telegram`.
- `mfc_mission` and its append-only `mfc_mission_transition` ledger are new
  schema, added in `V2__create_mfc_missions.sql` (Flyway; `V1` stays
  untouched per the immutable-migration rule) — see `TELEGRAMS.md` for the
  wire contract these rows back.
- The WCS confirming missions needs to authenticate against this API. Rather
  than a bespoke auth mechanism, it reuses the existing bearer-token scheme
  (ADR 0005) under a new `WCS` role — a small, additive schema change (the
  `app_user.role` check constraint gains a value in `V2`; `V1`'s original
  constraint is not edited) rather than a parallel authentication path.
- Retry/backoff is fixed-interval, not exponential, and capped at a small
  attempt count — proportionate to a PoC with one consumer and a bench-scale
  demo loop, not a production delivery guarantee. Recorded here rather than
  silently assumed.
- A real `agv-fleet-controller` does not exist yet in this ecosystem
  sequencing step; gate 5 (`PLAN.md`) is proved against a scripted stand-in
  kept in this repository's `scripts/wcs-standin/`, acting in that repo's
  name until it exists.

Sources (accessed 2026-07-19): [Spring Framework `RestClient` reference](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient);
[Spring `@Scheduled` reference](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled);
PostgreSQL `FOR UPDATE ... SKIP LOCKED` documentation (already cited in this
repo's ADR 0003 for the FIFO claim pattern reused here).
