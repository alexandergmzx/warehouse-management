package com.alexandergomez.wms.mfc;

import java.time.OffsetDateTime;
import java.util.List;

/** Admin diagnostic read of one mission and its append-only transition history. */
public record MissionDetailResponse(
        Long id,
        String missionType,
        String state,
        Long orderId,
        String orderNumber,
        String sourceLocationCode,
        String destinationLocationCode,
        int attempts,
        String lastError,
        List<TransitionSummary> transitions) {

    public MissionDetailResponse {
        transitions = List.copyOf(transitions);
    }

    public record TransitionSummary(
            String previousState, String newState, String reason, OffsetDateTime occurredAt) {
    }
}
