package com.alexandergomez.wms.orders;

/**
 * Order-line lifecycle states (ADR 0004). A line completes only when all its
 * tasks complete.
 */
public enum OrderLineStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED
}
