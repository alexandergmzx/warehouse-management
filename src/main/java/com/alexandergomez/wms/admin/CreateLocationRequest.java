package com.alexandergomez.wms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateLocationRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]+-[0-9]{2}-[0-9]{2}$") String code,
        @NotNull @Positive Integer pickSequence) {
}
