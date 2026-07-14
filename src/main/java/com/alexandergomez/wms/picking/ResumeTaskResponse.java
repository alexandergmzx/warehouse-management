package com.alexandergomez.wms.picking;

import java.time.Instant;

public record ResumeTaskResponse(Long taskId, String state, Instant resumedAt) {
}
