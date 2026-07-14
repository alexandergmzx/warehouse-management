package com.alexandergomez.wms.identity;

/**
 * Reason a bearer token was not accepted. The web layer maps these to stable
 * problem codes; inactive user/device fail closed as {@code INVALID}.
 */
public enum TokenFailure {
    INVALID,
    EXPIRED,
    REVOKED
}
