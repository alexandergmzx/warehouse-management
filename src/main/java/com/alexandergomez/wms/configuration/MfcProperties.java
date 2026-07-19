package com.alexandergomez.wms.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MFC work package configuration (ADR 0011). {@code telegram}/{@code
 * transport} fields are only meaningful when {@code adapter=telegram}; the
 * default {@code noop} adapter never reads them, so they carry harmless
 * blank/default values rather than {@code @NotNull} constraints that would
 * force every profile to set them. {@code
 * mfc.TelegramOrderCompletionPublisher} and {@code mfc.MissionDispatcher}
 * validate their own required fields at construction and refuse to start if
 * missing, scoped by {@code @ConditionalOnProperty} to only when the
 * telegram adapter is actually selected.
 */
@Validated
@ConfigurationProperties(prefix = "wms.mfc")
public record MfcProperties(String adapter, Telegram telegram, Transport transport) {

    public MfcProperties {
        if (telegram == null) {
            telegram = new Telegram(null, Duration.ofSeconds(30), 5);
        }
        if (transport == null) {
            transport = new Transport(null, null);
        }
    }

    public record Telegram(String baseUrl, Duration retryInterval, int maxAttempts) {
    }

    public record Transport(String sourceLocation, String destinationLocation) {
    }
}
