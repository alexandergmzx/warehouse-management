package com.alexandergomez.wms.mfc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * MFC work package acceptance gates (PLAN.md, ADR 0011, TELEGRAMS.md):
 * completing an order queues exactly one {@code PENDING} TRANSPORT mission;
 * the dispatcher delivers, retries, and eventually gives up; the WCS
 * confirmation endpoint drives the mission through
 * {@code ACCEPTED -> COMPLETED} with idempotent replay and rejects illegal
 * transitions, unknown missions, unauthenticated callers, and {@code SORT}
 * missions (visibly, as {@code 501}). The {@link MissionDispatcher} bean is
 * called directly ({@code dispatchNextOnce()}) rather than waiting on
 * {@code @Scheduled} timing, for deterministic tests; the retry interval is
 * set far longer than the test run so the background scheduler's own tick
 * cannot race these explicit calls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MfcTelegramLifecycleIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    /** Stub WCS receiver, started eagerly so its port is known before {@link DynamicPropertySource}. */
    private static final StubWcsServer STUB_WCS = StubWcsServer.startOrThrow();

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("wms.mfc.adapter", () -> "telegram");
        registry.add("wms.mfc.telegram.base-url", () -> "http://localhost:" + STUB_WCS.port());
        // Far longer than any test run: the background @Scheduled tick must not
        // race the explicit dispatchNextOnce() calls below.
        registry.add("wms.mfc.telegram.retry-interval", () -> "PT600S");
        registry.add("wms.mfc.telegram.max-attempts", () -> "3");
        registry.add("wms.mfc.transport.source-location", () -> "MFC-90-01");
        registry.add("wms.mfc.transport.destination-location", () -> "MFC-90-02");
    }

    @AfterAll
    static void stopStubWcs() {
        STUB_WCS.stop();
    }

    private final RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MissionDispatcher dispatcher;

    MfcTelegramLifecycleIT() {
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    @Test
    void completingAnOrderQueuesExactlyOnePendingTransportMission() throws Exception {
        String adminToken = loginAsAdmin();
        String sku = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, sku, locationCode, 5);

        String orderNumber = "TST-MFC-ORD-" + SEQUENCE.incrementAndGet();
        createOrder(adminToken, orderNumber, List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5)));
        backdateOrder(orderNumber);

        assertEquals(0, countMissionsForOrder(orderNumber), "no mission before the order completes");

        String pickerToken = loginAsFreshOperator("emit");
        pickAndConfirm(pickerToken, orderNumber, locationCode, sku, 5);

        assertEquals(1, countMissionsForOrder(orderNumber), "exactly one mission after completion");
        Map<String, Object> mission = jdbc.queryForMap(
                "SELECT mm.*, ls.code AS source_code, ld.code AS destination_code "
                        + "FROM mfc_mission mm "
                        + "JOIN location ls ON ls.id = mm.source_location_id "
                        + "JOIN location ld ON ld.id = mm.destination_location_id "
                        + "WHERE mm.order_number = ?",
                orderNumber);
        assertEquals("TRANSPORT", mission.get("mission_type"));
        assertEquals("PENDING", mission.get("state"));
        assertEquals("MFC-90-01", mission.get("source_code"));
        assertEquals("MFC-90-02", mission.get("destination_code"));
        assertEquals(0, ((Number) mission.get("attempts")).intValue());

        // Drain it so this mission never lingers PENDING for a later test
        // method's dispatchNextOnce() call to accidentally pick up.
        assertTrue(dispatcher.dispatchNextOnce());
    }

    @Test
    void incompleteOrderQueuesNoMission() throws Exception {
        String adminToken = loginAsAdmin();
        String skuA = article(adminToken);
        String skuB = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, skuA, locationCode, 5);
        adjustStock(adminToken, skuB, locationCode, 5);

        String orderNumber = "TST-MFC-PARTIAL-" + SEQUENCE.incrementAndGet();
        createOrder(adminToken, orderNumber, List.of(
                Map.of("lineNumber", 1, "articleSku", skuA, "quantity", 5),
                Map.of("lineNumber", 2, "articleSku", skuB, "quantity", 5)));
        backdateOrder(orderNumber);

        String pickerToken = loginAsFreshOperator("partial");
        // The first line's task is picked; the order stays IN_PROGRESS, and no
        // mission may be queued yet. The second line is picked immediately
        // after (never left dangling AVAILABLE, which would otherwise hijack
        // a later test method's FIFO claim) — completing it then completes
        // the order, proving the mission fires exactly at that point.
        pickAndConfirm(pickerToken, orderNumber, locationCode, skuA, 5);
        assertEquals(0, countMissionsForOrder(orderNumber),
                "an order that has not fully completed must not queue a mission");

        pickAndConfirm(pickerToken, orderNumber, locationCode, skuB, 5);
        assertEquals(1, countMissionsForOrder(orderNumber),
                "completing the remaining line must queue exactly one mission");
        assertTrue(dispatcher.dispatchNextOnce());
    }

    @Test
    void dispatchDeliversRetriesAndEventuallyExhausts() throws Exception {
        String adminToken = loginAsAdmin();
        String sku = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, sku, locationCode, 15);

        // (a) immediate success
        long okMissionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "ok");
        int bodiesBefore = STUB_WCS.receivedBodies().size();
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("DISPATCHED", stateOf(okMissionId));
        assertEquals(1, attemptsOf(okMissionId));
        assertEquals(bodiesBefore + 1, STUB_WCS.receivedBodies().size());
        String lastBody = STUB_WCS.receivedBodies().get(STUB_WCS.receivedBodies().size() - 1);
        JsonNode telegram = objectMapper.readTree(lastBody);
        assertEquals(okMissionId, telegram.get("missionId").asLong());
        assertEquals("TRANSPORT", telegram.get("missionType").asString());
        assertEquals("MFC-90-01", telegram.get("sourceLocationCode").asString());
        assertEquals("MFC-90-02", telegram.get("destinationLocationCode").asString());

        // (b) one failure, then success. The configured retry-interval is
        // deliberately long (see properties()) so the background @Scheduled
        // tick can't race these explicit calls; that also means a failed
        // mission's next_attempt_at is far in the future, so the test fast-
        // forwards it directly, the same way backdateOrder() controls FIFO.
        long retryMissionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "retry");
        STUB_WCS.enqueueStatus(500);
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("PENDING", stateOf(retryMissionId), "a failed attempt stays PENDING for retry");
        assertEquals(1, attemptsOf(retryMissionId));
        forceRetryNow(retryMissionId);
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("DISPATCHED", stateOf(retryMissionId));
        assertEquals(2, attemptsOf(retryMissionId));

        // (c) exhaustion: max-attempts is 3
        long failMissionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "fail");
        STUB_WCS.enqueueStatus(500);
        STUB_WCS.enqueueStatus(500);
        STUB_WCS.enqueueStatus(500);
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("PENDING", stateOf(failMissionId));
        forceRetryNow(failMissionId);
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("PENDING", stateOf(failMissionId));
        forceRetryNow(failMissionId);
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("FAILED", stateOf(failMissionId));
        assertEquals(3, attemptsOf(failMissionId));
        Map<String, Object> failed = jdbc.queryForMap("SELECT last_error FROM mfc_mission WHERE id = ?", failMissionId);
        assertTrue(failed.get("last_error") != null, "a FAILED mission records the last dispatch error");

        // FAILED is terminal: nothing further to claim from this queue.
        assertFalse(dispatcher.dispatchNextOnce());
    }

    /**
     * Regression for the {@code @Scheduled} entry point specifically: {@link
     * MissionDispatcher#dispatchPending()} must dispatch through the Spring
     * proxy so each claimed mission runs inside {@code dispatchNextOnce()}'s
     * {@code @Transactional} boundary. A plain internal {@code
     * this.dispatchNextOnce()} call bypasses the proxy and fails the
     * pessimistic-lock query with "No active transaction" — a failure mode
     * the other tests miss because they invoke {@code dispatchNextOnce()}
     * directly on the injected proxy bean.
     */
    @Test
    void scheduledDispatchLoopRunsEachMissionInATransaction() throws Exception {
        String adminToken = loginAsAdmin();
        String sku = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, sku, locationCode, 5);
        long missionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "scheduled");

        // The scheduled entry point, not the direct per-mission call: this is
        // exactly the path the background @Scheduled trigger takes.
        dispatcher.dispatchPending();

        assertEquals("DISPATCHED", stateOf(missionId),
                "the scheduled loop must dispatch the mission, not throw 'No active transaction'");
    }

    @Test
    void wcsConfirmationLifecycle() throws Exception {
        String adminToken = loginAsAdmin();
        String sku = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, sku, locationCode, 5);
        long missionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "confirm");
        assertTrue(dispatcher.dispatchNextOnce());
        assertEquals("DISPATCHED", stateOf(missionId));

        String wcsToken = loginAsWcs();

        ResponseEntity<String> accepted = confirm(missionId, wcsToken, "ACCEPTED", null);
        assertEquals(200, accepted.getStatusCode().value());
        JsonNode acceptedBody = json(accepted);
        assertEquals("ACCEPTED", acceptedBody.get("state").asString());
        assertFalse(acceptedBody.get("replayed").asBoolean());

        ResponseEntity<String> replay = confirm(missionId, wcsToken, "ACCEPTED", null);
        assertEquals(200, replay.getStatusCode().value());
        assertTrue(json(replay).get("replayed").asBoolean(), "re-confirming the current state must be a no-op replay");

        ResponseEntity<String> completed = confirm(missionId, wcsToken, "COMPLETED", null);
        assertEquals(200, completed.getStatusCode().value());
        assertEquals("COMPLETED", stateOf(missionId));

        ResponseEntity<String> illegal = confirm(missionId, wcsToken, "ACCEPTED", null);
        assertEquals(409, illegal.getStatusCode().value());
        assertEquals("INVALID_MISSION_STATE", json(illegal).get("code").asString());

        ResponseEntity<String> unknown = confirm(999_999_999L, wcsToken, "ACCEPTED", null);
        assertEquals(404, unknown.getStatusCode().value());
        assertEquals("MISSION_NOT_FOUND", json(unknown).get("code").asString());

        ResponseEntity<String> noAuth = confirm(missionId, null, "ACCEPTED", null);
        assertTrue(noAuth.getStatusCode().value() == 401 || noAuth.getStatusCode().value() == 403);

        long sortMissionId = insertSortMission();
        ResponseEntity<String> sortStub = confirm(sortMissionId, wcsToken, "ACCEPTED", null);
        assertEquals(501, sortStub.getStatusCode().value());
        assertEquals("SORT_NOT_IMPLEMENTED", json(sortStub).get("code").asString());
    }

    @Test
    void adminReadsMissionDetailWithTransitionHistory() throws Exception {
        String adminToken = loginAsAdmin();
        String sku = article(adminToken);
        String locationCode = location(adminToken);
        adjustStock(adminToken, sku, locationCode, 5);
        long missionId = completeOrderAndGetMissionId(adminToken, sku, locationCode, "admin");
        assertTrue(dispatcher.dispatchNextOnce());

        String wcsToken = loginAsWcs();
        assertEquals(200, confirm(missionId, wcsToken, "ACCEPTED", null).getStatusCode().value());

        ResponseEntity<String> detail = getWithToken("/api/v1/admin/mfc/missions/" + missionId, adminToken);
        assertEquals(200, detail.getStatusCode().value());
        JsonNode body = json(detail);
        assertEquals("ACCEPTED", body.get("state").asString());
        assertEquals("MFC-90-01", body.get("sourceLocationCode").asString());
        assertEquals(2, body.get("transitions").size(), "PENDING->DISPATCHED and DISPATCHED->ACCEPTED");
    }

    // --- fixtures & helpers -------------------------------------------------

    private long completeOrderAndGetMissionId(String adminToken, String sku, String locationCode, String label)
            throws Exception {
        String orderNumber = "TST-MFC-" + label.toUpperCase(Locale.ROOT) + "-" + SEQUENCE.incrementAndGet();
        createOrder(adminToken, orderNumber, List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5)));
        backdateOrder(orderNumber);
        String pickerToken = loginAsFreshOperator(label + SEQUENCE.incrementAndGet());
        pickAndConfirm(pickerToken, orderNumber, locationCode, sku, 5);
        return jdbc.queryForObject("SELECT id FROM mfc_mission WHERE order_number = ?", Long.class, orderNumber);
    }

    private void pickAndConfirm(String pickerToken, String orderNumber, String locationCode, String sku, int quantity)
            throws Exception {
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
        ResponseEntity<String> confirmed = postJson("/api/v1/hht/tasks/" + taskId + "/confirm",
                objectMapper.writeValueAsString(Map.of("confirmationId", confirmationId, "quantity", quantity)),
                pickerToken);
        assertEquals(200, confirmed.getStatusCode().value());
    }

    private long insertSortMission() {
        long orderId = jdbc.queryForObject("SELECT id FROM customer_order LIMIT 1", Long.class);
        Long sourceId = jdbc.queryForObject("SELECT id FROM location WHERE code = 'MFC-90-01'", Long.class);
        Long destinationId = jdbc.queryForObject("SELECT id FROM location WHERE code = 'MFC-90-02'", Long.class);
        return jdbc.queryForObject("""
                INSERT INTO mfc_mission
                    (event_id, mission_type, state, order_id, order_number,
                     source_location_id, destination_location_id, attempts)
                VALUES (gen_random_uuid(), 'SORT', 'PENDING', ?, 'TST-MFC-SORT-STUB', ?, ?, 0)
                RETURNING id
                """, Long.class, orderId, sourceId, destinationId);
    }

    private void forceRetryNow(long missionId) {
        jdbc.update("UPDATE mfc_mission SET next_attempt_at = CURRENT_TIMESTAMP WHERE id = ?", missionId);
    }

    private String stateOf(long missionId) {
        return jdbc.queryForObject("SELECT state FROM mfc_mission WHERE id = ?", String.class, missionId);
    }

    private int attemptsOf(long missionId) {
        return jdbc.queryForObject("SELECT attempts FROM mfc_mission WHERE id = ?", Integer.class, missionId);
    }

    private int countMissionsForOrder(String orderNumber) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mfc_mission WHERE order_number = ?", Integer.class, orderNumber);
        return count == null ? 0 : count;
    }

    private ResponseEntity<String> confirm(long missionId, String token, String state, String reason)
            throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("state", state);
        body.put("occurredAt", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
        if (reason != null) {
            body.put("reason", reason);
        }
        return postJson("/api/v1/mfc/missions/" + missionId + "/confirmations",
                objectMapper.writeValueAsString(body), token);
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-MFC-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin MFC test device");
        return loginAndGetToken("admin", "admin123", deviceCode);
    }

    private String loginAsWcs() throws Exception {
        return loginAndGetToken("wcs01", "wcs01pass", "AGV-FC-01");
    }

    private String loginAsFreshOperator(String label) throws Exception {
        int sequence = SEQUENCE.incrementAndGet();
        String deviceCode = "TEST-DEV-" + label.toUpperCase(Locale.ROOT) + "-" + sequence;
        String username = "test-picker-" + label.toLowerCase(Locale.ROOT) + "-" + sequence;
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Integration test device (" + label + ")");
        jdbc.update("""
                INSERT INTO app_user (username, password_hash, role)
                VALUES (?, (SELECT password_hash FROM app_user WHERE username = 'picker01'), 'PICKER')
                """, username);
        return loginAndGetToken(username, "picker123", deviceCode);
    }

    private String loginAndGetToken(String username, String password, String deviceCode) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password, "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body, null);
        assertEquals(200, login.getStatusCode().value(), "login must succeed for " + username);
        return json(login).get("token").asString();
    }

    private String article(String adminToken) throws Exception {
        String sku = "TST-MFC-" + SEQUENCE.incrementAndGet();
        String body = objectMapper.writeValueAsString(Map.of("sku", sku, "description", "MFC test article"));
        assertEquals(201, postJson("/api/v1/admin/articles", body, adminToken).getStatusCode().value());
        return sku;
    }

    private String location(String adminToken) throws Exception {
        String code = "ZMFC-01-" + String.format(Locale.ROOT, "%02d", SEQUENCE.incrementAndGet() % 90 + 1);
        String body = objectMapper.writeValueAsString(
                Map.of("code", code, "pickSequence", 91000 + SEQUENCE.incrementAndGet()));
        assertEquals(201, postJson("/api/v1/admin/locations", body, adminToken).getStatusCode().value());
        return code;
    }

    private void adjustStock(String adminToken, String articleSku, String locationCode, int delta) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("articleSku", articleSku, "locationCode", locationCode,
                "quantityDelta", delta, "reason", "seed for MFC test"));
        assertEquals(201, postJson("/api/v1/admin/stock/adjustments", body, adminToken).getStatusCode().value());
    }

    private ResponseEntity<String> createOrder(String adminToken, String orderNumber, List<Map<String, Object>> lines)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("orderNumber", orderNumber, "lines", lines));
        ResponseEntity<String> response = postJson("/api/v1/admin/orders", body, adminToken);
        assertEquals(201, response.getStatusCode().value());
        return response;
    }

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

    /** Minimal JDK-only stub WCS telegram receiver; no new test dependency. */
    private static final class StubWcsServer {
        private final HttpServer server;
        private final Deque<Integer> queuedStatuses = new ArrayDeque<>();
        private final List<String> receivedBodies = new java.util.concurrent.CopyOnWriteArrayList<>();

        private StubWcsServer(HttpServer server) {
            this.server = server;
        }

        static StubWcsServer startOrThrow() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                StubWcsServer stub = new StubWcsServer(server);
                server.createContext("/missions", stub::handle);
                server.start();
                return stub;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to start stub WCS server", ex);
            }
        }

        private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            receivedBodies.add(new String(requestBytes, StandardCharsets.UTF_8));
            Integer status;
            synchronized (queuedStatuses) {
                status = queuedStatuses.poll();
            }
            int responseCode = status != null ? status : 200;
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
        }

        int port() {
            return server.getAddress().getPort();
        }

        void stop() {
            server.stop(0);
        }

        void enqueueStatus(int status) {
            synchronized (queuedStatuses) {
                queuedStatuses.add(status);
            }
        }

        List<String> receivedBodies() {
            return receivedBodies.stream().collect(Collectors.toList());
        }
    }
}
