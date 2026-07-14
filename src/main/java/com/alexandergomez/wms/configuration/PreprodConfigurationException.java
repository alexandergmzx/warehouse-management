package com.alexandergomez.wms.configuration;

/**
 * Thrown by {@link PreprodConfigurationValidator} when the {@code preprod}
 * profile is missing a required variable or is configured with a known-unsafe
 * value. The message is safe to display verbatim: it names the offending
 * variable, never its value.
 */
public class PreprodConfigurationException extends RuntimeException {

    public PreprodConfigurationException(String message) {
        super(message);
    }
}
