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
 * Customer order header. FIFO offering is by {@code created_at} then {@code id}
 * (ADR 0003). {@code updated_at} is maintained by a database trigger.
 */
@Entity
@Table(name = "customer_order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected CustomerOrder() {
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markInProgress() {
        if (this.status == OrderStatus.OPEN) {
            this.status = OrderStatus.IN_PROGRESS;
        }
    }

    public void markCompleted(OffsetDateTime when) {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = when;
    }

    /**
     * Creates an order already released and claimable: order creation locks
     * candidate stock, allocates every line, and releases the order atomically
     * (architecture.md), so there is no separate unreleased state to model.
     */
    public static CustomerOrder open(String orderNumber, Long createdByUserId, OffsetDateTime releasedAt) {
        CustomerOrder order = new CustomerOrder();
        order.orderNumber = orderNumber;
        order.status = OrderStatus.OPEN;
        order.createdByUserId = createdByUserId;
        order.releasedAt = releasedAt;
        return order;
    }
}
