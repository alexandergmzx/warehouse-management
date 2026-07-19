package com.alexandergomez.wms.mfc;

/**
 * Mirrors {@code mfc_mission.mission_type} (TELEGRAMS.md). {@code SORT} is
 * specified but never emitted by this version; any confirmation naming a
 * {@code SORT} mission is rejected as {@code 501 SORT_NOT_IMPLEMENTED}.
 */
public enum MissionType {
    TRANSPORT,
    SORT
}
