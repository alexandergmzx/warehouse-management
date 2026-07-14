package com.alexandergomez.wms.configuration;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Renders {@link PreprodConfigurationException} as a clean, one-paragraph
 * startup diagnostic instead of a raw stack trace (FT-15's "safe diagnostic"
 * requirement). Registered in {@code META-INF/spring.factories}.
 */
public class PreprodConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<PreprodConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PreprodConfigurationException cause) {
        return new FailureAnalysis(
                cause.getMessage(),
                "Review docs/configuration-matrix.md for the required WMS_DB_* variables "
                        + "and their sensitivity, then set them before starting the preprod profile again.",
                cause);
    }
}
