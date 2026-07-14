package com.alexandergomez.wms.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials plus the handheld being used. All fields are required;
 * blank values yield {@code 422 VALIDATION_FAILED}.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String deviceCode) {
}
