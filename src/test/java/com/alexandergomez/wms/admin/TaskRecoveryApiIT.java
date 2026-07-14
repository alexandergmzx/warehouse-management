package com.alexandergomez.wms.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * End-to-end administrative task-recovery tests (FT-09): blocking an assigned
 * task audits the required reason and releases the assignment; resuming
 * returns it to {@code AVAILABLE}; neither changes stock. Own container,
 * separate from the other admin/picking test classes, to avoid FIFO-queue
 * ordering interference between suites.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TaskRecoveryApiIT {

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
    private static final String TEST_PASSWORD = "test-operator-pass-3";

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
    void ft09_blockReleasesAssignmentAndAuditsReasonThenResumeReturnsAvailableWithNoStockChange() throws Exception {
        String adminToken = loginAsAdmin();

        String sku = "TST-BLOCK-" + SEQUENCE.incrementAndGet();
        String locationCode = "ZBLK-01-01";
        createArticle(adminToken, sku, "FT-09 block/resume test article");
        createLocation(adminToken, locationCode, 90101 + SEQUENCE.incrementAndGet());
        adjustStock(adminToken, sku, locationCode, 5, "seed for FT-09");

        String orderNumber = "TST-BLOCK-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> createResponse = createOrder(adminToken, orderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5)));
        assertEquals(201, createResponse.getStatusCode().value());
        backdateOrder(orderNumber);

        String pickerToken = loginAsFreshOperator("ft09picker");
        JsonNode claimed = json(getWithToken("/api/v1/hht/tasks/next", pickerToken));
        assertEquals(orderNumber, claimed.get("orderNumber").asString());
        long taskId = claimed.get("id").asLong();

        int stockBefore = stockQuantity(sku, locationCode);

        String reason = "Damaged carton at " + locationCode + "; awaiting supervisor recount";
        ResponseEntity<String> blockResponse = postJson("/api/v1/admin/tasks/" + taskId + "/block",
                objectMapper.writeValueAsString(Map.of("reason", reason)), adminToken);
        assertEquals(200, blockResponse.getStatusCode().value());
        JsonNode blocked = json(blockResponse);
        assertEquals("BLOCKED", blocked.get("state").asString());
        assertEquals(reason, blocked.get("reason").asString());

        Long assignedUserAfterBlock = jdbc.queryForObject(
                "SELECT assigned_user_id FROM picking_task WHERE id = ?", Long.class, taskId);
        assertNull(assignedUserAfterBlock, "blocking must release the assignment");

        String auditedReason = jdbc.queryForObject("""
                SELECT reason FROM task_transition
                WHERE picking_task_id = ? AND new_status = 'BLOCKED'
                """, String.class, taskId);
        assertEquals(reason, auditedReason);
        assertEquals(stockBefore, stockQuantity(sku, locationCode));

        // A PICKER cannot block (admin-only surface).
        assertEquals(403, postJson("/api/v1/admin/tasks/" + taskId + "/block",
                objectMapper.writeValueAsString(Map.of("reason", "picker attempt")), pickerToken)
                .getStatusCode().value());

        ResponseEntity<String> resumeResponse = postJson("/api/v1/admin/tasks/" + taskId + "/resume", "", adminToken);
        assertEquals(200, resumeResponse.getStatusCode().value());
        assertEquals("AVAILABLE", json(resumeResponse).get("state").asString());
        assertEquals(stockBefore, stockQuantity(sku, locationCode));

        // Resuming an already-AVAILABLE task is not valid.
        assertProblem(postJson("/api/v1/admin/tasks/" + taskId + "/resume", "", adminToken), 409, "INVALID_TASK_STATE");
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-RECOVERY-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin recovery test device");
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
     * this is deliberately done out-of-band, after the create response and
     * this method's own assertions already exercised the real endpoint.
     */
    private void backdateOrder(String orderNumber) {
        jdbc.update("""
                UPDATE customer_order
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '100 days',
                    released_at = CURRENT_TIMESTAMP - INTERVAL '100 days'
                WHERE order_number = ?
                """, orderNumber);
    }

    private Integer stockQuantity(String articleSku, String locationCode) {
        return jdbc.queryForObject("""
                SELECT s.quantity FROM stock s
                JOIN article a ON a.id = s.article_id
                JOIN location l ON l.id = s.location_id
                WHERE a.sku = ? AND l.code = ?
                """, Integer.class, articleSku, locationCode);
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
