package com.alexandergomez.wms.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * FT-18: the admin dashboard requires an authenticated {@code ADMIN} session
 * (unauthenticated and non-admin access fail), and its JSON polling endpoint
 * reflects live task state without requiring the {@code /dashboard} page
 * itself to reload.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DashboardApiIT {

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
    private static final Pattern CSRF_PATTERN = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    /** Redirects are inspected explicitly (login/authorization checks depend on the 3xx target). */
    private final RestTemplate restTemplate = new RestTemplate(
            new JdkClientHttpRequestFactory(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()));

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
    void ft18_dashboardIsAdminOnlyAndPollingReflectsLiveTaskState() throws Exception {
        // Unauthenticated access to the dashboard fails (redirected to the login page, not the content).
        ResponseEntity<String> anonymous = getRaw("/dashboard", null);
        assertEquals(302, anonymous.getStatusCode().value());
        assertTrue(anonymous.getHeaders().getLocation().toString().contains("/login"),
                "expected redirect to login, was " + anonymous.getHeaders().getLocation());

        // Seed one live task via the existing bearer-token admin/HHT API so the dashboard has state to reflect.
        String adminApiToken = loginAsAdminApi();
        String sku = "TST-DASH-" + SEQUENCE.incrementAndGet();
        String locationCode = "ZDSH-01-01";
        createArticle(adminApiToken, sku, "FT-18 dashboard test article");
        createLocation(adminApiToken, locationCode, 90301 + SEQUENCE.incrementAndGet());
        adjustStock(adminApiToken, sku, locationCode, 5, "seed for FT-18");
        String orderNumber = "TST-DASH-ORD-" + SEQUENCE.incrementAndGet();
        assertEquals(201, createOrder(adminApiToken, orderNumber,
                List.of(Map.of("lineNumber", 1, "articleSku", sku, "quantity", 5))).getStatusCode().value());
        backdateOrder(orderNumber);

        // Wrong credentials do not establish a session.
        ResponseEntity<String> loginPage = getRaw("/login", null);
        String csrfToken = extractCsrfToken(loginPage.getBody());
        String anonymousCookie = firstCookie(loginPage);
        ResponseEntity<String> badLogin =
                postLoginForm(anonymousCookie, "admin", "wrong-password-entirely", csrfToken);
        assertEquals(302, badLogin.getStatusCode().value());
        assertTrue(badLogin.getHeaders().getLocation().toString().contains("/login"),
                "wrong credentials must not reach the dashboard");

        // A valid ADMIN session can view the server-rendered page and poll live JSON state.
        String adminSessionCookie = loginToDashboard("admin", "admin123");

        ResponseEntity<String> dashboardPage = getRaw("/dashboard", adminSessionCookie);
        assertEquals(200, dashboardPage.getStatusCode().value());
        assertTrue(dashboardPage.getBody().contains(orderNumber),
                "server-rendered dashboard should list the seeded order");

        ResponseEntity<String> polled = getRaw("/dashboard/api/tasks", adminSessionCookie);
        assertEquals(200, polled.getStatusCode().value());
        JsonNode tasks = objectMapper.readTree(polled.getBody());
        assertTrue(tasks.isArray() && !tasks.isEmpty(), "polling endpoint should list current tasks");
        boolean containsSeededOrder = false;
        for (JsonNode task : tasks) {
            if (orderNumber.equals(task.get("orderNumber").asString())) {
                containsSeededOrder = true;
                break;
            }
        }
        assertTrue(containsSeededOrder, "polling response should reflect the same live state as the page");

        // An authenticated PICKER can log in but the admin-only dashboard refuses them.
        String pickerUsername = createPickerUser("ft18picker");
        String pickerSessionCookie = loginToDashboard(pickerUsername, "picker123");
        ResponseEntity<String> forbidden = getRaw("/dashboard", pickerSessionCookie);
        assertEquals(403, forbidden.getStatusCode().value());
        ResponseEntity<String> forbiddenPoll = getRaw("/dashboard/api/tasks", pickerSessionCookie);
        assertEquals(403, forbiddenPoll.getStatusCode().value());
    }

    private String loginToDashboard(String username, String password) throws Exception {
        ResponseEntity<String> loginPage = getRaw("/login", null);
        String csrfToken = extractCsrfToken(loginPage.getBody());
        String cookie = firstCookie(loginPage);

        ResponseEntity<String> loginResponse = postLoginForm(cookie, username, password, csrfToken);
        assertEquals(302, loginResponse.getStatusCode().value());
        assertTrue(loginResponse.getHeaders().getLocation().toString().endsWith("/dashboard"),
                "successful login must redirect to /dashboard, was " + loginResponse.getHeaders().getLocation());

        String authenticatedCookie = firstCookie(loginResponse);
        return authenticatedCookie != null ? authenticatedCookie : cookie;
    }

    private ResponseEntity<String> postLoginForm(String cookie, String username, String password, String csrfToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        String body = "username=" + encode(username)
                + "&password=" + encode(password)
                + "&_csrf=" + encode(csrfToken);
        return restTemplate.postForEntity(url("/login"), new HttpEntity<>(body, headers), String.class);
    }

    private String createPickerUser(String label) {
        int sequence = SEQUENCE.incrementAndGet();
        String username = "test-picker-" + label.toLowerCase(java.util.Locale.ROOT) + "-" + sequence;
        jdbc.update("""
                INSERT INTO app_user (username, password_hash, role)
                VALUES (?, (SELECT password_hash FROM app_user WHERE username = 'picker01'), 'PICKER')
                """, username);
        return username;
    }

    private String loginAsAdminApi() throws Exception {
        String deviceCode = "TEST-ADMIN-DEV-DASH-" + SEQUENCE.incrementAndGet();
        jdbc.update("INSERT INTO device (device_code, description) VALUES (?, ?)",
                deviceCode, "Admin dashboard test device");
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "password", "admin123", "deviceCode", deviceCode));
        ResponseEntity<String> login = postJson("/api/v1/auth/login", body);
        assertEquals(200, login.getStatusCode().value());
        return json(login).get("token").asString();
    }

    private void createArticle(String adminToken, String sku, String description) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("sku", sku, "description", description));
        assertEquals(201, postJsonBearer("/api/v1/admin/articles", body, adminToken).getStatusCode().value());
    }

    private void createLocation(String adminToken, String code, int pickSequence) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("code", code, "pickSequence", pickSequence));
        assertEquals(201, postJsonBearer("/api/v1/admin/locations", body, adminToken).getStatusCode().value());
    }

    private void adjustStock(String adminToken, String articleSku, String locationCode, int delta, String reason)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "articleSku", articleSku, "locationCode", locationCode, "quantityDelta", delta, "reason", reason));
        assertEquals(201, postJsonBearer("/api/v1/admin/stock/adjustments", body, adminToken).getStatusCode().value());
    }

    private ResponseEntity<String> createOrder(String adminToken, String orderNumber, List<Map<String, Object>> lines)
            throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("orderNumber", orderNumber, "lines", lines));
        return postJsonBearer("/api/v1/admin/orders", body, adminToken);
    }

    /** Same backdating technique used by the Phase 7 admin recovery tests, to isolate FIFO ordering. */
    private void backdateOrder(String orderNumber) {
        jdbc.update("""
                UPDATE customer_order
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '100 days',
                    released_at = CURRENT_TIMESTAMP - INTERVAL '100 days'
                WHERE order_number = ?
                """, orderNumber);
    }

    private ResponseEntity<String> getRaw(String path, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> postJson(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> postJsonBearer(String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        return objectMapper.readTree(response.getBody());
    }

    private static String extractCsrfToken(String html) {
        Matcher matcher = CSRF_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("CSRF token not found in login page HTML");
        }
        return matcher.group(1);
    }

    private static String firstCookie(ResponseEntity<String> response) {
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null || setCookies.isEmpty()) {
            return null;
        }
        return setCookies.get(0).split(";", 2)[0];
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
