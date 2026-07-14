package com.alexandergomez.wms.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
 * Phase 10 MFC extension-seam acceptance gate: completing an order publishes
 * exactly one {@link OrderCompletionEvent} through the {@link
 * OrderCompletionPublisher} port. The fake below stands in for {@code
 * NoopOrderCompletionPublisher}; order-domain code (this test's target,
 * {@link com.alexandergomez.wms.picking.PickingService}) imports only the
 * port and the immutable event — no TCP or telegram class exists anywhere in
 * this codebase to leak into it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderCompletionSeamApiIT {

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

    @TestConfiguration
    static class FakePublisherConfiguration {
        @Bean
        @Primary
        OrderCompletionPublisher orderCompletionPublisher() {
            return new FakeOrderCompletionPublisher();
        }
    }

    private final RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private OrderCompletionPublisher orderCompletionPublisher;

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
    void completingAnOrderPublishesExactlyOneCompletionEvent() throws Exception {
        FakeOrderCompletionPublisher fake = (FakeOrderCompletionPublisher) orderCompletionPublisher;
        assertEquals(0, fake.published().size(), "fixture setup must not itself complete any order");

        String adminToken = loginAsAdmin();
        String sku = "TST-MFC-" + SEQUENCE.incrementAndGet();
        String locationCode = "ZMFC-01-01";
        createArticle(adminToken, sku, "Phase 10 seam test article");
        createLocation(adminToken, locationCode, 90501 + SEQUENCE.incrementAndGet());
        adjustStock(adminToken, sku, locationCode, 5, "seed for Phase 10 seam test");

        String orderNumber = "TST-MFC-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> createResponse = createOrder(adminToken, orderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5)));
        assertEquals(201, createResponse.getStatusCode().value());
        backdateOrder(orderNumber);

        String pickerToken = loginAsFreshOperator("mfcpicker");
        JsonNode claimed = json(getWithToken("/api/v1/hht/tasks/next", pickerToken));
        assertEquals(orderNumber, claimed.get("orderNumber").asString());
        long taskId = claimed.get("id").asLong();

        assertEquals(200, postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                objectMapper.writeValueAsString(Map.of("qrValue", "LOC:" + locationCode)), pickerToken)
                .getStatusCode().value());
        assertEquals(200, postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                objectMapper.writeValueAsString(Map.of("qrValue", "ART:" + sku)), pickerToken)
                .getStatusCode().value());

        String confirmationId = java.util.UUID.randomUUID().toString();
        ResponseEntity<String> confirmResponse = postJson("/api/v1/hht/tasks/" + taskId + "/confirm",
                objectMapper.writeValueAsString(Map.of("confirmationId", confirmationId, "quantity", 5)),
                pickerToken);
        assertEquals(200, confirmResponse.getStatusCode().value());
        JsonNode confirmed = json(confirmResponse);
        assertEquals("COMPLETED", confirmed.get("order").get("state").asString());

        assertEquals(1, fake.published().size(), "exactly one completion event must be published");
        OrderCompletionEvent event = fake.published().get(0);
        assertNotNull(event.eventId());
        assertEquals(orderNumber, event.orderNumber());
        assertNotNull(event.completedAt());
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-MFC-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin MFC seam test device");
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
        jdbc.update("""
                INSERT INTO app_user (username, password_hash, role)
                VALUES (?, (SELECT password_hash FROM app_user WHERE username = 'picker01'), 'PICKER')
                """, username);
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "picker123", "deviceCode", deviceCode));
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
     * fixture's own older-but-unavailable tasks (same technique used by the
     * Phase 7 admin recovery tests and the Phase 8 label test).
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
        return restTemplate.exchange(url(path), org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<String> postJson(String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
