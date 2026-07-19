package com.alexandergomez.wms.mfc;

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
 * Transactional outbox row for one MFC mission telegram (ADR 0011,
 * TELEGRAMS.md). A {@code PENDING} row is written in the same transaction
 * that completes the order ({@code TelegramOrderCompletionPublisher}); the
 * {@link MissionDispatcher} and {@code MissionConfirmationService} own every
 * later state change. {@code updated_at} is database-trigger maintained.
 */
@Entity
@Table(name = "mfc_mission")
public class MfcMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 20)
    private MissionType missionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private MissionState state;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "source_location_id", nullable = false)
    private Long sourceLocationId;

    @Column(name = "destination_location_id", nullable = false)
    private Long destinationLocationId;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MfcMission() {
    }

    public static MfcMission createTransport(UUID eventId, Long orderId, String orderNumber,
            Long sourceLocationId, Long destinationLocationId, OffsetDateTime now) {
        MfcMission mission = new MfcMission();
        mission.eventId = eventId;
        mission.missionType = MissionType.TRANSPORT;
        mission.state = MissionState.PENDING;
        mission.orderId = orderId;
        mission.orderNumber = orderNumber;
        mission.sourceLocationId = sourceLocationId;
        mission.destinationLocationId = destinationLocationId;
        mission.attempts = 0;
        mission.createdAt = now;
        mission.updatedAt = now;
        return mission;
    }

    public void recordDispatchAttempt() {
        this.attempts = this.attempts + 1;
    }

    public void markDispatched() {
        this.state = MissionState.DISPATCHED;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void scheduleRetry(String error, OffsetDateTime nextAttemptAt) {
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markDispatchExhausted(String error) {
        this.state = MissionState.FAILED;
        this.lastError = error;
        this.nextAttemptAt = null;
    }

    public void applyConfirmedState(MissionState newState, String reason) {
        this.state = newState;
        if (newState == MissionState.FAILED) {
            this.lastError = reason;
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public MissionState getState() {
        return state;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
