package com.alexandergomez.wms.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

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

import com.alexandergomez.wms.api.CorrelationIdFilter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end authentication tests over the running HTTP server and the migrated,
 * digest-pinned PostgreSQL container. Covers FT-01 (login + idempotent logout),
 * FT-03 (malformed request, missing token, insufficient role, validation), and
 * FT-14 (expired, revoked, inactive-user, and inactive-device access fail
 * closed with stable problem codes).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthApiIT {

    /** Immutable image reference required by ADR 0002; keep in sync with compose.yaml. */
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    private static final String NEXT_TASK_PATH = "/api/v1/hht/tasks/next";

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    // JDK HttpClient factory: unlike the default HttpURLConnection, it exposes the
    // response body for a POST that returns 401, which the login tests assert on.
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
    void ft01_loginPickerSucceedsAndLogoutIsIdempotent() throws Exception {
        ResponseEntity<String> login = login("picker01", "picker123", "HHT-PI-01");
        assertEquals(200, login.getStatusCode().value());

        JsonNode body = json(login);
        assertTrue(body.get("token").asString().startsWith("wms_"));
        assertEquals("Bearer", body.get("tokenType").asString());
        assertFalse(body.get("expiresAt").asString().isBlank());
        assertEquals("PICKER", body.get("user").get("role").asString());
        assertEquals("HHT-PI-01", body.get("device").get("code").asString());

        String token = body.get("token").asString();
        assertEquals(204, logout(token).getStatusCode().value());
        assertEquals(204, logout(token).getStatusCode().value());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        assertProblem(login("picker01", "wrong-password", "HHT-PI-01"), 401, "INVALID_CREDENTIALS");
    }

    @Test
    void loginConflictsWhenDeviceHoldsAnotherUsersActiveTask() throws Exception {
        // Seed: picker01 holds a LOCATION_CONFIRMED task on HHT-PI-01, so admin cannot claim it.
        assertProblem(login("admin", "admin123", "HHT-PI-01"), 409, "DEVICE_ASSIGNMENT_CONFLICT");
    }

    @Test
    void ft03_malformedJsonReturnsProblemAndEchoesCorrelationId() throws Exception {
        String correlationId = "11111111-1111-1111-1111-111111111111";
        HttpHeaders headers = jsonHeaders();
        headers.set(CorrelationIdFilter.HEADER, correlationId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"), new HttpEntity<>("{ not valid json ", headers), String.class);

        assertProblem(response, 400, "MALFORMED_REQUEST");
        assertEquals(correlationId, json(response).get("correlationId").asString());
        assertEquals(correlationId, response.getHeaders().getFirst(CorrelationIdFilter.HEADER));
    }

    @Test
    void ft03_missingTokenIsUnauthorized() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(url(NEXT_TASK_PATH), String.class);
        assertProblem(response, 401, "INVALID_TOKEN");
    }

    @Test
    void ft03_insufficientRoleIsForbidden() throws Exception {
        String token = loginToken("picker01", "picker123", "HHT-PI-01");
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/admin/orders/DEMO-1001"), HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertProblem(response, 403, "FORBIDDEN");
    }

    @Test
    void ft03_validationFailureListsInvalidFields() throws Exception {
        ResponseEntity<String> response = postJson("/api/v1/auth/login",
                "{\"username\":\"\",\"password\":\"picker123\",\"deviceCode\":\"HHT-PI-01\"}");
        assertProblem(response, 422, "VALIDATION_FAILED");
        assertTrue(json(response).get("fields").has("username"));
    }

    @Test
    void ft14_expiredTokenIsRejected() throws Exception {
        String token = "wms_expired-" + UUID.randomUUID();
        Long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'picker01'", Long.class);
        Long deviceId = jdbc.queryForObject("SELECT id FROM device WHERE device_code = 'HHT-PI-01'", Long.class);
        jdbc.update("""
                INSERT INTO auth_token (token_hash, user_id, device_id, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                sha256Hex(token), userId, deviceId,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

        assertProblem(getWithToken(NEXT_TASK_PATH, token), 401, "TOKEN_EXPIRED");
    }

    @Test
    void ft14_revokedTokenIsRejected() throws Exception {
        String token = loginToken("picker01", "picker123", "HHT-PI-01");
        assertEquals(204, logout(token).getStatusCode().value());
        assertProblem(getWithToken(NEXT_TASK_PATH, token), 401, "TOKEN_REVOKED");
    }

    @Test
    void ft14_inactiveUserFailsClosed() throws Exception {
        String token = loginToken("picker01", "picker123", "HHT-PI-01");
        jdbc.update("UPDATE app_user SET active = false WHERE username = 'picker01'");
        try {
            assertProblem(getWithToken(NEXT_TASK_PATH, token), 401, "INVALID_TOKEN");
        } finally {
            jdbc.update("UPDATE app_user SET active = true WHERE username = 'picker01'");
        }
    }

    @Test
    void ft14_inactiveDeviceFailsClosed() throws Exception {
        String token = loginToken("picker01", "picker123", "HHT-PI-01");
        jdbc.update("UPDATE device SET active = false WHERE device_code = 'HHT-PI-01'");
        try {
            assertProblem(getWithToken(NEXT_TASK_PATH, token), 401, "INVALID_TOKEN");
        } finally {
            jdbc.update("UPDATE device SET active = true WHERE device_code = 'HHT-PI-01'");
        }
    }

    private ResponseEntity<String> login(String username, String password, String deviceCode) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password, "deviceCode", deviceCode));
        return postJson("/api/v1/auth/login", body);
    }

    private String loginToken(String username, String password, String deviceCode) throws Exception {
        ResponseEntity<String> response = login(username, password, deviceCode);
        assertEquals(200, response.getStatusCode().value());
        return json(response).get("token").asString();
    }

    private ResponseEntity<String> logout(String token) {
        return restTemplate.exchange(url("/api/v1/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<String> postJson(String path, String body) {
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, jsonHeaders()), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) throws Exception {
        assertEquals(status, response.getStatusCode().value());
        MediaType contentType = response.getHeaders().getContentType();
        assertTrue(contentType != null && contentType.toString().contains("application/problem+json"),
                "expected problem+json, was " + contentType);
        JsonNode body = json(response);
        assertEquals(code, body.get("code").asString());
        assertFalse(body.get("correlationId").asString().isBlank());
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

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
