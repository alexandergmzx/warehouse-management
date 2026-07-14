package com.alexandergomez.wms.picking;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Named-parameter JDBC scaffolding for the FIFO task claim (ADR 0003). This is
 * the one row-locking read in the picking module that cannot be expressed as a
 * Spring Data derived query: it joins across three tables to establish the
 * global FIFO order (order creation time, order ID, line number, task
 * sequence, task ID) while locking only the {@code picking_task} row and
 * skipping rows a concurrent claim already holds.
 */
@Repository
public class PickingJdbcRepository {

    private static final String CLAIM_NEXT_AVAILABLE_SQL = """
            SELECT t.id
            FROM picking_task t
            JOIN order_line ol ON ol.id = t.order_line_id
            JOIN customer_order o ON o.id = ol.order_id
            WHERE t.status = 'AVAILABLE'
            ORDER BY o.created_at, o.id, ol.line_number, t.task_sequence, t.id
            LIMIT 1
            FOR UPDATE OF t SKIP LOCKED
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PickingJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> claimNextAvailableTaskId() {
        List<Long> ids = jdbc.query(CLAIM_NEXT_AVAILABLE_SQL, Map.of(), (rs, rowNum) -> rs.getLong(1));
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
