package com.alexandergomez.wms.inventory;

/**
 * Stock movement categories. Mirrors the {@code stock_movement.movement_type}
 * check constraint; only {@code PICK} is tied to an order/line/task/device.
 */
public enum MovementType {
    INITIAL_STOCK,
    RECEIPT,
    PICK,
    ADJUSTMENT
}
