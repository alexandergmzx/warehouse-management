package com.alexandergomez.wms.picking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * End-to-end negative-path, idempotency, and concurrency tests for the picking
 * workflow, against a migrated, digest-pinned PostgreSQL container dedicated to
 * this class (kept separate from {@link PickingApiIT} so the two test classes
 * never share fixture state or depend on method execution order).
 *
 * <p>Covers FT-05 (wrong location/article leave stock, movement, and task state
 * unchanged), FT-07 (zero/over/partial quantity are all rejected by the exact-
 * quantity rule with no stock or movement change), FT-12 (reusing a confirmation
 * UUID with a different quantity is {@code 409 CONFIRMATION_ID_REUSED} with no
 * second stock change), and FT-04 (concurrent claims never assign the same task
 * to two different users, and a user racing a claim against themselves ends up
 * with at most one active task).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PickingNegativePathApiIT {

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
    private static final String TEST_PASSWORD = "test-operator-pass-2";

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
    void ft05Ft07Ft12_wrongScansQuantityMismatchAndConfirmationReuseLeaveNoTrace() throws Exception {
        OperatorSession operator = loginAsFreshOperator("neg");
        String token = operator.token();

        // Claim the oldest AVAILABLE seed task (deterministic FIFO order).
        JsonNode claimed = json(getWithToken("/api/v1/hht/tasks/next", token));
        long taskId = claimed.get("id").asLong();
        String locationCode = claimed.get("location").get("code").asString();
        String articleSku = claimed.get("article").get("sku").asString();
        int quantity = claimed.get("quantity").asInt();
        int stockBefore = stockQuantity(articleSku, locationCode);

        // FT-05a: wrong location while ASSIGNED is rejected; task, stock, and
        // movement state do not advance.
        assertProblem(postJson(scanLocationPath(taskId), qr("LOC:Z-99-99"), token), 409, "WRONG_LOCATION");
        assertEquals("ASSIGNED", activeTaskState(token));
        assertEquals(stockBefore, stockQuantity(articleSku, locationCode));
        assertEquals(0L, movementCount(taskId));

        // Correct location scan progresses normally.
        assertEquals(200, postJson(scanLocationPath(taskId), qr("LOC:" + locationCode), token)
                .getStatusCode().value());
        assertEquals("LOCATION_CONFIRMED", activeTaskState(token));

        // FT-05b: wrong article while LOCATION_CONFIRMED is rejected; task, stock,
        // and movement state do not advance.
        assertProblem(postJson(scanArticlePath(taskId), qr("ART:DOES-NOT-EXIST"), token), 409, "WRONG_ARTICLE");
        assertEquals("LOCATION_CONFIRMED", activeTaskState(token));
        assertEquals(stockBefore, stockQuantity(articleSku, locationCode));
        assertEquals(0L, movementCount(taskId));

        // Correct article scan progresses normally.
        assertEquals(200, postJson(scanArticlePath(taskId), qr("ART:" + articleSku), token)
                .getStatusCode().value());
        assertEquals("ARTICLE_CONFIRMED", activeTaskState(token));

        // FT-07: zero, over-quantity, and partial quantity are all rejected by the
        // exact-quantity rule, with no stock or movement change for any of them.
        for (int badQuantity : new int[] {0, quantity + 5, quantity - 1}) {
            String body = objectMapper.writeValueAsString(
                    Map.of("confirmationId", UUID.randomUUID().toString(), "quantity", badQuantity));
            assertProblem(postJson(confirmPath(taskId), body, token), 422, "QUANTITY_MISMATCH");
        }
        assertEquals("ARTICLE_CONFIRMED", activeTaskState(token));
        assertEquals(stockBefore, stockQuantity(articleSku, locationCode));
        assertEquals(0L, movementCount(taskId));

        // Exact quantity succeeds, completing the task with one movement.
        String confirmationId = UUID.randomUUID().toString();
        String confirmBody = objectMapper.writeValueAsString(
                Map.of("confirmationId", confirmationId, "quantity", quantity));
        ResponseEntity<String> confirm = postJson(confirmPath(taskId), confirmBody, token);
        assertEquals(200, confirm.getStatusCode().value());
        assertEquals(stockBefore - quantity, stockQuantity(articleSku, locationCode));
        assertEquals(1L, movementCount(taskId));

        // FT-12: reusing the same confirmation UUID with a DIFFERENT quantity is
        // rejected; no second stock change or movement is created.
        String reuseBody = objectMapper.writeValueAsString(
                Map.of("confirmationId", confirmationId, "quantity", quantity - 1));
        assertProblem(postJson(confirmPath(taskId), reuseBody, token), 409, "CONFIRMATION_ID_REUSED");
        assertEquals(stockBefore - quantity, stockQuantity(articleSku, locationCode));
        assertEquals(1L, movementCount(taskId));
    }

    @Test
    void ft04_concurrentClaimsNeverDuplicateAndOneUserGetsAtMostOneActiveTask() throws Exception {
        // A small isolated pool of synthetic AVAILABLE tasks, so this race is
        // independent of whatever the other test method in this class consumes
        // and independent of method execution order.
        long orderId = insertSyntheticOrder("CONC-TEST-1");
        long lineId = insertSyntheticOrderLine(orderId, "ART-002");
        long taskA = insertSyntheticTask(lineId, 1, "ART-002", "A-02-01");
        long taskB = insertSyntheticTask(lineId, 2, "ART-002", "A-02-01");
        long taskC = insertSyntheticTask(lineId, 3, "ART-002", "A-02-01");
        long taskD = insertSyntheticTask(lineId, 4, "ART-002", "A-02-01");
        Set<Long> pool = Set.of(taskA, taskB, taskC, taskD);

        // Part A: two DIFFERENT users claiming concurrently must never receive
        // the same task. SKIP LOCKED guarantees this deterministically: whichever
        // transaction locks a row first, the other's scan simply skips it.
        OperatorSession operatorA = loginAsFreshOperator("concA");
        OperatorSession operatorB = loginAsFreshOperator("concB");
        ExecutorService twoWorkers = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<ResponseEntity<String>> futureA =
                    twoWorkers.submit(() -> claimWithBarrier(barrier, operatorA.token()));
            Future<ResponseEntity<String>> futureB =
                    twoWorkers.submit(() -> claimWithBarrier(barrier, operatorB.token()));

            ResponseEntity<String> responseA = futureA.get(20, TimeUnit.SECONDS);
            ResponseEntity<String> responseB = futureB.get(20, TimeUnit.SECONDS);

            assertEquals(200, responseA.getStatusCode().value());
            assertEquals(200, responseB.getStatusCode().value());
            long claimedA = json(responseA).get("id").asLong();
            long claimedB = json(responseB).get("id").asLong();
            assertTrue(claimedA != claimedB, "two concurrent claimants must never receive the same task");
            assertTrue(pool.contains(claimedA));
            assertTrue(pool.contains(claimedB));
        } finally {
            twoWorkers.shutdownNow();
        }

        // Part B: the SAME user firing two concurrent claim requests (a
        // double-tap) ends up with at most one active task in the database,
        // regardless of which HTTP call "won."
        OperatorSession operatorC = loginAsFreshOperator("concC");
        ExecutorService raceWorkers = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<ResponseEntity<String>> first =
                    raceWorkers.submit(() -> claimWithBarrier(barrier, operatorC.token()));
            Future<ResponseEntity<String>> second =
                    raceWorkers.submit(() -> claimWithBarrier(barrier, operatorC.token()));

            ResponseEntity<String> r1 = first.get(20, TimeUnit.SECONDS);
            ResponseEntity<String> r2 = second.get(20, TimeUnit.SECONDS);

            List<Integer> statuses = List.of(r1.getStatusCode().value(), r2.getStatusCode().value());
            assertTrue(statuses.contains(200), "at least one concurrent claim by the same user must succeed");
            for (int status : statuses) {
                assertTrue(status == 200 || status == 409,
                        "expected 200 (won the race) or 409 TASK_ASSIGNMENT_CONFLICT (lost it), was " + status);
            }

            long activeTasksForOperatorC = jdbc.queryForObject("""
                    SELECT count(*) FROM picking_task
                    WHERE assigned_user_id = (SELECT id FROM app_user WHERE username = ?)
                      AND status IN ('ASSIGNED', 'LOCATION_CONFIRMED', 'ARTICLE_CONFIRMED')
                    """, Long.class, operatorC.username());
            assertEquals(1L, activeTasksForOperatorC,
                    "a user/device may hold at most one active task even after a claim race");
        } finally {
            raceWorkers.shutdownNow();
        }
    }

    private ResponseEntity<String> claimWithBarrier(CyclicBarrier barrier, String token) throws Exception {
        barrier.await(10, TimeUnit.SECONDS);
        return getWithToken("/api/v1/hht/tasks/next", token);
    }

    private record OperatorSession(String username, String token) {
    }

    private OperatorSession loginAsFreshOperator(String label) throws Exception {
        int sequence = OPERATOR_SEQUENCE.incrementAndGet();
        String deviceCode = "TEST-DEV-" + label.toUpperCase(Locale.ROOT) + "-" + sequence;
        String username = "test-picker-" + label.toLowerCase(Locale.ROOT) + "-" + sequence;

        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Integration test device (" + label + ")");
        jdbc.update("INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, 'PICKER')",
                username, passwordEncoder.encode(TEST_PASSWORD));

        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", TEST_PASSWORD, "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body, null);
        assertEquals(200, login.getStatusCode().value());
        return new OperatorSession(username, json(login).get("token").asString());
    }

    private long insertSyntheticOrder(String orderNumber) {
        // Backdated well before every seed order: FIFO claims the oldest order
        // first, and the sibling test method in this class may leave leftover
        // AVAILABLE seed tasks that would otherwise be claimed before this pool.
        Long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);
        jdbc.update("""
                INSERT INTO customer_order (order_number, status, created_by_user_id, created_at, released_at)
                VALUES (?, 'OPEN', ?, CURRENT_TIMESTAMP - INTERVAL '100 days',
                        CURRENT_TIMESTAMP - INTERVAL '100 days')
                """, orderNumber, adminId);
        return jdbc.queryForObject("SELECT id FROM customer_order WHERE order_number = ?", Long.class, orderNumber);
    }

    private long insertSyntheticOrderLine(long orderId, String articleSku) {
        Long articleId = jdbc.queryForObject("SELECT id FROM article WHERE sku = ?", Long.class, articleSku);
        jdbc.update("""
                INSERT INTO order_line (order_id, line_number, article_id, requested_quantity)
                VALUES (?, 1, ?, 1000)
                """, orderId, articleId);
        return jdbc.queryForObject(
                "SELECT id FROM order_line WHERE order_id = ? AND line_number = 1", Long.class, orderId);
    }

    private long insertSyntheticTask(long lineId, int taskSequence, String articleSku, String locationCode) {
        Long articleId = jdbc.queryForObject("SELECT id FROM article WHERE sku = ?", Long.class, articleSku);
        Long locationId = jdbc.queryForObject("SELECT id FROM location WHERE code = ?", Long.class, locationCode);
        String taskNumber = "CONC-TEST-TASK-" + lineId + "-" + taskSequence;
        jdbc.update("""
                INSERT INTO picking_task (
                    task_number, order_line_id, task_sequence, article_id, source_location_id, requested_quantity
                )
                VALUES (?, ?, ?, ?, ?, 1)
                """, taskNumber, lineId, taskSequence, articleId, locationId);
        return jdbc.queryForObject("SELECT id FROM picking_task WHERE task_number = ?", Long.class, taskNumber);
    }

    private String activeTaskState(String token) {
        ResponseEntity<String> response = getWithToken("/api/v1/hht/tasks/next", token);
        assertEquals(200, response.getStatusCode().value());
        return json(response).get("state").asString();
    }

    private long movementCount(long taskId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE picking_task_id = ?", Long.class, taskId);
    }

    private Integer stockQuantity(String articleSku, String locationCode) {
        return jdbc.queryForObject("""
                SELECT s.quantity FROM stock s
                JOIN article a ON a.id = s.article_id
                JOIN location l ON l.id = s.location_id
                WHERE a.sku = ? AND l.code = ?
                """, Integer.class, articleSku, locationCode);
    }

    private String qr(String value) {
        return objectMapper.writeValueAsString(Map.of("qrValue", value));
    }

    private static String scanLocationPath(long taskId) {
        return "/api/v1/hht/tasks/" + taskId + "/scan-location";
    }

    private static String scanArticlePath(long taskId) {
        return "/api/v1/hht/tasks/" + taskId + "/scan-article";
    }

    private static String confirmPath(long taskId) {
        return "/api/v1/hht/tasks/" + taskId + "/confirm";
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
