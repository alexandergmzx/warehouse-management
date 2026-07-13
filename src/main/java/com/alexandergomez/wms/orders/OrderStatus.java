package com.alexandergomez.wms.orders;

/**
 * Order lifecycle states (ADR 0004). An order completes only when all its lines
 * complete.
 */
public enum OrderStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED
}
