package com.alexandergomez.wms.mfc;

public record MissionConfirmationResponse(Long missionId, String state, boolean replayed) {
}
