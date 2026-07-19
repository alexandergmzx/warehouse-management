# MFC mission telegrams

**Status: TRANSPORT implemented and evidenced; SORT specified and stubbed
(`PLAN.md`, MFC work package).**
Contract version: `v1.0.0` (semantic versioning; consumers pin a version per
`../ECOSYSTEM.md`'s contract rules)
Direction: this repository (warehouse-management, the WMS) → `agv-fleet-
controller` (the WCS) for the mission telegram; confirmations return via this
repository's `/api/v1` REST surface, per `../ECOSYSTEM.md`'s "confirmations
return via REST" description.
Media type: `application/json`
Timestamps: UTC ISO-8601 strings
Transport: transactional outbox + HTTP push (ADR 0011) — not the raw TCP
telegram socket the confirmed workflow baseline excludes.

Ecosystem contract rules apply: versioned, consumers pin, integration
friction is a contract defect first, and the example payloads below
(`src/test/resources/telegrams/`) double as test fixtures —
`TelegramFixturesIT` asserts every one of them parses.

## Mission lifecycle

```
PENDING --dispatch success--> DISPATCHED --WCS accepts--> ACCEPTED --WCS completes--> COMPLETED
   |                              |                            |
   +--dispatch exhausted--------> FAILED <--WCS reports failure-+
```

Allowed transitions: `PENDING → DISPATCHED`, `PENDING → FAILED` (dispatch
retry exhaustion, WMS-side), `DISPATCHED → ACCEPTED`, `DISPATCHED → FAILED`,
`ACCEPTED → COMPLETED`, `ACCEPTED → FAILED`. Any other requested transition
is rejected (`409 INVALID_MISSION_STATE`). A confirmation naming the mission's
*current* state is not a transition — it is treated as an idempotent replay
(`200`, `replayed: true`), matching the `/api/v1` HHT surface's own replay-
safety convention (API.md).

## TRANSPORT — tote, source location → destination location

Fully specified and implemented. Emitted automatically, exactly once, when a
customer order completes (`OrderCompletionPublisher`, ADR 0007), behind the
`telegram` adapter (ADR 0011). `sourceLocationCode`/`destinationLocationCode`
are the two configured handover points (`wms.mfc.transport.source-location`
/ `.destination-location`, `docs/configuration-matrix.md`) — this PoC models
one fixed staging point and one fixed handover station rather than deriving
a per-line source location, a deliberate proportionality simplification
recorded here rather than left implicit.

### Outbound telegram (WMS → WCS)

`POST {wms.mfc.telegram.base-url}/missions`

```json
{
  "missionId": 1042,
  "eventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "missionType": "TRANSPORT",
  "orderId": 501,
  "orderNumber": "ORD-2026-0501",
  "sourceLocationCode": "MFC-90-01",
  "destinationLocationCode": "MFC-90-02",
  "state": "DISPATCHED",
  "dispatchedAt": "2026-07-19T14:32:07Z"
}
```

Fixture: `src/test/resources/telegrams/transport-mission.json`.

`eventId` is the idempotency key (ADR 0007/0011): a WCS that receives the
same `eventId` twice (an at-least-once redelivery from a retried dispatch)
must treat the second delivery as the same mission, not a new one.

### Confirmation (WCS → WMS)

`POST /api/v1/mfc/missions/{missionId}/confirmations` — bearer-authenticated
(`ROLE_WCS`), same conventions as the rest of `/api/v1` (RFC 9457 errors,
`X-Correlation-Id`).

Request:

```json
{
  "state": "ACCEPTED",
  "occurredAt": "2026-07-19T14:32:41Z"
}
```

`state` must be `ACCEPTED`, `COMPLETED`, or `FAILED`; `reason` is required
when `state` is `FAILED` and otherwise optional.

Fixtures: `src/test/resources/telegrams/confirmation-accepted.json`,
`src/test/resources/telegrams/confirmation-completed.json`,
`src/test/resources/telegrams/confirmation-failed.json`.

Response (`200`):

```json
{
  "missionId": 1042,
  "state": "ACCEPTED",
  "replayed": false
}
```

An illegal transition (e.g. `COMPLETED` requested from `PENDING`) returns
`409` with `code: INVALID_MISSION_STATE`. An unknown `missionId` returns
`404` with `code: MISSION_NOT_FOUND`.

## SORT — parcel, induction station → chute

**Specified, not implemented (stubbed).** Reserved for the sortation
expansion phase (`../ECOSYSTEM.md` sequencing step 9). The outbound telegram
shape mirrors TRANSPORT with `missionType: "SORT"`, an induction-station
source and a chute destination in place of the tote handover points; no
sender emits it in this version, and any confirmation naming a `SORT`
mission returns `501` with `code: SORT_NOT_IMPLEMENTED` — a visible, honest
stub rather than a silent 404 or a route that doesn't exist.

## Open question: WCS-originated missions

`../ECOSYSTEM.md` v3's bin-full handling has the WCS itself spawn a
TRANSPORT mission (to swap a full bin) rather than the WMS. This version of
the contract does **not** support that: every mission in `v1.0.0` is
WMS-issued (`TelegramOrderCompletionPublisher`, ADR 0011); there is no
endpoint for the WCS to register a new mission it originated. This is a
reserved gap, not an oversight — closing it (a new endpoint, or a different
model where the WCS calls back into a "create mission" API) is future scope
for the sortation expansion phase, when bin-full handling is actually
implemented, and must be a version bump when it lands.

## Mission states reference

| State | Meaning | Set by |
|---|---|---|
| `PENDING` | Mission row written in the same transaction as order completion; not yet sent | `TelegramOrderCompletionPublisher` |
| `DISPATCHED` | Telegram POSTed to the WCS and accepted (2xx) at the transport level | `MissionDispatcher` |
| `ACCEPTED` | WCS has accepted the mission for execution | WCS confirmation |
| `COMPLETED` | WCS has finished executing the mission | WCS confirmation |
| `FAILED` | Dispatch retries exhausted, or the WCS reported failure | `MissionDispatcher` or WCS confirmation |
