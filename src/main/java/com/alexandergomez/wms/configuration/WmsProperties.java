package com.alexandergomez.wms.configuration;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "wms")
public record WmsProperties(
        @Valid @NotNull Task task, @Valid @NotNull Auth auth, @Valid @NotNull Dashboard dashboard) {

    public record Task(@NotNull Duration stuckThreshold) {
    }

    public record Auth(@NotNull Duration tokenTtl) {
    }

    public record Dashboard(@NotNull Duration pollInterval) {
    }
}

