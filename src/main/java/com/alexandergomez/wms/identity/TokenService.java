package com.alexandergomez.wms.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.configuration.WmsProperties;

/**
 * Issues, validates, and revokes opaque bearer tokens (ADR 0005). Tokens carry
 * at least 256 bits of entropy; only their SHA-256 hash is persisted, bound to
 * one user/device pair with an absolute expiry. The raw token never leaves this
 * service except in the login response, and is never logged.
 */
@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "wms_";
    private static final int TOKEN_BYTES = 32;

    private final AuthTokenRepository authTokens;
    private final AppUserRepository users;
    private final DeviceRepository devices;
    private final WmsProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(AuthTokenRepository authTokens, AppUserRepository users,
            DeviceRepository devices, WmsProperties properties) {
        this.authTokens = authTokens;
        this.users = users;
        this.devices = devices;
        this.properties = properties;
    }

    @Transactional
    public IssuedToken issue(AppUser user, Device device) {
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.auth().tokenTtl());
        authTokens.save(AuthToken.issue(sha256Hex(token), user.getId(), device.getId(), expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public TokenAuthentication authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return TokenAuthentication.failure(TokenFailure.INVALID);
        }
        Optional<AuthToken> maybeToken = authTokens.findByTokenHash(sha256Hex(rawToken));
        if (maybeToken.isEmpty()) {
            return TokenAuthentication.failure(TokenFailure.INVALID);
        }
        AuthToken token = maybeToken.get();
        if (token.getRevokedAt() != null) {
            return TokenAuthentication.failure(TokenFailure.REVOKED);
        }
        if (!token.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            return TokenAuthentication.failure(TokenFailure.EXPIRED);
        }
        AppUser user = users.findById(token.getUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            return TokenAuthentication.failure(TokenFailure.INVALID);
        }
        Device device = devices.findById(token.getDeviceId()).orElse(null);
        if (device == null || !device.isActive()) {
            return TokenAuthentication.failure(TokenFailure.INVALID);
        }
        return TokenAuthentication.success(new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getRole(),
                device.getId(), device.getDeviceCode(), token.getId()));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        authTokens.findByTokenHash(sha256Hex(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(OffsetDateTime.now(ZoneOffset.UTC));
                authTokens.save(token);
            }
        });
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }
}
