package com.alexandergomez.wms.picking;

/**
 * Picking-task states (ADR 0004). Normal path:
 * {@code AVAILABLE -> ASSIGNED -> LOCATION_CONFIRMED -> ARTICLE_CONFIRMED -> COMPLETED}.
 * An administrator may {@code BLOCK} active work and resume it to
 * {@code AVAILABLE}; the HHT has no skip, block, or resume operation.
 */
public enum TaskStatus {
    AVAILABLE,
    ASSIGNED,
    LOCATION_CONFIRMED,
    ARTICLE_CONFIRMED,
    BLOCKED,
    COMPLETED
}
