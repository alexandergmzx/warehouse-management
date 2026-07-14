package com.alexandergomez.wms.admin;

import java.time.Instant;

public record CreateOrderResponse(
        String orderNumber, String state, Instant createdAt, Integer lineCount, Integer taskCount) {
}
