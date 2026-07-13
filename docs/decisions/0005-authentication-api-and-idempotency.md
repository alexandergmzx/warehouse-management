# ADR 0005: Authentication, API errors, and idempotency

- Status: accepted for design; implementation authorized 2026-07-13, delivery pending
- Date: 2026-07-13
- Decisions: D-08, D-09

## Decision

Use Argon2id password hashing; measure and record the selected parameters before
fixture generation. Issue opaque tokens containing at least 256 random bits,
store only their SHA-256 hashes, bind them to one user/device pair, expire them
absolutely (default eight hours), and revoke them on logout or replacement.
Never log credentials, hashes, or bearer tokens.

Use versioned `/api/v1` REST with bearer authentication and RFC 9457
`application/problem+json`. Include stable `code`, `correlationId`, and only
safe endpoint-approved extensions. Exact quantity is mandatory; wrong scans,
invalid state, assignment conflicts, and business quantity/stock failures do
not change stock or movement state.

Location and article scans are retry-safe and never regress state. Final
confirmation requires a client UUID. The same UUID and canonical payload return
the original result without a second decrement; reuse with different content
returns `409 CONFIRMATION_ID_REUSED`. Ambiguous network failures retry with the
same UUID.

## Consequences

- API schemas and the complete problem catalogue are maintained in `API.md`.
- Browser administration uses an HttpOnly, SameSite-Strict cookie and CSRF
  protection for mutations; non-loopback preproduction transport requires HTTPS.
- Authentication and retry behavior require positive, negative, and idempotency
  tests before acceptance.

