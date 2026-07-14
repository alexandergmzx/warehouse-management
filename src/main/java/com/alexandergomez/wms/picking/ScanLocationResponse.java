package com.alexandergomez.wms.picking;

import java.time.Instant;

public record ScanLocationResponse(
        Long taskId, String state, String locationCode, Instant confirmedAt, boolean replayed) {
}
