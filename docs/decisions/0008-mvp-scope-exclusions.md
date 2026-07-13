# ADR 0008: MVP scope and exclusions

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decision: D-17

## Decision

The MVP demonstrates relational SQL, Java/Spring services, configuration,
functional testing, diagnostics, logging, and operational runbooks through the
small workflow defined in the Phase 3 MVP recommendation.

Explicitly exclude wave planning, route optimization, partial or short picks,
HHT skip, automatic timeout release, FEFO, lot/serial handling, replenishment,
a separate SPA/mobile application, Kotlin, cloud deployment, a log aggregation
platform, direct HHT database access, robot control, sockets, TCP telegram
delivery, schedulers, transport retries, and destructive database reset as
rollback or incident recovery.

## Consequences

- The HHT remains a separate REST client.
- Administrative block/resume is the only initial recovery path for blocked work.
- Additional capability requires a new scope or architecture decision rather
  than implicit expansion of the MVP.

