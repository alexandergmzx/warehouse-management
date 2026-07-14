package com.alexandergomez.wms.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.alexandergomez.wms.WarehouseManagementApplication;

/**
 * FT-15: starting the {@code preprod} profile with missing or unsafe required
 * configuration fails fast with a safe diagnostic (no secret or connection
 * detail exposed), rather than a raw property-binding stack trace.
 */
@ExtendWith(OutputCaptureExtension.class)
class PreprodConfigurationValidatorIT {

    @Test
    void missingDatabaseUrlFailsFastWithCleanDiagnostic(CapturedOutput output) {
        SpringApplication application = new SpringApplication(WarehouseManagementApplication.class);
        application.setAdditionalProfiles("preprod");
        application.setWebApplicationType(WebApplicationType.NONE);

        try {
            application.run("--WMS_DB_USERNAME=wms_preprod", "--WMS_DB_PASSWORD=a-real-preprod-secret");
            fail("expected preprod startup to fail on a missing WMS_DB_URL");
        } catch (RuntimeException expected) {
            // The failure itself is the point; assertions below are on the reported diagnostic.
        }

        assertTrue(output.getAll().contains("Missing required preprod configuration: WMS_DB_URL"),
                "expected the clean missing-variable diagnostic in the startup report");
        assertTrue(output.getAll().contains("docs/configuration-matrix.md"),
                "expected the remediation pointer in the startup report");
        assertFalse(output.getAll().contains("a-real-preprod-secret"),
                "the configured secret value must never be printed");
    }

    @Test
    void devDefaultPasswordInPreprodFailsFastWithCleanDiagnostic(CapturedOutput output) {
        SpringApplication application = new SpringApplication(WarehouseManagementApplication.class);
        application.setAdditionalProfiles("preprod");
        application.setWebApplicationType(WebApplicationType.NONE);

        try {
            application.run(
                    "--WMS_DB_URL=jdbc:postgresql://prod-db.internal:5432/wms",
                    "--WMS_DB_USERNAME=wms_preprod",
                    "--WMS_DB_PASSWORD=" + PreprodConfigurationValidator.DEV_DEFAULT_PASSWORD);
            fail("expected preprod startup to fail when reusing the committed dev password");
        } catch (RuntimeException expected) {
            // The failure itself is the point; assertions below are on the reported diagnostic.
        }

        assertTrue(output.getAll().contains("committed development default"),
                "expected the clean unsafe-password diagnostic in the startup report");
        assertFalse(output.getAll().contains(PreprodConfigurationValidator.DEV_DEFAULT_PASSWORD),
                "the offending password value must never be printed, even though it is a known default");
    }
}
