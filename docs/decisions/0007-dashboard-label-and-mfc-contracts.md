# ADR 0007: Dashboard, labels, and MFC extension contracts

- Status: accepted for design; Phase 8 (dashboard/labels) and Phase 10 (MFC
  seam) both implemented and evidenced
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

## Implementation refinement (2026-07-13): library, font, and determinism selection

This discharges the "must be recorded before implementation" consequence below.

- **QR encoding:** `com.google.zxing:core` and `:javase` **3.5.4** (Apache-2.0,
  current Maven Central release). ZXing is in maintenance mode — contributed
  bug fixes only, no feature roadmap — which is acceptable for a frozen
  encoding spec; Phase 2 already compared it against QRGen and other barcode
  libraries (`docs/research/phase-2-research.md`, JAVA-10) and it remained the
  direct, unwrapped choice.
- **PDF generation:** `org.apache.pdfbox:pdfbox` **3.0.8** (Apache-2.0,
  current Maven Central release), confirming the Phase 2 selection over
  iText (AGPL) and OpenPDF.
- **Embedded font:** Liberation Sans Regular, SIL Open Font License 1.1,
  vendored at `src/main/resources/fonts/liberation-sans/` with its `LICENSE`
  and `AUTHORS` files (copyright Red Hat, Inc. 2012, Reserved Font Name
  "Liberation"; retrieved 2026-07-13 from the LibrePDF/OpenPDF project, which
  redistributes the same OFL-licensed binaries:
  <https://github.com/LibrePDF/OpenPDF/tree/master/openpdf-fonts-extra/src/main/resources/liberation>).
  The OFL permits embedding and redistribution with software provided the
  copyright notice and license text travel with each copy (satisfied by
  vendoring `LICENSE`/`AUTHORS` alongside the `.ttf`); load it into PDFBox via
  `PDType0Font.load(document, fontFile)` for embedded-subset text.
- **Determinism.** PNG: ZXing's `BitMatrix` from fixed input plus `ImageIO`
  PNG encoding carries no embedded timestamp, so output bytes are stable;
  proved by asserting byte-equality across two generations in the FT-17 test,
  not assumed. PDF: the two non-deterministic fields are the info-dictionary
  dates and the trailer document ID — the same fields the reproducible-builds
  ecosystem normalizes via `SOURCE_DATE_EPOCH`
  (<https://www.tug.org/pipermail/pdftex/2015-July/008955.html>) — fixed by
  setting `PDDocumentInformation` creation/modification dates to a constant
  and seeding the trailer ID via `COSDocument#setDocumentId`, then asserting
  byte-equality across repeated generation.

Sources (accessed 2026-07-13):
[ZXing repository](https://github.com/zxing/zxing);
[ZXing core Maven Central metadata](https://repo1.maven.org/maven2/com/google/zxing/core/maven-metadata.xml);
[PDFBox downloads](https://pdfbox.apache.org/download.html);
[PDFBox Maven Central metadata](https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/maven-metadata.xml);
[Liberation Fonts license](https://github.com/liberationfonts/liberation-fonts/blob/main/LICENSE);
[OpenPDF vendored Liberation fonts](https://github.com/LibrePDF/OpenPDF/tree/master/openpdf-fonts-extra/src/main/resources/liberation).

## Implementation note (2026-07-14): Phase 10 MFC seam

`orders.OrderCompletionPublisher` (port) and `orders.OrderCompletionEvent`
(the exact `eventId`/`orderId`/`orderNumber`/`completedAt` record decided
above) are implemented; `mfc.NoopOrderCompletionPublisher` is the only
adapter, selected via `wms.mfc.adapter=noop` (`@ConditionalOnProperty`,
`matchIfMissing = true`). `picking.PickingService.confirm()` calls
`publish(...)` exactly where `CustomerOrder` transitions to `COMPLETED`.
`OrderCompletionSeamApiIT` (a Testcontainers-backed full claim/scan/confirm
flow) substitutes `orders.FakeOrderCompletionPublisher` for the no-op adapter
via a `@TestConfiguration`/`@Primary` bean and asserts exactly one
publication with the correct `orderNumber` and non-null `eventId`/
`completedAt`. Future TCP serialization, timeout, delivery-result, retry, and
observability boundaries are documented — not implemented — in
`docs/architecture.md`'s "MFC extension seam" section, per this ADR's
"future MFC transport remains a separate decision" consequence below.

## Consequences

- Exact QR/PDF library, font, and licence evidence must be recorded before
  implementation. **Resolved above.**
- Order-domain code depends only on the publisher port and immutable message.
  **Resolved above** — confirmed by code inspection: neither
  `OrderCompletionPublisher` nor `OrderCompletionEvent` imports anything
  beyond `java.time`/`java.util`, and no socket or telegram class exists
  anywhere in this codebase.
- Future MFC transport remains a separate decision and implementation.

