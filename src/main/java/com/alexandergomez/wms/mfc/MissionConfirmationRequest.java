package com.alexandergomez.wms.mfc;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * WCS confirmation request body (TELEGRAMS.md). {@code state} must be
 * {@code ACCEPTED}, {@code COMPLETED}, or {@code FAILED}; {@code reason} is
 * required when {@code state} is {@code FAILED}.
 */
public record MissionConfirmationRequest(
        @NotBlank String state, @NotNull OffsetDateTime occurredAt, String reason) {
}
