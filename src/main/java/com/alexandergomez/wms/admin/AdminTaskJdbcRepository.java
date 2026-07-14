package com.alexandergomez.wms.admin;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Dynamic, filtered task-list read for the administration dashboard surface
 * (API.md). Joins across every module that contributes a display field, so it
 * intentionally lives in {@code admin} rather than inside {@code picking}'s
 * focused claim/scan/confirm scope.
 */
@Repository
public class AdminTaskJdbcRepository {

    private static final Set<String> ACTIVE_STATES = Set.of("ASSIGNED", "LOCATION_CONFIRMED", "ARTICLE_CONFIRMED");
    private static final int MAX_ROWS = 500;

    private static final String BASE_SQL = """
            SELECT t.id, t.task_number, t.status, o.order_number, ol.line_number, l.code AS location_code,
                   a.sku AS article_sku, t.requested_quantity, u.username AS assigned_username,
                   d.device_code, t.last_transition_at
            FROM picking_task t
            JOIN order_line ol ON ol.id = t.order_line_id
            JOIN customer_order o ON o.id = ol.order_id
            JOIN article a ON a.id = t.article_id
            JOIN location l ON l.id = t.source_location_id
            LEFT JOIN app_user u ON u.id = t.assigned_user_id
            LEFT JOIN device d ON d.id = t.assigned_device_id
            WHERE 1 = 1
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminTaskJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminTaskSummary> listTasks(List<String> states, String orderNumber, String assignedUsername,
            boolean stuckOnly, Duration stuckThreshold) {
        StringBuilder sql = new StringBuilder(BASE_SQL);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (states != null && !states.isEmpty()) {
            sql.append(" AND t.status IN (:states)");
            params.addValue("states", states);
        }
        if (orderNumber != null && !orderNumber.isBlank()) {
            sql.append(" AND o.order_number = :orderNumber");
            params.addValue("orderNumber", orderNumber);
        }
        if (assignedUsername != null && !assignedUsername.isBlank()) {
            sql.append(" AND u.username = :assignedUsername");
            params.addValue("assignedUsername", assignedUsername);
        }
        OffsetDateTime stuckCutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(stuckThreshold);
        if (stuckOnly) {
            sql.append(" AND t.status IN ('ASSIGNED', 'LOCATION_CONFIRMED', 'ARTICLE_CONFIRMED')");
            sql.append(" AND t.last_transition_at < :stuckCutoff");
            params.addValue("stuckCutoff", stuckCutoff);
        }
        sql.append(" ORDER BY t.last_transition_at DESC LIMIT ").append(MAX_ROWS);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            String status = rs.getString("status");
            OffsetDateTime lastTransitionAt = rs.getObject("last_transition_at", OffsetDateTime.class);
            boolean stuck = ACTIVE_STATES.contains(status) && lastTransitionAt.isBefore(stuckCutoff);
            return new AdminTaskSummary(
                    rs.getLong("id"), rs.getString("task_number"), status, rs.getString("order_number"),
                    rs.getInt("line_number"), rs.getString("location_code"), rs.getString("article_sku"),
                    rs.getInt("requested_quantity"), rs.getString("assigned_username"), rs.getString("device_code"),
                    lastTransitionAt.toInstant(), stuck);
        });
    }
}
