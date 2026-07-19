package com.alexandergomez.wms.mfc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Named-parameter JDBC scaffolding for claiming the next dispatchable mission
 * (ADR 0011), mirroring {@code picking.PickingJdbcRepository}'s FIFO claim:
 * lock only the claimed row and skip rows a concurrent dispatcher run already
 * holds.
 */
@Repository
public class MfcMissionJdbcRepository {

    private static final String CLAIM_NEXT_DISPATCHABLE_SQL = """
            SELECT id
            FROM mfc_mission
            WHERE state = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP)
            ORDER BY created_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public MfcMissionJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> claimNextDispatchableMissionId() {
        List<Long> ids = jdbc.query(CLAIM_NEXT_DISPATCHABLE_SQL, Map.of(), (rs, rowNum) -> rs.getLong(1));
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
