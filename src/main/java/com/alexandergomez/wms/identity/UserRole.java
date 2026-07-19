package com.alexandergomez.wms.identity;

/**
 * Application roles. Mirrors the {@code app_user.role} check constraint.
 * {@code WCS} authenticates {@code agv-fleet-controller} (or its dev stand-in,
 * {@code scripts/wcs-standin/}) to confirm MFC missions (ADR 0011).
 */
public enum UserRole {
    ADMIN,
    PICKER,
    WCS
}
