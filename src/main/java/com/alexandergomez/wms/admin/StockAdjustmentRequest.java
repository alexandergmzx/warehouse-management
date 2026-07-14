package com.alexandergomez.wms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotBlank String articleSku,
        @NotBlank String locationCode,
        @NotNull Integer quantityDelta,
        @NotBlank String reason) {
}
