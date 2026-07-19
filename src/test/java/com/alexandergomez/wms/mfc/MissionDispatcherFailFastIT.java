package com.alexandergomez.wms.mfc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.alexandergomez.wms.WarehouseManagementApplication;

/**
 * ADR 0011's "refuses to start" claim, proved rather than asserted: selecting
 * the {@code telegram} adapter without the required base-url or transport
 * location configuration must fail application startup ({@code
 * MissionDispatcher}/{@code TelegramOrderCompletionPublisher}'s {@code
 * @PostConstruct}/constructor checks), not fail silently or only at first
 * use. A real database is provided (Testcontainers, same image as every
 * other IT) so the only possible failure is the configuration check under
 * test, not an unrelated datasource problem.
 */
@Testcontainers
class MissionDispatcherFailFastIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    @Test
    void telegramAdapterWithoutBaseUrlRefusesToStart() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ConfigurableApplicationContext ignored = run(
                    "wms.mfc.adapter=telegram",
                    "wms.mfc.transport.source-location=MFC-90-01",
                    "wms.mfc.transport.destination-location=MFC-90-02")) {
                // never reached
            }
        });
        assertTrue(containsMessage(failure, "wms.mfc.telegram.base-url"),
                "failure must name the missing property, not just 'something went wrong': " + failure);
    }

    @Test
    void telegramAdapterWithoutTransportLocationsRefusesToStart() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ConfigurableApplicationContext ignored = run(
                    "wms.mfc.adapter=telegram",
                    "wms.mfc.telegram.base-url=http://localhost:1")) {
                // never reached
            }
        });
        assertTrue(containsMessage(failure, "wms.mfc.transport.source-location"),
                "failure must name the missing property, not just 'something went wrong': " + failure);
    }

    /**
     * Command-line-style {@code --key=value} arguments, the only property
     * source with higher precedence than {@code application-dev.yml}'s own
     * {@code spring.datasource.url} default — {@link
     * SpringApplicationBuilder#properties} alone is not enough to override it.
     */
    private static ConfigurableApplicationContext run(String... mfcProperties) {
        java.util.List<String> args = new java.util.ArrayList<>(java.util.List.of(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.flyway.locations=classpath:db/migration,classpath:db/devdata",
                "--spring.jpa.hibernate.ddl-auto=validate"));
        for (String property : mfcProperties) {
            args.add("--" + property);
        }
        return new SpringApplicationBuilder(WarehouseManagementApplication.class)
                .web(WebApplicationType.NONE)
                .run(args.toArray(new String[0]));
    }

    private static boolean containsMessage(Throwable throwable, String needle) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
