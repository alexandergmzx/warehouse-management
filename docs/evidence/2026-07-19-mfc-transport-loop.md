# MFC transport loop evidence — consumer proof (PLAN.md gate 5)

**Build/configuration identifier:** git HEAD `cff8cde` (the committed MFC
implementation) plus one working-tree fix discovered during this run (see
"Defect found and fixed" below), applied before the evidence run reported
here. The fix becomes immutable when committed alongside this file.

## Scope

Gate 5 of the MFC work package (`PLAN.md`): "`agv-fleet-controller` (or,
until it exists, a scripted stand-in kept in that repo's name) consumes a
TRANSPORT telegram and returns a confirmation end-to-end." `agv-fleet-
controller` does not exist yet in the ecosystem sequencing
(`../ECOSYSTEM.md`), so `scripts/wcs-standin/wcs_standin.py` acts in its
name, exactly as ADR 0011 anticipated.

## Setup

- `docker compose up -d` — fresh `wms-postgres` (`postgres:17.10-alpine`,
  digest-pinned), empty volume.
- `./mvnw spring-boot:run` with:
  `WMS_MFC_ADAPTER=telegram`,
  `WMS_MFC_TELEGRAM_BASE_URL=http://localhost:8090`,
  `WMS_MFC_TELEGRAM_RETRY_INTERVAL=PT5S`, `WMS_MFC_TELEGRAM_MAX_ATTEMPTS=5`,
  `WMS_MFC_TRANSPORT_SOURCE_LOCATION=MFC-90-01`,
  `WMS_MFC_TRANSPORT_DESTINATION_LOCATION=MFC-90-02`.
- `python3 scripts/wcs-standin/wcs_standin.py` listening on `:8090`,
  confirming against `http://localhost:8080`, authenticating as the seeded
  `wcs01`/`AGV-FC-01` (`db/devdata/V2_1__seed_wcs_client.sql`).

## Defect found and fixed during this run

The first live attempt failed: the dispatcher logged "MFC mission queued for
dispatch" but the mission stayed `PENDING` forever, and the app log recorded
`org.springframework.dao.InvalidDataAccessApiUsageException: No active
transaction` from a background `scheduling-1` thread. Root cause: `@Scheduled
dispatchPending()` called `dispatchNextOnce()` as a plain same-class method
call — Spring AOP proxies only intercept calls that arrive *through* the
proxy, so that internal call bypassed the `@Transactional` boundary
entirely, and the pessimistic-lock query (`MfcMissionRepository
.findByIdForUpdate`, `@Lock(PESSIMISTIC_WRITE)`) failed outside any
transaction. The 43 integration tests passing beforehand never caught this:
every test called `dispatcher.dispatchNextOnce()` directly on the injected
proxy bean (an external call, correctly proxied) and never exercised the
`@Scheduled` entry point itself.

Fixed by self-injecting the proxy (`@Lazy MissionDispatcher self` in the
constructor) and routing the loop through it: `dispatchPending()` now calls
`self.dispatchNextOnce()`. A regression test,
`MfcTelegramLifecycleIT.scheduledDispatchLoopRunsEachMissionInATransaction`,
drives `dispatchPending()` itself (not `dispatchNextOnce()` directly) and
would have failed before this fix. Full suite re-run after the fix: 43/43,
0 Checkstyle, 0 SpotBugs — see the diff accompanying this evidence file.

This is exactly the kind of gap a real, running-process consumer-proof
exercise catches that a mocked/direct-call integration test cannot; it is
recorded here per the "record failed experiments" rule rather than silently
folded into the implementation commit.

## Clean end-to-end run (no manual state manipulation)

Order `MFC-DEMO-ORD-2` (article `MFC-DEMO-002`, location `A-01-02`, qty 3),
picked and confirmed by a fresh operator (`mfcdemo01`/`MFC-DEMO-DEV-01`) at
`2026-07-19T18:13:13Z`. With no manual intervention:

```
=== picker completes MFC-DEMO-ORD-2 ===
claimed: {"id":7,"state":"ASSIGNED","orderNumber":"MFC-DEMO-ORD-2", ...}
{"taskId":7,"state":"LOCATION_CONFIRMED", ...,"replayed":false}
{"taskId":7,"state":"ARTICLE_CONFIRMED", ...,"replayed":false}
{"taskId":7,"state":"COMPLETED","confirmedQuantity":3,"movementId":10,
 "remainingStock":0,"order":{"number":"MFC-DEMO-ORD-2","state":"COMPLETED"},
 "completedAt":"2026-07-19T18:13:13.410379591Z"}
```

Stand-in log, unprompted (next `@Scheduled` tick, ~4s later):

```
[wcs-standin] 2026-07-19T12:13:17-0600 received telegram: {'missionId': 2,
  'eventId': '0e73782b-1c47-4bac-a133-c27e30169cc3', 'missionType': 'TRANSPORT',
  'orderId': 5, 'orderNumber': 'MFC-DEMO-ORD-2', 'sourceLocationCode': 'MFC-90-01',
  'destinationLocationCode': 'MFC-90-02', 'state': 'DISPATCHED',
  'dispatchedAt': '2026-07-19T18:13:17.594005212Z'}
[wcs-standin] 2026-07-19T12:13:17-0600 confirmed mission=2 state=ACCEPTED
  -> 200 {'missionId': 2, 'state': 'ACCEPTED', 'replayed': False}
[wcs-standin] 2026-07-19T12:13:17-0600 mission=2 simulating transit (2.0s):
  MFC-90-01 -> MFC-90-02
[wcs-standin] 2026-07-19T12:13:19-0600 confirmed mission=2 state=COMPLETED
  -> 200 {'missionId': 2, 'state': 'COMPLETED', 'replayed': False}
```

### Idempotency and negative-path checks, against the same completed mission

| Request | Result |
|---|---|
| Replay `COMPLETED` on an already-`COMPLETED` mission | `200 {"missionId":2,"state":"COMPLETED","replayed":true}` |
| `ACCEPTED` requested after `COMPLETED` (illegal transition) | `409 INVALID_MISSION_STATE`, `"Cannot transition mission from COMPLETED to ACCEPTED."` |
| Confirmation with no bearer token | `401 INVALID_TOKEN` |

### SQL reconciliation

```
 id |  order_number  | mission_type |   state   | attempts |  source   | destination
----+----------------+--------------+-----------+----------+-----------+-------------
  1 | MFC-DEMO-ORD-1 | TRANSPORT    | COMPLETED |        1 | MFC-90-01 | MFC-90-02
  2 | MFC-DEMO-ORD-2 | TRANSPORT    | COMPLETED |        1 | MFC-90-01 | MFC-90-02

 mfc_mission_id | previous_state | new_state  |          occurred_at
----------------+----------------+------------+-------------------------------
              2 | PENDING        | DISPATCHED | 2026-07-19 18:13:17.594005+00
              2 | DISPATCHED     | ACCEPTED   | 2026-07-19 18:13:17.638706+00
              2 | ACCEPTED       | COMPLETED  | 2026-07-19 18:13:19.687496+00
```

`mfc_mission_transition` for mission 2 is complete and gapless (no skipped
or out-of-order states); one dispatch attempt, no retries needed against a
healthy stand-in. Stock/ledger reconciliation (the same diagnostic query
`docs/sql-diagnostics.md` uses) returns zero mismatches across the whole
database after both demo orders:

```
 mismatches
------------
          0
```

## Mission 1 (first attempt, includes the autonomous-recovery evidence)

`MFC-DEMO-ORD-1` was completed before the self-invocation defect above was
found and fixed, so its mission sat `PENDING` across an app restart. On
restart with the fix applied, the scheduler's very first tick picked up that
stale `PENDING` row **on its own** — no manual dispatch call — and drove it
to `COMPLETED` (`mfc_mission` id 1 above), which is itself useful evidence:
a mission queued while the dispatcher was unavailable is not lost; it is
picked up automatically the next time the scheduler runs.

## What this proves

- The telegram is delivered over real HTTP to an external process
  authenticating as `agv-fleet-controller`'s stand-in identity, not an
  in-process test double.
- The WCS confirms back over the real `/api/v1` surface, bearer-token
  authenticated as `ROLE_WCS`, and the mission's state and append-only
  transition ledger update exactly as `TELEGRAMS.md` specifies.
- Idempotent replay, illegal-transition rejection, and unauthenticated
  rejection all behave as specified against a live server, not just the
  Testcontainers suite.
- A mission queued before the consumer is reachable is recovered
  automatically once the dispatcher scheduler next runs.

## Residual

Stock adjustment device `HHT-DEV-01` reuse, dev article/location naming
(`MFC-DEMO-00x`, `MFC-DEMO-DEV-01`) are demo-only artifacts local to this
run; they are not part of the committed dev fixture set
(`db/devdata/V1_1`/`V1_2`/`V2_1`) and were created ad hoc via the admin API
and one-off SQL inserts for FIFO ordering control, the same technique
`docs/evidence/2026-07-15-hht-loopback-integration.md` and this repo's own
integration tests already use.
