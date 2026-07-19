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
 * Append-only audit record of an {@link MfcMission} state change. Rows can
 * never be updated or deleted (database trigger), mirroring {@code
 * task_transition}.
 */
@Entity
@Table(name = "mfc_mission_transition")
public class MfcMissionTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mfc_mission_id", nullable = false)
    private Long mfcMissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", length = 20)
    private MissionState previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false, length = 20)
    private MissionState newState;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected MfcMissionTransition() {
    }

    public static MfcMissionTransition record(Long mfcMissionId, MissionState previousState,
            MissionState newState, String reason, UUID correlationId, OffsetDateTime occurredAt) {
        MfcMissionTransition transition = new MfcMissionTransition();
        transition.mfcMissionId = mfcMissionId;
        transition.previousState = previousState;
        transition.newState = newState;
        transition.reason = reason;
        transition.correlationId = correlationId;
        transition.occurredAt = occurredAt;
        return transition;
    }

    public Long getId() {
        return id;
    }

    public Long getMfcMissionId() {
        return mfcMissionId;
    }

    public MissionState getPreviousState() {
        return previousState;
    }

    public MissionState getNewState() {
        return newState;
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
