package com.alexandergomez.wms.admin;

import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        String orderNumber,
        String state,
        Instant createdAt,
        Instant completedAt,
        List<LineDetail> lines) {

    public OrderDetailResponse {
        lines = List.copyOf(lines);
    }

    public record LineDetail(
            Integer lineNumber,
            String articleSku,
            Integer requestedQuantity,
            Integer pickedQuantity,
            String state,
            List<TaskDetail> tasks) {

        public LineDetail {
            tasks = List.copyOf(tasks);
        }
    }

    public record TaskDetail(Long id, String locationCode, Integer quantity, String state) {
    }
}
