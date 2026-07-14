package com.alexandergomez.wms.identity;

import java.util.UUID;

/**
 * Authenticated principal established from a valid bearer token: the operator,
 * their role, the bound device, and the token identifier. Placed in the Spring
 * Security context and available to controllers.
 */
public record AuthenticatedUser(
        Long userId,
        String username,
        UserRole role,
        Long deviceId,
        String deviceCode,
        UUID tokenId) {
}
