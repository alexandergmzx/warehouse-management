# Candidate backlog — limitations and industry-grounded extensions

**Status:** candidate list, nothing here is authorized. Per `CLAUDE.md`,
every item is new scope, maintenance, or an invariant change: it enters work
only by owner decision, with an ADR where architecture is touched, and the
confirmed workflow baseline can only be changed by the owner explicitly.
This document *records and prioritizes* — it does not approve.

Each item is tagged with its **origin**: `recorded` (already documented as a
limitation/risk somewhere in this repo), `inspection` (found by examining
the implementation, 2026-07-19), or `research` (grounded in the external
sources listed at the end; all accessed 2026-07-19). Industry claims are
cited; untagged rationale is project interpretation.

## Tier 0 — already-open acceptance items (authorized, evidence-shaped)

Not new scope; these are the pending closures of work already approved.

| Item | Origin | What closes it |
|---|---|---|
| HandheldPi Stage 3: physical GamePi20 over WiFi against the WMS host, runbook §3–4 firewall exercised | recorded (`handheld-plan.md`, HandheldPi `LAN_E2E_RUNBOOK.md`) | Owner-executed run with the device on the LAN; evidence in both repos |
| Manual set validation and publication: real screenshots, per-profile pilot sessions | recorded (RSK-21, RSK-22, `docs/manual/anexos/matriz-de-validacion-del-manual.md`) | Pilots per profile, corrections, versioned publication |
| Post-MFC HandheldPi loopback re-run — the frozen `hht-picker` against the V2-schema build, as fresh two-generations evidence | inspection | One loopback session + evidence entry (low effort; seed and v1 surface verified compatible 2026-07-19) |

## Tier 1 — recommended next candidates (small, close real gaps in shipped scope)

These harden the MFC package that just shipped; each is proportional to the
PoC and demonstrable. All need owner approval; none changes an invariant.

| Item | Origin | Rationale | Cost shape |
|---|---|---|---|
| **Stuck-mission detection.** A mission whose WCS accepted and went silent stays `DISPATCHED`/`ACCEPTED` forever; nothing flags it, unlike stuck tasks (`WMS_TASK_STUCK_THRESHOLD`) | inspection | Industry MFC protocols treat a missing acknowledgement within a configured interval as a fault and act on it (SAP EWM MFS: configurable ack timeout, repetition count, connection-considered-broken) — silence is an error state, not a wait state | New threshold parameter + diagnostic surfacing (SQL §5 extension, dashboard row, or log event); no schema change |
| **Admin mission operations.** Only `GET /admin/mfc/missions/{id}` exists: no list/filter by state, and a `FAILED` mission cannot be re-queued through an audited API — recovery is manual agreement with the WCS side | inspection | Mirrors the existing block/resume philosophy: recovery through audited endpoints, never SQL. `eventId` idempotency already makes re-dispatch safe | List endpoint + audited re-dispatch action + transition rows; no schema change |
| **Outbound telegram authentication.** The dispatcher POSTs telegrams unauthenticated; the WCS confirms *inbound* with a bearer token, but anyone on the LAN can impersonate the WMS toward the WCS | inspection | Asymmetric trust is a real misconfiguration risk (OWASP API8 Security Misconfiguration) | A configured shared token header on the outbound `RestClient`; contract note in `TELEGRAMS.md` (additive, v1.1) |
| **Login rate limiting / lockout.** `POST /auth/login` accepts unlimited attempts | inspection + research | OWASP API Security Top 10 2023: API4 Unrestricted Resource Consumption (replaced "Lack of Rate Limiting"); API2 Broken Authentication | Small filter + config parameters; tests for lockout/recovery |
| **MFC delivery metrics.** No counters/latency for dispatch success/failure — the architecture doc's observability boundary names this as owned-but-minimal | recorded (`docs/architecture.md`) | Operations can currently see individual log lines but not trends toward exhaustion | Micrometer counters via the already-present actuator; expose deliberately (keep the minimal-actuator rule) |

## Tier 2 — WMS domain extensions (industry-grounded, need a scope decision)

Real-WMS capabilities this PoC lacks entirely; ADR 0008 excluded most of
them deliberately. Ordered by fit with the project's demonstration goal
(SQL/ledger discipline, auditable workflows) rather than by industry size.

| Item | Origin | Rationale | Notes |
|---|---|---|---|
| **Cycle counting.** Rolling, audited counts against the append-only ledger; discrepancies become audited adjustments | research | Core WMS function in every industry reference (Made4net, Generix); rolling counts replace shutdown inventories | Best domain fit: it *showcases* the existing `stock_movement` ledger and reconciliation SQL; new count-document tables via migration |
| **Receiving/putaway flow.** Inbound today is an admin stock adjustment; there is no receipt document, directed putaway, or dock-to-location trace | research | Receiving → putaway → storage → picking → packing → shipping is the canonical WMS process spine (Made4net, Generix, Gestisoft) | Medium scope: receipt entity + putaway task type; reuses the task state machine pattern |
| **Replenishment (reserve → pick face).** | research + recorded (excluded, ADR 0008) | Standard trigger-based WMS function (Made4net) | Only meaningful after locations gain zone/role semantics — sequence after receiving/putaway |
| **Wave/batch picking, slotting, pick-path optimization** | research + recorded (excluded, ADR 0008) | Real but heavy; the FIFO single-task model is a deliberate, documented simplification | Likely stays out for the PoC — disproportionate to the job-application goal |
| **Packing/shipping stage, carrier integration, yard management** | research | Present in full WMS suites (Generix, Made4net) | Out of PoC proportion; the TRANSPORT mission to the handover station is this PoC's outbound boundary |

## Tier 3 — MFC/contract evolution (ecosystem-gated)

Anchored to `../ECOSYSTEM.md` sequencing; each lands with a `TELEGRAMS.md`
version bump and consumer coordination.

| Item | Origin | Gate |
|---|---|---|
| **SORT missions implemented** (induction → chute; today an honest `501` stub) | recorded (`TELEGRAMS.md`) | Ecosystem step 9 (sortation expansion, tilt-tray conversion) |
| **WCS-originated missions** (bin-full spawns a TRANSPORT to swap the bin) — the reserved v1 gap | recorded (`TELEGRAMS.md` open question) | Ecosystem step 9; needs a "create mission" path in the contract, decided by ADR |
| **Mission cancellation.** No way to cancel a dispatched mission; the vehicle-side standard the ecosystem mirrors (VDA 5050, v2.1.0 published January 2025) models cancellation as an instant action | inspection + research | Becomes real once `agv-fleet-controller` exists (ecosystem step 3); contract addition + state-machine extension |
| **Per-order transport source derivation.** Source/destination are two fixed configured handover points — a recorded proportionality simplification | recorded (`TELEGRAMS.md`) | Revisit when physical topology (multiple drop points) exists |
| **Outbox scale-up: polling publisher → transaction log tailing** (Debezium-class CDC) | research | Explicitly **not planned**: microservices.io documents log tailing as the low-latency successor to polling, at the cost of DB-specific tooling — disproportionate for a single-consumer PoC. Recorded so the trade-off is a known decision, not an unknown |

## Tier 4 — production-shaped hardening (recorded, not planned)

All already documented as limitations (`docs/manual/07-…` "Limitaciones",
risk register, `docs/log-analysis-guide.md`); grouped here so they are
visible as one deliberate bucket. Each would need a scope decision that the
PoC's job-application goal has so far not justified:

- production profile and deployment architecture (RSK-19); HTTPS/TLS
  termination; database TLS (RSK-14);
- service installation (systemd/Windows service), log retention/aggregation;
- build/configuration identifier embedded per log line
  (`docs/log-analysis-guide.md` residual);
- user/device administration API (accounts are migration-provisioned);
- stock-adjustment idempotency key (RSK-16) and adjustment-vs-open-
  reservation validation (RSK-15);
- HHT success-path correlation logging (RSK-20, HandheldPi side).

## Explicitly frozen — not backlog

These look like "missing features" but are confirmed workflow invariants or
standing exclusions; they change only by explicit owner decision, not by
backlog grooming: exact-quantity confirmation (no short picks), no HHT skip,
raw TCP telegram sockets, message brokers for MFC, direct HHT database
access, destructive reset as recovery, FEFO/lot/serial (ADR 0008 — revisit
only with a domain reason such as expiry-driven picking).

## Sources (accessed 2026-07-19)

Industry WMS scope:
[Made4net — What is a WMS](https://made4net.com/knowledge-center/what-is-a-warehouse-management-system-wms/) ·
[Generix — key WMS processes](https://www.generixgroup.com/en/blog/how-do-warehouse-management-systems-work-understanding-key-processes-in-warehouse-management) ·
[Gestisoft — WMS components](https://www.gestisoft.com/en/blog/components-of-warehouse-management-system)

WCS / material flow control:
[BEUMER Group — WCS glossary](https://www.beumergroup.com/knowledge/wd/glossary-warehouse-distribution/glossary-warehouse-control-system-wcs/) ·
[Conveyco — What is a WCS](https://www.conveyco.com/news/what-is-a-warehouse-control-system-wcs/) ·
[AutoStore — WCS guide](https://www.autostoresystem.com/insights/warehouse-control-systems-wcs-ultimate-guide)

MFC telegram protocol practice (ack/handshake, timeouts, repetitions):
[SAP Community — EWM MFS receiving and processing telegrams](https://community.sap.com/t5/supply-chain-management-blog-posts-by-sap/sap-ewm-mfs-receiving-and-processing-telegrams/ba-p/13467618) ·
[SAP Community — EWM MFS communication](https://community.sap.com/t5/supply-chain-management-blog-posts-by-sap/sap-ewm-mfs-communication/ba-p/13478232)

Security:
[OWASP API Security Top 10 — 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)

Vehicle interface standard (ecosystem context):
[VDA 5050 — publication page](https://www.vda.de/en/topics/automotive-industry/vda-5050) ·
[VDA5050 2.1.0 specification (GitHub)](https://github.com/VDA5050/VDA5050/blob/2.1.0/VDA5050_EN.md)

Outbox scale-up:
[microservices.io — Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html) ·
[microservices.io — Polling publisher](https://microservices.io/patterns/data/polling-publisher.html) ·
[microservices.io — Transaction log tailing](https://microservices.io/patterns/data/transaction-log-tailing.html)
