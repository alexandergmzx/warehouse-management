package com.alexandergomez.wms.picking;

import java.time.Instant;

public record ScanArticleResponse(
        Long taskId, String state, String articleSku, Instant confirmedAt, boolean replayed) {
}
