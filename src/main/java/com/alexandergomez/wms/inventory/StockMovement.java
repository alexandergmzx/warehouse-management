package com.alexandergomez.wms.inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only movement ledger row. Every stock quantity change appends exactly
 * one row; rows can never be updated or deleted (enforced by database trigger).
 * Foreign keys are held as scalar identifiers.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "resulting_quantity", nullable = false)
    private Integer resultingQuantity;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_line_id")
    private Long orderLineId;

    @Column(name = "picking_task_id")
    private Long pickingTaskId;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected StockMovement() {
    }

    public Long getId() {
        return id;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public Integer getResultingQuantity() {
        return resultingQuantity;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getOrderLineId() {
        return orderLineId;
    }

    public Long getPickingTaskId() {
        return pickingTaskId;
    }

    public Long getPerformedByUserId() {
        return performedByUserId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getReason() {
        return reason;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
