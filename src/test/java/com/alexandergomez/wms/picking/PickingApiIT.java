package com.alexandergomez.wms.picking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * End-to-end picking workflow tests over the running HTTP server and the
 * migrated, digest-pinned PostgreSQL container. Covers FT-06 (scan-location and
 * scan-article progress the task in order and repeated correct scans replay
 * safely) and FT-08 (exact-quantity confirm creates one movement and one
 * decrement; repeating the same confirmation UUID and quantity replays the
 * original result).
 *
 * <p>Every test claims "whichever task {@code GET /hht/tasks/next} returns" and
 * validates against that task's own returned fields, rather than hard-coding a
 * specific seeded task number. This keeps tests order-independent: each uses a
 * freshly inserted picker/device pair (the seed fixture's only free user is
 * {@code admin} and its only device is already busy with {@code picker01}'s
 * seeded active task), so claiming does not race with other test methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PickingApiIT {

    /** Immutable image reference required by ADR 0002; keep in sync with compose.yaml. */
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    private static final AtomicInteger OPERATOR_SEQUENCE = new AtomicInteger();
    private static final String TEST_PASSWORD = "test-operator-pass-1";

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    // JDK HttpClient factory: the default HttpURLConnection does not expose the
    // response body for certain non-2xx methods; kept consistent with AuthApiIT.
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
    void ft06AndFt08_claimScanConfirmAndReplaySafely() throws Exception {
        String token = loginAsFreshOperator("primary");

        // FT-06 / claim: the first GET /next call atomically claims the oldest
        // AVAILABLE task in FIFO order.
        ResponseEntity<String> firstClaim = getWithToken("/api/v1/hht/tasks/next", token);
        assertEquals(200, firstClaim.getStatusCode().value());
        JsonNode claimed = json(firstClaim);
        long taskId = claimed.get("id").asLong();
        assertEquals("ASSIGNED", claimed.get("state").asString());
        String locationCode = claimed.get("location").get("code").asString();
        String articleSku = claimed.get("article").get("sku").asString();
        int quantity = claimed.get("quantity").asInt();

        // Repeating GET /next before any scan returns the SAME active task, not a
        // new claim (one active task per user).
        ResponseEntity<String> secondNext = getWithToken("/api/v1/hht/tasks/next", token);
        assertEquals(200, secondNext.getStatusCode().value());
        assertEquals(taskId, json(secondNext).get("id").asLong());
        assertEquals("ASSIGNED", json(secondNext).get("state").asString());

        // Wrong location leaves the task in ASSIGNED (no state regression).
        assertProblem(postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                "{\"qrValue\":\"LOC:Z-99-99\"}", token), 409, "WRONG_LOCATION");

        // Correct location scan progresses the task.
        ResponseEntity<String> locationScan = postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                "{\"qrValue\":\"LOC:" + locationCode + "\"}", token);
        assertEquals(200, locationScan.getStatusCode().value());
        JsonNode locationBody = json(locationScan);
        assertEquals("LOCATION_CONFIRMED", locationBody.get("state").asString());
        assertFalse(locationBody.get("replayed").asBoolean());

        // Repeating the same correct location scan replays without regressing state.
        ResponseEntity<String> locationReplay = postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                "{\"qrValue\":\"LOC:" + locationCode + "\"}", token);
        assertEquals(200, locationReplay.getStatusCode().value());
        assertEquals("LOCATION_CONFIRMED", json(locationReplay).get("state").asString());
        assertTrue(json(locationReplay).get("replayed").asBoolean());

        // Wrong article leaves the task in LOCATION_CONFIRMED (no state regression).
        assertProblem(postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                "{\"qrValue\":\"ART:DOES-NOT-EXIST\"}", token), 409, "WRONG_ARTICLE");

        // Correct article scan progresses the task.
        ResponseEntity<String> articleScan = postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                "{\"qrValue\":\"ART:" + articleSku + "\"}", token);
        assertEquals(200, articleScan.getStatusCode().value());
        JsonNode articleBody = json(articleScan);
        assertEquals("ARTICLE_CONFIRMED", articleBody.get("state").asString());
        assertFalse(articleBody.get("replayed").asBoolean());

        // Repeating the same correct article scan replays without regressing state.
        ResponseEntity<String> articleReplay = postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                "{\"qrValue\":\"ART:" + articleSku + "\"}", token);
        assertEquals(200, articleReplay.getStatusCode().value());
        assertEquals("ARTICLE_CONFIRMED", json(articleReplay).get("state").asString());
        assertTrue(json(articleReplay).get("replayed").asBoolean());

        Integer stockBeforeConfirm = stockQuantity(articleSku, locationCode);

        // FT-08 / confirm: exact quantity creates one decrement and one PICK movement.
        String confirmationId = UUID.randomUUID().toString();
        String confirmBody = objectMapper.writeValueAsString(
                Map.of("confirmationId", confirmationId, "quantity", quantity));
        ResponseEntity<String> confirm = postJson("/api/v1/hht/tasks/" + taskId + "/confirm", confirmBody, token);
        assertEquals(200, confirm.getStatusCode().value());
        JsonNode confirmed = json(confirm);
        assertEquals("COMPLETED", confirmed.get("state").asString());
        assertEquals(quantity, confirmed.get("confirmedQuantity").asInt());
        long movementId = confirmed.get("movementId").asLong();
        int remainingStock = confirmed.get("remainingStock").asInt();
        assertEquals(stockBeforeConfirm - quantity, remainingStock);
        assertEquals((long) (stockBeforeConfirm - quantity), (long) stockQuantity(articleSku, locationCode));

        Long movementCountAfterFirstConfirm = jdbc.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE picking_task_id = ?", Long.class, taskId);
        assertEquals(1L, movementCountAfterFirstConfirm);

        // Idempotent retry: same confirmation UUID and same quantity returns the
        // original result without a second stock decrement.
        ResponseEntity<String> retry = postJson("/api/v1/hht/tasks/" + taskId + "/confirm", confirmBody, token);
        assertEquals(200, retry.getStatusCode().value());
        JsonNode retried = json(retry);
        assertEquals("COMPLETED", retried.get("state").asString());
        assertEquals(movementId, retried.get("movementId").asLong());
        assertEquals(remainingStock, retried.get("remainingStock").asInt());
        assertEquals((long) (stockBeforeConfirm - quantity), (long) stockQuantity(articleSku, locationCode));

        Long movementCountAfterRetry = jdbc.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE picking_task_id = ?", Long.class, taskId);
        assertEquals(1L, movementCountAfterRetry);
    }

    private String loginAsFreshOperator(String label) throws Exception {
        int sequence = OPERATOR_SEQUENCE.incrementAndGet();
        String deviceCode = "TEST-DEV-" + label.toUpperCase(java.util.Locale.ROOT) + "-" + sequence;
        String username = "test-picker-" + label + "-" + sequence;

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
        JsonNode body = json(response);
        assertEquals(code, body.get("code").asString());
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
