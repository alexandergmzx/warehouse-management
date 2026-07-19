package com.alexandergomez.wms.mfc;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wire shape of the outbound MFC telegram (TELEGRAMS.md,
 * {@code src/test/resources/telegrams/transport-mission.json}). Field names
 * are the JSON member names verbatim (default Jackson camelCase binding).
 */
public record TelegramPayload(
        Long missionId,
        UUID eventId,
        String missionType,
        Long orderId,
        String orderNumber,
        String sourceLocationCode,
        String destinationLocationCode,
        String state,
        OffsetDateTime dispatchedAt) {
}
