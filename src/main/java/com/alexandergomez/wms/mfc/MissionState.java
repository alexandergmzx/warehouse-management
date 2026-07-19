package com.alexandergomez.wms.mfc;

/**
 * Mirrors {@code mfc_mission.state} (TELEGRAMS.md). Allowed transitions:
 * {@code PENDING -> DISPATCHED}, {@code PENDING -> FAILED} (dispatch
 * exhaustion, WMS-side), {@code DISPATCHED -> ACCEPTED},
 * {@code DISPATCHED -> FAILED}, {@code ACCEPTED -> COMPLETED},
 * {@code ACCEPTED -> FAILED}. A confirmation naming the mission's current
 * state is an idempotent replay, not a transition.
 */
public enum MissionState {
    PENDING,
    DISPATCHED,
    ACCEPTED,
    COMPLETED,
    FAILED
}
