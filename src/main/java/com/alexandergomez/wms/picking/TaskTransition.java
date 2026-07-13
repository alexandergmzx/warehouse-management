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
 * Append-only audit record of a picking-task state change (claim, scans, block,
 * resume, completion). Rows can never be updated or deleted (database trigger).
 */
@Entity
@Table(name = "task_transition")
public class TaskTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "picking_task_id", nullable = false)
    private Long pickingTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private TaskStatus newStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "confirmation_id")
    private UUID confirmationId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected TaskTransition() {
    }

    public Long getId() {
        return id;
    }

    public Long getPickingTaskId() {
        return pickingTaskId;
    }

    public TaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public Long getActorUserId() {
        return actorUserId;
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

    public UUID getConfirmationId() {
        return confirmationId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
