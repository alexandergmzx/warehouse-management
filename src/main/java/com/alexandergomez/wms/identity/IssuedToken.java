package com.alexandergomez.wms.identity;

import java.time.OffsetDateTime;

/**
 * A freshly issued opaque token and its absolute expiry. The raw token is
 * returned to the client exactly once and never persisted (only its hash is).
 */
public record IssuedToken(String token, OffsetDateTime expiresAt) {
}
