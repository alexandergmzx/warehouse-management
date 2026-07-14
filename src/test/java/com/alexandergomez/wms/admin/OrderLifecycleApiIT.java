package com.alexandergomez.wms.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * End-to-end order/line lifecycle test (FT-10): a line completes only once
 * all its tasks complete, and the order completes only once all its lines
 * complete — not before. Own container, separate from the other admin/picking
 * test classes, to avoid FIFO-queue ordering interference between suites.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderLifecycleApiIT {

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
    private static final String TEST_PASSWORD = "test-operator-pass-4";

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
    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void ft10_lineCompletesWhenAllItsTasksCompleteAndOrderCompletesWhenAllLinesComplete() throws Exception {
        String adminToken = loginAsAdmin();

        String sku = "TST-LIFE-" + SEQUENCE.incrementAndGet();
        String locationA = "ZLIFA-01-01";
        String locationB = "ZLIFB-01-01";
        createArticle(adminToken, sku, "FT-10 lifecycle test article");
        createLocation(adminToken, locationA, 90301 + SEQUENCE.incrementAndGet());
        createLocation(adminToken, locationB, 90301 + SEQUENCE.incrementAndGet());
        adjustStock(adminToken, sku, locationA, 5, "seed for FT-10 line 1");
        adjustStock(adminToken, sku, locationB, 5, "seed for FT-10 line 2");

        String orderNumber = "TST-LIFE-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> createResponse = createOrder(adminToken, orderNumber, List.of(
                Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5),
                Map.of("lineNumber", 2, "articleSku", sku, "quantity", 5)));
        assertEquals(201, createResponse.getStatusCode().value());
        assertEquals(2, json(createResponse).get("taskCount").asInt());
        backdateOrder(orderNumber);

        String pickerToken = loginAsFreshOperator("ft10picker");

        // Claim + complete the first line's only task.
        claimScanAndConfirm(pickerToken, orderNumber);

        JsonNode afterLine1 = json(getWithToken("/api/v1/admin/orders/" + orderNumber, adminToken));
        assertEquals("IN_PROGRESS", afterLine1.get("state").asString(),
                "order must not complete while line 2 is still open");
        assertEquals("COMPLETED", lineByNumber(afterLine1, 1).get("state").asString());
        assertEquals("OPEN", lineByNumber(afterLine1, 2).get("state").asString());

        // Claim + complete the second (and last) line's only task.
        claimScanAndConfirm(pickerToken, orderNumber);

        JsonNode afterLine2 = json(getWithToken("/api/v1/admin/orders/" + orderNumber, adminToken));
        assertEquals("COMPLETED", afterLine2.get("state").asString());
        assertEquals("COMPLETED", lineByNumber(afterLine2, 1).get("state").asString());
        assertEquals("COMPLETED", lineByNumber(afterLine2, 2).get("state").asString());
    }

    /** Claims the picker's next task (must belong to {@code orderNumber}) and picks it to completion. */
    private void claimScanAndConfirm(String pickerToken, String orderNumber) throws Exception {
        JsonNode claimed = json(getWithToken("/api/v1/hht/tasks/next", pickerToken));
        assertEquals(orderNumber, claimed.get("orderNumber").asString());
        long taskId = claimed.get("id").asLong();
        String locationCode = claimed.get("location").get("code").asString();
        String articleSku = claimed.get("article").get("sku").asString();
        int quantity = claimed.get("quantity").asInt();

        assertEquals(200, postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                objectMapper.writeValueAsString(Map.of("qrValue", "LOC:" + locationCode)), pickerToken)
                .getStatusCode().value());
        assertEquals(200, postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                objectMapper.writeValueAsString(Map.of("qrValue", "ART:" + articleSku)), pickerToken)
                .getStatusCode().value());
        String confirmBody = objectMapper.writeValueAsString(
                Map.of("confirmationId", UUID.randomUUID().toString(), "quantity", quantity));
        assertEquals(200, postJson("/api/v1/hht/tasks/" + taskId + "/confirm", confirmBody, pickerToken)
                .getStatusCode().value());
    }

    private JsonNode lineByNumber(JsonNode orderDetail, int lineNumber) {
        for (JsonNode line : orderDetail.get("lines")) {
            if (line.get("lineNumber").asInt() == lineNumber) {
                return line;
            }
        }
        throw new AssertionError("no line with lineNumber " + lineNumber);
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-LIFECYCLE-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin lifecycle test device");
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "password", "admin123", "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body, null);
        assertEquals(200, login.getStatusCode().value());
        return json(login).get("token").asString();
    }

    private String loginAsFreshOperator(String label) throws Exception {
        int sequence = SEQUENCE.incrementAndGet();
        String deviceCode = "TEST-DEV-" + label.toUpperCase(java.util.Locale.ROOT) + "-" + sequence;
        String username = "test-picker-" + label.toLowerCase(java.util.Locale.ROOT) + "-" + sequence;
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Integration test device (" + label + ")");
        jdbc.update("INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, 'PICKER')",
                username, passwordEncoder.encode(TEST_PASSWORD));
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", TEST_PASSWORD, "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body, null);
        assertEquals(200, login.getStatusCode().value());
        return json(login).get("token").asString();
    }

    private void createArticle(String adminToken, String sku, String description) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("sku", sku, "description", description));
        assertEquals(201, postJson("/api/v1/admin/articles", body, adminToken).getStatusCode().value());
    }

    private void createLocation(String adminToken, String code, int pickSequence) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("code", code, "pickSequence", pickSequence));
        assertEquals(201, postJson("/api/v1/admin/locations", body, adminToken).getStatusCode().value());
    }

    private void adjustStock(String adminToken, String articleSku, String locationCode, int delta, String reason)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "articleSku", articleSku, "locationCode", locationCode, "quantityDelta", delta, "reason", reason));
        assertEquals(201, postJson("/api/v1/admin/stock/adjustments", body, adminToken).getStatusCode().value());
    }

    private ResponseEntity<String> createOrder(String adminToken, String orderNumber, List<Map<String, Object>> lines)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("orderNumber", orderNumber, "lines", lines));
        return postJson("/api/v1/admin/orders", body, adminToken);
    }

    /**
     * Backdates the order so it is unambiguously the oldest in global FIFO
     * order, decoupling this test's picker-facing steps from the shared seed
     * fixture's own older-but-unavailable tasks. Order creation has no request
     * field for {@code created_at} (a real admin cannot backdate an order), so
     * this is deliberately done out-of-band, after the create response already
     * exercised the real endpoint.
     */
    private void backdateOrder(String orderNumber) {
        jdbc.update("""
                UPDATE customer_order
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '100 days',
                    released_at = CURRENT_TIMESTAMP - INTERVAL '100 days'
                WHERE order_number = ?
                """, orderNumber);
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<String> postJson(String path, String body, String token) {
        HttpHeaders headers = jsonHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        return objectMapper.readTree(response.getBody());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
