package com.alexandergomez.wms.picking;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Final pick confirmation (API.md). {@code quantity} is intentionally not
 * range-validated here: zero, over, and partial quantities are all rejected by
 * the exact-quantity business rule ({@code QUANTITY_MISMATCH}, FT-07) rather
 * than by request-level validation, so every mismatch takes the same path.
 */
public record ConfirmRequest(@NotNull UUID confirmationId, @NotNull Integer quantity) {
}
