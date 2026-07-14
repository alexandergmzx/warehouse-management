package com.alexandergomez.wms.picking;

import java.time.Instant;

/**
 * The caller's active task: either just claimed or already held (API.md).
 */
public record NextTaskResponse(
        Long id,
        String state,
        String orderNumber,
        Integer lineNumber,
        Integer taskSequence,
        LocationSummary location,
        ArticleSummary article,
        Integer quantity,
        Instant assignedAt) {

    public record LocationSummary(String code) {
    }

    public record ArticleSummary(String sku, String description) {
    }
}
