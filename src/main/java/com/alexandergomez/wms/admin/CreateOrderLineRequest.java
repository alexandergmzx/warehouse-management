package com.alexandergomez.wms.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderLineRequest(
        @NotNull @Min(1) Integer lineNumber,
        @NotBlank String articleSku,
        @NotNull @Min(1) Integer quantity) {
}
