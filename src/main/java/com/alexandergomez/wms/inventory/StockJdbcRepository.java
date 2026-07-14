package com.alexandergomez.wms.inventory;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Named-parameter JDBC scaffolding for the PostgreSQL-specific read paths that
 * back allocation and diagnostics (ADR 0003). These are pure reads and make no
 * state change.
 *
 * <p>The mutating locking paths — the FIFO next-task claim
 * ({@code FOR UPDATE OF task SKIP LOCKED}) and the ascending
 * {@code (article_id, location_id)} allocation lock — are delivered together
 * with their transactional services in the picking happy-path (plan Step 3) and
 * admin-order (plan Step 5) slices, so no row-locking SQL is executed here yet.
 */
@Repository
public class StockJdbcRepository {

    /**
     * Available-to-allocate quantity for one bin: on-hand minus the requested
     * quantity of every unfinished task already drawing from it (ADR 0003).
     * Released tasks are durable reservations, so any non-{@code COMPLETED}
     * task counts against availability.
     */
    private static final String AVAILABILITY_SQL = """
            SELECT s.quantity - COALESCE((
                       SELECT sum(t.requested_quantity)
                       FROM picking_task t
                       WHERE t.article_id = s.article_id
                         AND t.source_location_id = s.location_id
                         AND t.status <> 'COMPLETED'
                   ), 0) AS available
            FROM stock s
            WHERE s.article_id = :articleId
              AND s.location_id = :locationId
            """;

    /**
     * Every bin that currently carries a given article, ordered by ascending
     * location code — the order in which order creation draws from bins to
     * satisfy a multi-bin line (confirmed workflow baseline).
     */
    private static final String CANDIDATE_BINS_SQL = """
            SELECT s.article_id, s.location_id, l.code AS location_code
            FROM stock s
            JOIN location l ON l.id = s.location_id
            WHERE s.article_id = :articleId
            ORDER BY l.code
            """;

    /**
     * Sum of requested quantity across every unfinished (non-{@code COMPLETED})
     * task already drawing from one bin — the same reservation figure embedded
     * in {@link #availableQuantity}, exposed standalone so order creation can
     * combine it with a stock quantity already read under its own lock rather
     * than re-reading {@code stock} here.
     */
    private static final String UNFINISHED_RESERVATION_SQL = """
            SELECT COALESCE(sum(t.requested_quantity), 0)
            FROM picking_task t
            WHERE t.article_id = :articleId
              AND t.source_location_id = :locationId
              AND t.status <> 'COMPLETED'
            """;

    /**
     * Stock-versus-ledger reconciliation: bins whose on-hand quantity differs
     * from the sum of their append-only movement deltas (FT-13). A clean
     * fixture returns no rows.
     */
    private static final String RECONCILE_SQL = """
            SELECT s.article_id, s.location_id, s.quantity AS stock_quantity,
                   COALESCE(ledger.ledger_quantity, 0) AS ledger_quantity
            FROM stock s
            LEFT JOIN (
                SELECT article_id, location_id, sum(quantity_delta) AS ledger_quantity
                FROM stock_movement
                GROUP BY article_id, location_id
            ) ledger USING (article_id, location_id)
            WHERE s.quantity <> COALESCE(ledger.ledger_quantity, 0)
            ORDER BY s.article_id, s.location_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public StockJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int availableQuantity(long articleId, long locationId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("articleId", articleId)
                .addValue("locationId", locationId);
        Integer available = jdbc.queryForObject(AVAILABILITY_SQL, params, Integer.class);
        return available == null ? 0 : available;
    }

    public List<CandidateBin> candidateBinsForArticle(long articleId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("articleId", articleId);
        return jdbc.query(CANDIDATE_BINS_SQL, params, (rs, rowNum) -> new CandidateBin(
                rs.getLong("article_id"), rs.getLong("location_id"), rs.getString("location_code")));
    }

    public int unfinishedTaskReservation(long articleId, long locationId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("articleId", articleId)
                .addValue("locationId", locationId);
        Integer reserved = jdbc.queryForObject(UNFINISHED_RESERVATION_SQL, params, Integer.class);
        return reserved == null ? 0 : reserved;
    }

    public List<StockLedgerDiscrepancy> reconcileStockAgainstMovements() {
        return jdbc.query(RECONCILE_SQL, (rs, rowNum) -> new StockLedgerDiscrepancy(
                rs.getLong("article_id"),
                rs.getLong("location_id"),
                rs.getInt("stock_quantity"),
                rs.getLong("ledger_quantity")));
    }
}
