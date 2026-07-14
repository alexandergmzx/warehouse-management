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

    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed"),
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
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    TASK_NOT_ASSIGNED_TO_USER(HttpStatus.CONFLICT, "Task not assigned to user"),
    INVALID_TASK_STATE(HttpStatus.CONFLICT, "Invalid task state"),
    WRONG_LOCATION(HttpStatus.CONFLICT, "Wrong location"),
    WRONG_ARTICLE(HttpStatus.CONFLICT, "Wrong article"),
    TASK_ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "Task assignment conflict"),
    CONFIRMATION_ID_REUSED(HttpStatus.CONFLICT, "Confirmation ID reused"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock"),
    QUANTITY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Quantity mismatch"),
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
