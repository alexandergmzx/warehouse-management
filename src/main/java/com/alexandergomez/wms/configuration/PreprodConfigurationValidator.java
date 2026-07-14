package com.alexandergomez.wms.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Fails fast, before any bean (including the datasource) is created, when the
 * {@code preprod} profile is missing a required database variable or carries
 * the committed development password (FT-15). Runs after
 * {@link ConfigDataEnvironmentPostProcessor} so profile-specific YAML has
 * already been merged. Deliberately throws rather than logs: a {@link
 * PreprodConfigurationFailureAnalyzer} renders the clean diagnostic, so this
 * class does not depend on the logging subsystem being initialized yet.
 *
 * <p>Registered in {@code META-INF/spring.factories} — this extension point
 * predates the {@code *.imports} file convention (confirmed against the
 * Spring Boot 4.0.7 sources: {@code org.springframework.boot.env.…} is
 * deprecated for removal in 4.2.0 in favor of this package).
 */
public class PreprodConfigurationValidator implements EnvironmentPostProcessor, Ordered {

    static final String PREPROD_PROFILE = "preprod";
    static final String DEV_DEFAULT_PASSWORD = "wms_dev_password";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of(PREPROD_PROFILE))) {
            return;
        }

        String url = environment.getProperty("WMS_DB_URL");
        String username = environment.getProperty("WMS_DB_USERNAME");
        String password = environment.getProperty("WMS_DB_PASSWORD");

        List<String> missing = new ArrayList<>();
        if (isBlank(url)) {
            missing.add("WMS_DB_URL");
        }
        if (isBlank(username)) {
            missing.add("WMS_DB_USERNAME");
        }
        if (isBlank(password)) {
            missing.add("WMS_DB_PASSWORD");
        }
        if (!missing.isEmpty()) {
            throw new PreprodConfigurationException(
                    "Missing required preprod configuration: " + String.join(", ", missing)
                            + ". Set these environment variables before starting the preprod profile.");
        }
        if (DEV_DEFAULT_PASSWORD.equals(password)) {
            throw new PreprodConfigurationException(
                    "WMS_DB_PASSWORD is set to the committed development default; "
                            + "preprod must use a distinct credential.");
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 10;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
