package com.alexandergomez.wms.picking;

import java.time.Instant;

public record ConfirmResponse(
        Long taskId,
        String state,
        Integer confirmedQuantity,
        Long movementId,
        Integer remainingStock,
        OrderSummary order,
        Instant completedAt) {

    public record OrderSummary(String number, String state) {
    }
}
