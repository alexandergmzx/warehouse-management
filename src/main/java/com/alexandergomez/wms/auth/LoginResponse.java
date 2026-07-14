package com.alexandergomez.wms.auth;

import java.time.Instant;

import com.alexandergomez.wms.identity.AppUser;
import com.alexandergomez.wms.identity.Device;
import com.alexandergomez.wms.identity.IssuedToken;

/**
 * Successful login payload (API.md). {@code token} is returned exactly once and
 * is never logged or persisted in raw form.
 */
public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserSummary user,
        DeviceSummary device) {

    public record UserSummary(Long id, String username, String role) {
    }

    public record DeviceSummary(Long id, String code) {
    }

    public static LoginResponse of(IssuedToken issued, AppUser user, Device device) {
        return new LoginResponse(
                issued.token(),
                "Bearer",
                issued.expiresAt().toInstant(),
                new UserSummary(user.getId(), user.getUsername(), user.getRole().name()),
                new DeviceSummary(device.getId(), device.getDeviceCode()));
    }
}
