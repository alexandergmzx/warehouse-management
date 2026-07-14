package com.alexandergomez.wms.picking;

import java.time.Instant;

public record BlockTaskResponse(Long taskId, String state, String reason, Instant blockedAt) {
}
