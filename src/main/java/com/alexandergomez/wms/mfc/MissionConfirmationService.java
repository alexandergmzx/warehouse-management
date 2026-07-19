package com.alexandergomez.wms.mfc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;

/**
 * WCS-facing mission confirmation flow (TELEGRAMS.md, ADR 0011): validates
 * the requested transition against {@code mfc_mission}'s current state,
 * treats a confirmation naming the mission's current state as an idempotent
 * replay (no new transition row), and rejects anything else with {@code 409
 * INVALID_MISSION_STATE}. {@code SORT} missions always return {@code 501
 * SORT_NOT_IMPLEMENTED} — visible, not silent.
 */
@Service
public class MissionConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(MissionConfirmationService.class);

    private static final Map<MissionState, Set<MissionState>> LEGAL_TRANSITIONS = Map.of(
            MissionState.PENDING, Set.of(MissionState.DISPATCHED, MissionState.FAILED),
            MissionState.DISPATCHED, Set.of(MissionState.ACCEPTED, MissionState.FAILED),
            MissionState.ACCEPTED, Set.of(MissionState.COMPLETED, MissionState.FAILED));

    private static final Set<MissionState> CONFIRMABLE_STATES =
            Set.of(MissionState.ACCEPTED, MissionState.COMPLETED, MissionState.FAILED);

    private final MfcMissionRepository missions;
    private final MfcMissionTransitionRepository transitions;

    public MissionConfirmationService(MfcMissionRepository missions, MfcMissionTransitionRepository transitions) {
        this.missions = missions;
        this.transitions = transitions;
    }

    @Transactional
    public MissionConfirmationResponse confirm(Long missionId, MissionConfirmationRequest request) {
        MfcMission mission = missions.findByIdForUpdate(missionId)
                .orElseThrow(() -> new ProblemException(ProblemCode.MISSION_NOT_FOUND, "Mission not found."));

        if (mission.getMissionType() == MissionType.SORT) {
            throw new ProblemException(ProblemCode.SORT_NOT_IMPLEMENTED,
                    "SORT mission confirmations are not implemented in this version; see TELEGRAMS.md.");
        }

        MissionState requestedState = parseState(request.state());
        if (!CONFIRMABLE_STATES.contains(requestedState)) {
            throw new ProblemException(ProblemCode.INVALID_MISSION_STATE,
                    "A confirmation may only report ACCEPTED, COMPLETED, or FAILED.");
        }
        if (requestedState == MissionState.FAILED && isBlank(request.reason())) {
            throw new ProblemException(ProblemCode.VALIDATION_FAILED, "reason is required when state is FAILED.");
        }

        if (mission.getState() == requestedState) {
            return new MissionConfirmationResponse(mission.getId(), mission.getState().name(), true);
        }

        Set<MissionState> legalNext = LEGAL_TRANSITIONS.getOrDefault(mission.getState(), Set.of());
        if (!legalNext.contains(requestedState)) {
            throw new ProblemException(ProblemCode.INVALID_MISSION_STATE,
                    "Cannot transition mission from " + mission.getState() + " to " + requestedState + ".");
        }

        MissionState previous = mission.getState();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        mission.applyConfirmedState(requestedState, request.reason());
        missions.save(mission);
        transitions.save(MfcMissionTransition.record(
                mission.getId(), previous, requestedState, request.reason(), currentCorrelationUuid(), now));

        log.atInfo()
                .addKeyValue("missionId", mission.getId())
                .addKeyValue("previousState", previous)
                .addKeyValue("newState", requestedState)
                .addKeyValue("wcsOccurredAt", request.occurredAt())
                .log("MFC mission confirmed");

        return new MissionConfirmationResponse(mission.getId(), mission.getState().name(), false);
    }

    private static MissionState parseState(String raw) {
        try {
            return MissionState.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new ProblemException(ProblemCode.VALIDATION_FAILED, "Unknown mission state: " + raw);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static UUID currentCorrelationUuid() {
        try {
            return UUID.fromString(CorrelationIdFilter.currentCorrelationId());
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }
}
