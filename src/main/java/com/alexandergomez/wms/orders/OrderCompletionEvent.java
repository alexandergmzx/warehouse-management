package com.alexandergomez.wms.orders;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable order-completion message (ADR 0007, Phase 10 MFC extension
 * seam). {@code eventId} is also the idempotency identifier a future
 * at-least-once transport would use to detect a redelivered event.
 */
public record OrderCompletionEvent(UUID eventId, Long orderId, String orderNumber, Instant completedAt) {
}
