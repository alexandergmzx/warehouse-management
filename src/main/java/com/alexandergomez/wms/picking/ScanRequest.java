package com.alexandergomez.wms.picking;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared request shape for scan-location and scan-article (API.md): both
 * endpoints accept exactly one scanned QR payload.
 */
public record ScanRequest(@NotBlank String qrValue) {
}
