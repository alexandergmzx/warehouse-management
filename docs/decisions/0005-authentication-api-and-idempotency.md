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

## Implementation refinement (2026-07-13): demo password hashing

To keep the MVP demo reproducible and remove a per-workstation tuning step from
the critical path, the PoC uses Spring Security's
`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` (Argon2id, salt 16 B,
hash 32 B, parallelism 1, memory 16 MiB, 2 iterations), whose parameters are
OWASP-aligned. Demo credential hashes are precomputed once with this encoder and
stored as literal PHC strings in the `db/devdata` seed migration, replacing the
previous pgcrypto `crypt()/gen_salt('bf', 10)` bcrypt fixtures. Because the
Argon2 PHC string is self-describing, verification needs no external parameter
configuration, and raising parameters later for production does not invalidate
stored hashes (`upgradeEncoding` can trigger a rehash on next login).

The ~250 ms per-workstation measurement referenced above is deferred to a
production-hardening step; the Spring v5.8 defaults are accepted for the LAN
PoC. `password_hash` widens from `VARCHAR(100)` to `VARCHAR(255)` to hold the
~97-character encoded value with headroom. `pgcrypto` is dropped: the demo
`crypt()`/`gen_salt()` fixtures are gone and PostgreSQL 17 provides
`gen_random_uuid()` in core, so the extension is no longer referenced.

Spring Security's `Argon2PasswordEncoder` delegates to BouncyCastle's
`Argon2BytesGenerator`. Spring Boot 4 does not manage a BouncyCastle version, so
the provider is pinned explicitly to `org.bouncycastle:bcprov-jdk18on:1.85`, the
latest stable patch level for the cryptographic provider (Maven Central metadata
accessed 2026-07-13). Track this pin as a security dependency and prefer the
current patched release over an older frozen one.

Sources (accessed 2026-07-13):
[OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html);
[Spring Security `Argon2PasswordEncoder` API](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.html).

## Consequences

- API schemas and the complete problem catalogue are maintained in `API.md`.
- Browser administration uses an HttpOnly, SameSite-Strict cookie and CSRF
  protection for mutations; non-loopback preproduction transport requires HTTPS.
- Authentication and retry behavior require positive, negative, and idempotency
  tests before acceptance.

