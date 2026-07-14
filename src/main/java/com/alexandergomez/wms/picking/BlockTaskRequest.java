package com.alexandergomez.wms.picking;

import jakarta.validation.constraints.NotBlank;

public record BlockTaskRequest(@NotBlank String reason) {
}
