package com.alexandergomez.wms.inventory;

/**
 * One stock bin carrying a given article, identified for allocation purposes.
 * {@code locationCode} drives the ascending multi-bin allocation order
 * (confirmed workflow baseline); the numeric IDs drive the deadlock-safe
 * ascending {@code (article_id, location_id)} lock order (ADR 0003) — the two
 * orderings are independent and must not be conflated.
 */
public record CandidateBin(long articleId, long locationId, String locationCode) {
}
