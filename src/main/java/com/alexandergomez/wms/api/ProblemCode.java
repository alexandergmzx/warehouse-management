package com.alexandergomez.wms.api;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;

/**
 * Stable application error catalogue (API.md). The wire {@code code} is the enum
 * constant name; the problem {@code type} URI is derived from it. Codes, titles,
 * and statuses are part of the {@code v1} contract and must not change within it.
 */
public enum ProblemCode {

    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expired"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Token revoked"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "User inactive"),
    DEVICE_INACTIVE(HttpStatus.FORBIDDEN, "Device inactive"),
    DEVICE_NOT_REGISTERED(HttpStatus.NOT_FOUND, "Device not registered"),
    DEVICE_ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "Device assignment conflict"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

    private static final String TYPE_PREFIX = "https://warehouse.example/problems/";

    private final HttpStatus status;
    private final String title;

    ProblemCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String code() {
        return name();
    }

    public URI type() {
        return URI.create(TYPE_PREFIX + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
