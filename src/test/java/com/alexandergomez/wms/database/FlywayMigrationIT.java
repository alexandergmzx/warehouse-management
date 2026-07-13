package com.alexandergomez.wms.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayMigrationIT {

    /**
     * Immutable image reference required by ADR 0002; keep in sync with compose.yaml.
     * Digest validated against the local runtime on 2026-07-13.
     */
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:17.10-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("wms_test")
            .withUsername("wms_test")
            .withPassword("wms_test_password");

    @BeforeAll
    static void migrateDatabase() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration", "classpath:db/devdata")
                .load()
                .migrate();
    }

    @Test
    void appliesAllMigrationsAndSeedsDemoScenarios() throws SQLException {
        assertEquals(2, queryForLong("SELECT count(*) FROM flyway_schema_history WHERE success"));
        assertEquals(3, queryForLong("SELECT count(*) FROM customer_order"));
        assertEquals(5, queryForLong("SELECT count(*) FROM picking_task"));
        assertEquals(11, queryForLong("SELECT count(*) FROM task_transition"));
        assertEquals(1, queryForLong("SELECT count(*) FROM stock_movement WHERE movement_type = 'PICK'"));
        assertEquals(3, queryForLong("""
            SELECT count(*)
            FROM picking_task t
            JOIN order_line ol ON ol.id = t.order_line_id
            JOIN customer_order o ON o.id = ol.order_id
            WHERE o.order_number = 'DEMO-1001' AND t.status = 'AVAILABLE'
            """));
        assertEquals(1, queryForLong("""
            SELECT count(*)
            FROM picking_task t
            JOIN order_line ol ON ol.id = t.order_line_id
            JOIN customer_order o ON o.id = ol.order_id
            WHERE o.order_number = 'DEMO-1002'
              AND t.status = 'LOCATION_CONFIRMED'
              AND t.last_transition_at < CURRENT_TIMESTAMP - INTERVAL '30 minutes'
            """));
        assertEquals(1, queryForLong("""
            SELECT count(*)
            FROM stock_movement m
            JOIN customer_order o ON o.id = m.order_id
            JOIN picking_task t ON t.id = m.picking_task_id
            WHERE o.order_number = 'DEMO-1003'
              AND o.status = 'COMPLETED'
              AND t.status = 'COMPLETED'
              AND m.quantity_delta = -2
            """));
        assertEquals(2, queryForLong("""
                SELECT count(*)
                FROM app_user
                WHERE password_hash LIKE '$argon2id$%'
                """));
        assertEquals(1, queryForLong("""
                SELECT count(*)
                FROM task_transition transition
                JOIN picking_task task ON task.id = transition.picking_task_id
                WHERE task.task_number = 'DEMO-1003-001-01'
                  AND transition.previous_status = 'ARTICLE_CONFIRMED'
                  AND transition.new_status = 'COMPLETED'
                """));
            assertEquals(0, queryForLong("""
                SELECT count(*)
                FROM order_line ol
                JOIN (
                    SELECT order_line_id, sum(requested_quantity) AS task_quantity
                    FROM picking_task
                    GROUP BY order_line_id
                ) allocated ON allocated.order_line_id = ol.id
                WHERE allocated.task_quantity <> ol.requested_quantity
                """));
            assertEquals(1, queryForLong("""
                SELECT count(*)
                FROM picking_task first_task
                JOIN picking_task second_task
                  ON second_task.order_line_id = first_task.order_line_id
                 AND second_task.task_sequence = 2
                JOIN location first_location ON first_location.id = first_task.source_location_id
                JOIN location second_location ON second_location.id = second_task.source_location_id
                WHERE first_task.task_number = 'DEMO-1001-001-01'
                  AND first_location.code < second_location.code
                  AND first_task.requested_quantity = 20
                  AND second_task.requested_quantity = 5
                """));
    }

    @Test
    void seededStockMatchesTheAppendOnlyMovementLedger() throws SQLException {
        assertEquals(0, queryForLong("""
                SELECT count(*)
                FROM stock s
                LEFT JOIN (
                    SELECT article_id, location_id, sum(quantity_delta) AS ledger_quantity
                    FROM stock_movement
                    GROUP BY article_id, location_id
                ) ledger USING (article_id, location_id)
                WHERE s.quantity <> coalesce(ledger.ledger_quantity, 0)
                """));
    }

    @Test
    void movementLedgerRejectsUpdatesAndDeletes() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException updateError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE stock_movement SET reason = 'tampered' WHERE id = 1");
                }
            });
            assertEquals("55000", updateError.getSQLState());
        }

        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException deleteError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM stock_movement WHERE id = 1");
                }
            });
            assertEquals("55000", deleteError.getSQLState());
        }
    }

    @Test
    void taskTransitionLedgerRejectsUpdatesAndDeletes() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException updateError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE task_transition SET reason = 'tampered' WHERE id = 1");
                }
            });
            assertEquals("55000", updateError.getSQLState());
        }

        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException deleteError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM task_transition WHERE id = 1");
                }
            });
            assertEquals("55000", deleteError.getSQLState());
        }
    }

    @Test
    void relationalConstraintsRejectInconsistentTasksAndMovements() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException taskError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO picking_task (
                                task_number, order_line_id, task_sequence, article_id,
                                source_location_id, requested_quantity
                            )
                            SELECT
                                'INVALID-ARTICLE', ol.id, 99, wrong_article.id,
                                l.id, 1
                            FROM order_line ol
                            JOIN customer_order o ON o.id = ol.order_id
                            JOIN article wrong_article ON wrong_article.sku = 'ART-002'
                            JOIN location l ON l.code = 'A-02-01'
                            WHERE o.order_number = 'DEMO-1001' AND ol.line_number = 1
                            """);
                }
            });
            assertEquals("23503", taskError.getSQLState());
        }

        try (Connection connection = POSTGRES.createConnection("")) {
            SQLException movementError = assertThrows(SQLException.class, () -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO stock_movement (
                                movement_type, article_id, location_id, quantity_delta,
                                resulting_quantity, performed_by_user_id, reason
                            )
                            SELECT 'ADJUSTMENT', a.id, l.id, 1, 999, u.id, 'Invalid test movement'
                            FROM article a
                            JOIN location l ON l.code = 'A-01-01'
                            JOIN app_user u ON u.username = 'admin'
                            WHERE a.sku = 'ART-001'
                            """);
                }
            });
            assertEquals("23514", movementError.getSQLState());
        }
    }

    private static long queryForLong(String sql) throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
