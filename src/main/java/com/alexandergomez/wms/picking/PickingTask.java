package com.alexandergomez.wms.picking;

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
 * Picking task: one bin-level slice of an order line. Overlapping composite
 * foreign keys (to {@code order_line}, {@code stock}) are held as scalar
 * identifiers; the claim and confirmation transactions lock the relevant rows
 * directly via JDBC (ADR 0003). {@code last_transition_at} and
 * {@code updated_at} are database-trigger maintained.
 */
@Entity
@Table(name = "picking_task")
public class PickingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_number", nullable = false, length = 80)
    private String taskNumber;

    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Column(name = "task_sequence", nullable = false)
    private Integer taskSequence;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "source_location_id", nullable = false)
    private Long sourceLocationId;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "confirmed_quantity", nullable = false)
    private Integer confirmedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "assigned_device_id")
    private Long assignedDeviceId;

    @Column(name = "confirmation_id")
    private UUID confirmationId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "location_confirmed_at")
    private OffsetDateTime locationConfirmedAt;

    @Column(name = "article_confirmed_at")
    private OffsetDateTime articleConfirmedAt;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "last_transition_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastTransitionAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected PickingTask() {
    }

    public Long getId() {
        return id;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public Long getOrderLineId() {
        return orderLineId;
    }

    public Integer getTaskSequence() {
        return taskSequence;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getConfirmedQuantity() {
        return confirmedQuantity;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public Long getAssignedDeviceId() {
        return assignedDeviceId;
    }

    public UUID getConfirmationId() {
        return confirmationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public OffsetDateTime getLocationConfirmedAt() {
        return locationConfirmedAt;
    }

    public OffsetDateTime getArticleConfirmedAt() {
        return articleConfirmedAt;
    }

    public OffsetDateTime getBlockedAt() {
        return blockedAt;
    }

    public OffsetDateTime getLastTransitionAt() {
        return lastTransitionAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
