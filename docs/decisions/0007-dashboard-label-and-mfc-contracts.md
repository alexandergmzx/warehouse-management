# ADR 0007: Dashboard, labels, and MFC extension contracts

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decisions: D-15, D-16

## Decision

Use exact case-sensitive QR payloads `LOC:<location-code>` and `ART:<sku>`.
Generate deterministic 300-by-300 PNGs with UTF-8 content, black-on-white output,
error correction M, and a four-module quiet zone. Generate deterministic A4 PDFs
with fixed geometry and an embedded, licence-reviewed redistributable font.

Use an authenticated, admin-only server-rendered dashboard with a configurable
two-second polling interval. Avoid a separate frontend build.

Define an `OrderCompletionPublisher` application port receiving an immutable
message with `eventId`, `orderId`, `orderNumber`, and `completedAt`. Use a
configuration-selected no-op adapter and a test fake. Do not implement sockets,
TCP telegrams, schedulers, queues, transport retries, or delivery ownership.

## Consequences

- Exact QR/PDF library, font, and licence evidence must be recorded before
  implementation.
- Order-domain code depends only on the publisher port and immutable message.
- Future MFC transport remains a separate decision and implementation.

