package com.alexandergomez.wms.label;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * FT-17: generating the same location/article labels twice yields exact,
 * deterministic PNG/PDF bytes, and the encoded QR payload decodes to the exact
 * value the HHT scan endpoints accept.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LabelApiIT {

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
    void ft17_labelsAreDeterministicAndScanToValuesTheHhtApiAccepts() throws Exception {
        String adminToken = loginAsAdmin();

        String sku = "TST-LABEL-" + SEQUENCE.incrementAndGet();
        String locationCode = "ZLBL-01-01";
        createArticle(adminToken, sku, "FT-17 label test article");
        createLocation(adminToken, locationCode, 90201 + SEQUENCE.incrementAndGet());
        adjustStock(adminToken, sku, locationCode, 5, "seed for FT-17");

        String orderNumber = "TST-LABEL-ORD-" + SEQUENCE.incrementAndGet();
        ResponseEntity<String> createResponse = createOrder(adminToken, orderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5)));
        assertEquals(201, createResponse.getStatusCode().value());
        backdateOrder(orderNumber);

        // Determinism: repeated generation is byte-identical for both PNG and PDF, location and article.
        byte[] locationPngFirst = getBytes("/api/v1/admin/labels/locations/" + locationCode + "/png", adminToken);
        byte[] locationPngSecond = getBytes("/api/v1/admin/labels/locations/" + locationCode + "/png", adminToken);
        assertArrayEquals(locationPngFirst, locationPngSecond, "location PNG must be byte-identical across generations");

        byte[] locationPdfFirst = getBytes("/api/v1/admin/labels/locations/" + locationCode + "/pdf", adminToken);
        byte[] locationPdfSecond = getBytes("/api/v1/admin/labels/locations/" + locationCode + "/pdf", adminToken);
        assertArrayEquals(locationPdfFirst, locationPdfSecond, "location PDF must be byte-identical across generations");

        byte[] articlePngFirst = getBytes("/api/v1/admin/labels/articles/" + sku + "/png", adminToken);
        byte[] articlePngSecond = getBytes("/api/v1/admin/labels/articles/" + sku + "/png", adminToken);
        assertArrayEquals(articlePngFirst, articlePngSecond, "article PNG must be byte-identical across generations");

        byte[] articlePdfFirst = getBytes("/api/v1/admin/labels/articles/" + sku + "/pdf", adminToken);
        byte[] articlePdfSecond = getBytes("/api/v1/admin/labels/articles/" + sku + "/pdf", adminToken);
        assertArrayEquals(articlePdfFirst, articlePdfSecond, "article PDF must be byte-identical across generations");

        // Exact payload: decode the PNG and compare against the exact ADR 0007 payload format.
        String decodedLocation = decodeQr(locationPngFirst);
        assertEquals("LOC:" + locationCode, decodedLocation);
        String decodedArticle = decodeQr(articlePngFirst);
        assertEquals("ART:" + sku, decodedArticle);

        // Round-trip: the decoded payload is accepted by the real HHT scan endpoints.
        String pickerToken = loginAsFreshOperator("ft17picker");
        JsonNode claimed = json(getWithToken("/api/v1/hht/tasks/next", pickerToken));
        assertEquals(orderNumber, claimed.get("orderNumber").asString());
        long taskId = claimed.get("id").asLong();

        ResponseEntity<String> scanLocation = postJson("/api/v1/hht/tasks/" + taskId + "/scan-location",
                objectMapper.writeValueAsString(Map.of("qrValue", decodedLocation)), pickerToken);
        assertEquals(200, scanLocation.getStatusCode().value());
        assertEquals("LOCATION_CONFIRMED", json(scanLocation).get("state").asString());

        ResponseEntity<String> scanArticle = postJson("/api/v1/hht/tasks/" + taskId + "/scan-article",
                objectMapper.writeValueAsString(Map.of("qrValue", decodedArticle)), pickerToken);
        assertEquals(200, scanArticle.getStatusCode().value());
        assertEquals("ARTICLE_CONFIRMED", json(scanArticle).get("state").asString());

        // Admin-only surface: a PICKER cannot generate labels.
        assertEquals(403, getResponse("/api/v1/admin/labels/locations/" + locationCode + "/png", pickerToken)
                .getStatusCode().value());

        // Unknown code/sku produce the existing not-found problems, not a raw error.
        assertProblem(getResponse("/api/v1/admin/labels/locations/DOES-NOT-EXIST/png", adminToken),
                404, "LOCATION_NOT_FOUND");
        assertProblem(getResponse("/api/v1/admin/labels/articles/DOES-NOT-EXIST/png", adminToken),
                404, "ARTICLE_NOT_FOUND");
    }

    private static String decodeQr(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private String loginAsAdmin() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-LABEL-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin label test device");
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
     * fixture's own older-but-unavailable tasks (same technique as the Phase 7
     * admin recovery tests).
     */
    private void backdateOrder(String orderNumber) {
        jdbc.update("""
                UPDATE customer_order
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '100 days',
                    released_at = CURRENT_TIMESTAMP - INTERVAL '100 days'
                WHERE order_number = ?
                """, orderNumber);
    }

    private byte[] getBytes(String path, String token) {
        ResponseEntity<byte[]> response =
                restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), byte[].class);
        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }

    private ResponseEntity<String> getResponse(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
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
