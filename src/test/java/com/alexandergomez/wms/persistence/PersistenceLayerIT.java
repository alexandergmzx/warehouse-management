package com.alexandergomez.wms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.identity.AppUser;
import com.alexandergomez.wms.identity.AppUserRepository;
import com.alexandergomez.wms.identity.DeviceRepository;
import com.alexandergomez.wms.identity.UserRole;
import com.alexandergomez.wms.inventory.MovementType;
import com.alexandergomez.wms.inventory.StockJdbcRepository;
import com.alexandergomez.wms.inventory.StockLedgerDiscrepancy;
import com.alexandergomez.wms.inventory.StockMovement;
import com.alexandergomez.wms.inventory.StockMovementRepository;
import com.alexandergomez.wms.inventory.StockRepository;
import com.alexandergomez.wms.orders.CustomerOrder;
import com.alexandergomez.wms.orders.CustomerOrderRepository;
import com.alexandergomez.wms.orders.OrderLine;
import com.alexandergomez.wms.orders.OrderLineRepository;
import com.alexandergomez.wms.orders.OrderStatus;
import com.alexandergomez.wms.picking.PickingTask;
import com.alexandergomez.wms.picking.PickingTaskRepository;
import com.alexandergomez.wms.picking.TaskStatus;
import com.alexandergomez.wms.picking.TaskTransition;
import com.alexandergomez.wms.picking.TaskTransitionRepository;

/**
 * Boots the persistence layer against the migrated, digest-pinned PostgreSQL
 * image. A successful context start proves Hibernate {@code validate} accepts
 * every entity mapping against the Flyway-owned schema (never create/update).
 * The read assertions prove the Spring Data repositories and the JDBC
 * scaffolding return the deterministic development fixtures.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PersistenceLayerIT {

    /** Immutable image reference required by ADR 0002; keep in sync with compose.yaml. */
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/devdata");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private AppUserRepository appUsers;
    @Autowired
    private DeviceRepository devices;
    @Autowired
    private ArticleRepository articles;
    @Autowired
    private LocationRepository locations;
    @Autowired
    private StockRepository stock;
    @Autowired
    private StockMovementRepository stockMovements;
    @Autowired
    private CustomerOrderRepository orders;
    @Autowired
    private OrderLineRepository orderLines;
    @Autowired
    private PickingTaskRepository pickingTasks;
    @Autowired
    private TaskTransitionRepository taskTransitions;
    @Autowired
    private StockJdbcRepository stockJdbc;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void repositoriesReadSeedMasterData() {
        AppUser admin = appUsers.findByUsername("admin").orElseThrow();
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.getPasswordHash().startsWith("$argon2id$"));
        assertEquals(UserRole.PICKER, appUsers.findByUsername("picker01").orElseThrow().getRole());

        assertTrue(devices.findByDeviceCode("HHT-PI-01").isPresent());

        assertEquals(4, articles.count());
        Article art001 = articles.findBySku("ART-001").orElseThrow();
        assertEquals("ART:ART-001", art001.getQrValue());
        assertTrue(articles.findByQrValue("ART:ART-002").isPresent());

        assertEquals(5, locations.count());
        assertTrue(locations.findByCode("A-01-01").isPresent());

        assertEquals(5, stock.count());
        assertEquals(2, stock.findByArticleIdOrderByLocationId(art001.getId()).size());
    }

    @Test
    void repositoriesReadSeedOrdersTasksAndLedger() {
        CustomerOrder demo1001 = orders.findByOrderNumber("DEMO-1001").orElseThrow();
        assertEquals(OrderStatus.OPEN, demo1001.getStatus());
        assertEquals(3, orders.count());

        List<OrderLine> lines = orderLines.findByOrderIdOrderByLineNumber(demo1001.getId());
        assertEquals(2, lines.size());
        assertEquals(25, lines.get(0).getRequestedQuantity());

        assertEquals(5, pickingTasks.count());
        assertEquals(3, pickingTasks.countByStatus(TaskStatus.AVAILABLE));
        assertEquals(1, pickingTasks.countByStatus(TaskStatus.LOCATION_CONFIRMED));
        assertEquals(1, pickingTasks.countByStatus(TaskStatus.COMPLETED));

        PickingTask first = pickingTasks.findByTaskNumber("DEMO-1001-001-01").orElseThrow();
        assertEquals(TaskStatus.AVAILABLE, first.getStatus());
        assertEquals(20, first.getRequestedQuantity());

        PickingTask completed = pickingTasks.findByTaskNumber("DEMO-1003-001-01").orElseThrow();
        assertNotNull(completed.getConfirmationId());

        List<TaskTransition> history =
                taskTransitions.findByPickingTaskIdOrderByOccurredAtAscIdAsc(completed.getId());
        assertEquals(5, history.size());
        assertEquals(TaskStatus.COMPLETED, history.get(history.size() - 1).getNewStatus());

        assertEquals(6, stockMovements.count());
        StockMovement pick = stockMovements.findByPickingTaskId(completed.getId()).orElseThrow();
        assertEquals(MovementType.PICK, pick.getMovementType());
        assertEquals(-2, pick.getQuantityDelta());
        assertEquals(1, stockMovements.findByOrderIdOrderByOccurredAtAscIdAsc(pick.getOrderId()).size());
    }

    @Test
    void stockAvailabilitySubtractsUnfinishedTaskQuantity() {
        long art001 = articles.findBySku("ART-001").orElseThrow().getId();
        long art002 = articles.findBySku("ART-002").orElseThrow().getId();
        long art004 = articles.findBySku("ART-004").orElseThrow().getId();
        long binA0101 = locationId("A-01-01");
        long binA0201 = locationId("A-02-01");
        long binB0102 = locationId("B-01-02");

        // 20 on hand, one AVAILABLE task reserves all 20.
        assertEquals(0, stockJdbc.availableQuantity(art001, binA0101));
        // 12 on hand, one LOCATION_CONFIRMED task reserves 3.
        assertEquals(9, stockJdbc.availableQuantity(art004, binB0102));
        // 13 on hand after the completed pick; the reserving task is COMPLETED, so it is released.
        assertEquals(13, stockJdbc.availableQuantity(art002, binA0201));
    }

    @Test
    void stockReconciliationIsCleanForSeedFixtures() {
        assertTrue(stockJdbc.reconcileStockAgainstMovements().isEmpty());
    }

    @Test
    void reconciliationDetectsAnInjectedDiscrepancy() {
        // Directly corrupt one stock row's quantity, bypassing the append-only
        // movement ledger entirely, simulating real-world drift the diagnostic
        // must catch (FT-13).
        long articleId = articles.findBySku("ART-003").orElseThrow().getId();
        long locationId = locationId("B-01-01");
        jdbc.update("UPDATE stock SET quantity = quantity + 3 WHERE article_id = ? AND location_id = ?",
                articleId, locationId);

        List<StockLedgerDiscrepancy> discrepancies = stockJdbc.reconcileStockAgainstMovements();
        assertEquals(1, discrepancies.size());
        StockLedgerDiscrepancy discrepancy = discrepancies.get(0);
        assertEquals(articleId, discrepancy.articleId());
        assertEquals(locationId, discrepancy.locationId());
        assertEquals(11, discrepancy.stockQuantity());
        assertEquals(8, discrepancy.ledgerQuantity());
    }

    private long locationId(String code) {
        Location location = locations.findByCode(code).orElseThrow();
        return location.getId();
    }
}
