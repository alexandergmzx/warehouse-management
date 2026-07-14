package com.alexandergomez.wms.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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
 * End-to-end order-allocation tests (FT-02): a two-bin split succeeds in
 * ascending location-code order, and a shortage rolls back the entire attempt
 * (no order, line, or task row survives). No picker is needed for this
 * class's scope, so the picking FIFO queue and its shared seed fixture are
 * never touched here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderAllocationApiIT {

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
    void ft02_twoBinSplitSucceedsInAscendingCodeOrderAndShortageRollsBackEverything() throws Exception {
        String adminToken = loginAsAdmin();

        String sku = "TST-SPLIT-" + SEQUENCE.incrementAndGet();
        String locationA = "ZSPLA-01-01";
        String locationB = "ZSPLB-01-02";
        createArticle(adminToken, sku, "FT-02 split test article");
        createLocation(adminToken, locationA, 90201);
        createLocation(adminToken, locationB, 90202);
        adjustStock(adminToken, sku, locationA, 6, "seed for FT-02 bin A");
        adjustStock(adminToken, sku, locationB, 10, "seed for FT-02 bin B");

        String orderNumber = "TST-SPLIT-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> createResponse = createOrder(adminToken, orderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 10)));
        assertEquals(201, createResponse.getStatusCode().value());
        JsonNode created = json(createResponse);
        assertEquals("OPEN", created.get("state").asString());
        assertEquals(1, created.get("lineCount").asInt());
        assertEquals(2, created.get("taskCount").asInt());
        assertEquals("/api/v1/admin/orders/" + orderNumber,
                createResponse.getHeaders().getFirst(HttpHeaders.LOCATION));

        JsonNode detail = json(getWithToken("/api/v1/admin/orders/" + orderNumber, adminToken));
        JsonNode line = detail.get("lines").get(0);
        assertEquals(10, line.get("requestedQuantity").asInt());
        JsonNode tasks = line.get("tasks");
        assertEquals(2, tasks.size());
        assertEquals(locationA, tasks.get(0).get("locationCode").asString());
        assertEquals(6, tasks.get(0).get("quantity").asInt());
        assertEquals("AVAILABLE", tasks.get(0).get("state").asString());
        assertEquals(locationB, tasks.get(1).get("locationCode").asString());
        assertEquals(4, tasks.get(1).get("quantity").asInt());
        assertEquals("AVAILABLE", tasks.get(1).get("state").asString());

        // Shortage: after the first order, availability is 0 at bin A (6-6) and
        // 6 at bin B (10-4). Requesting more than that must roll back entirely.
        String shortageOrderNumber = "TST-SPLIT-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> shortageResponse = createOrder(adminToken, shortageOrderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 100)));
        assertProblem(shortageResponse, 409, "INSUFFICIENT_AVAILABLE_STOCK");

        assertEquals(404, getWithToken("/api/v1/admin/orders/" + shortageOrderNumber, adminToken)
                .getStatusCode().value());
        Long strayTaskCount = jdbc.queryForObject(
                "SELECT count(*) FROM picking_task WHERE task_number LIKE ?", Long.class, shortageOrderNumber + "%");
        assertEquals(0L, strayTaskCount);
    }

    private String loginAsAdmin() throws Exception {
        int sequence = SEQUENCE.incrementAndGet();
        String deviceCode = "TEST-ADMIN-DEV-ALLOC-" + sequence;
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin allocation test device");
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "password", "admin123", "deviceCode", deviceCode));
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

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertEquals(status, response.getStatusCode().value());
        MediaType contentType = response.getHeaders().getContentType();
        assertTrue(contentType != null && contentType.toString().contains("application/problem+json"),
                "expected problem+json, was " + contentType);
        assertEquals(code, json(response).get("code").asString());
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
