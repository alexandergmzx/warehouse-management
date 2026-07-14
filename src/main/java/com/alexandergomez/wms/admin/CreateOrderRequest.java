package com.alexandergomez.wms.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record CreateOrderRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_-]{1,50}$") String orderNumber,
        @NotEmpty @Valid List<CreateOrderLineRequest> lines) {

    public CreateOrderRequest {
        lines = lines == null ? null : List.copyOf(lines);
    }
}
