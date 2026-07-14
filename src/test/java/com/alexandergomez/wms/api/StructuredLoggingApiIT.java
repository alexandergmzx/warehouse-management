package com.alexandergomez.wms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * FT-16: executing a stock-changing request produces a structured JSON log
 * entry carrying correlation/article/location/quantity/actor/duration fields
 * and no secret; a business-rule violation (the wrong-scan/stock-integrity
 * class of incident the Phase 9 acceptance gate names) is diagnosable from a
 * second, centralized log entry without a debugger.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingApiIT {

    /** Immutable image reference required by ADR 0002; keep in sync with compose.yaml. */
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    private final RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void doNotThrowOnErrorStatus() {
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    @Test
    void ft16_stockChangingRequestLogsStructuredFieldsAndBusinessViolationsAreDiagnosable(CapturedOutput output)
            throws Exception {
        String adminToken = loginAsAdmin();
        String sku = "TST-LOG-" + SEQUENCE.incrementAndGet();
        String locationCode = "ZLOG-01-01";
        createArticle(adminToken, sku, "FT-16 structured logging test article");
        createLocation(adminToken, locationCode, 90401 + SEQUENCE.incrementAndGet());

        // Positive case: a real stock-changing request (the adjustment endpoint).
        adjustStock(adminToken, sku, locationCode, 7, "FT-16 positive case");

        JsonNode adjustmentLog = findJsonLogLine(output, "\"message\":\"stock adjusted");
        assertEquals(sku, adjustmentLog.get("articleSku").asString());
        assertEquals(locationCode, adjustmentLog.get("locationCode").asString());
        assertEquals(7, adjustmentLog.get("quantityDelta").asInt());
        assertEquals(7, adjustmentLog.get("resultingQuantity").asInt());
        assertTrue(adjustmentLog.has("adminUserId"));
        assertTrue(adjustmentLog.has("correlationId"), "correlation id should be present via MDC");

        // Negative case: a business-rule violation (NEGATIVE_RESULTING_STOCK) must be
        // diagnosable from logs alone, per the Phase 9 acceptance gate.
        adjustStock(adminToken, sku, locationCode, -100, "FT-16 negative case");

        JsonNode violationLog = findJsonLogLine(output, "\"message\":\"business rule violation");
        assertEquals("NEGATIVE_RESULTING_STOCK", violationLog.get("problemCode").asString());
        assertTrue(violationLog.has("correlationId"));

        // No secret or token leaks into the structured log stream.
        assertFalse(output.getAll().contains(adminToken), "the bearer token must never be logged");
        assertFalse(output.getAll().contains("admin123"), "the login password must never be logged");
    }

    private JsonNode findJsonLogLine(CapturedOutput output, String marker) throws Exception {
        Optional<String> line = output.getAll().lines()
                .filter(candidate -> candidate.contains(marker))
                .reduce((first, second) -> second); // the most recent matching line
        assertTrue(line.isPresent(), "expected a JSON log line containing: " + marker);
        return objectMapper.readTree(line.get());
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-LOG-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Structured logging test device");
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "password", "admin123", "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body, null);
        assertEquals(200, login.getStatusCode().value());
        return objectMapper.readTree(login.getBody()).get("token").asString();
    }

    private void createArticle(String adminToken, String sku, String description) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("sku", sku, "description", description));
        assertEquals(201, postJson("/api/v1/admin/articles", body, adminToken).getStatusCode().value());
    }

    private void createLocation(String adminToken, String code, int pickSequence) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("code", code, "pickSequence", pickSequence));
        assertEquals(201, postJson("/api/v1/admin/locations", body, adminToken).getStatusCode().value());
    }

    private ResponseEntity<String> adjustStock(
            String adminToken, String articleSku, String locationCode, int delta, String reason) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "articleSku", articleSku, "locationCode", locationCode, "quantityDelta", delta, "reason", reason));
        return postJson("/api/v1/admin/stock/adjustments", body, adminToken);
    }

    private ResponseEntity<String> postJson(String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
