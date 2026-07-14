package com.alexandergomez.wms.admin;

import java.time.Instant;

/** One row of the dashboard-facing task list (API.md). */
public record AdminTaskSummary(
        Long id,
        String taskNumber,
        String state,
        String orderNumber,
        Integer lineNumber,
        String locationCode,
        String articleSku,
        Integer quantity,
        String assignedUsername,
        String deviceCode,
        Instant lastTransitionAt,
        boolean stuck) {
}
