package com.alexandergomez.wms.orders;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Order line. {@code picked_quantity} never exceeds {@code requested_quantity}
 * (database check constraint); {@code updated_at} is trigger-maintained.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private Integer pickedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderLineStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected OrderLine() {
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getPickedQuantity() {
        return pickedQuantity;
    }

    public OrderLineStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markInProgress() {
        if (this.status == OrderLineStatus.OPEN) {
            this.status = OrderLineStatus.IN_PROGRESS;
        }
    }

    public void addPickedQuantity(int amount) {
        this.pickedQuantity += amount;
    }

    public void markCompleted() {
        this.status = OrderLineStatus.COMPLETED;
    }
}
